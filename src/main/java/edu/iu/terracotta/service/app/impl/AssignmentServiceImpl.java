package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ags.LineItem;
import edu.iu.terracotta.model.ags.LineItems;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.oauth2.LTIToken;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.TreatmentService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.service.lti.AdvantageAGSService;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Exposure;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class AssignmentServiceImpl implements AssignmentService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    AdvantageAGSService advantageAGSService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    ExperimentService experimentService;

    @Autowired
    ParticipantService participantService;

    @Autowired
    TreatmentService treatmentService;

    @Autowired
    CanvasAPIClient canvasAPIClient;

    @Autowired
    GroupService groupService;

    @Autowired
    APIJWTService apijwtService;

    @Value("${application.url}")
    private String localUrl;




    static final Logger log = LoggerFactory.getLogger(AssignmentServiceImpl.class);

    @Override
    public List<Assignment> findAllByExposureId(long exposureId) {
        return allRepositories.assignmentRepository.findByExposure_ExposureId(exposureId);
    }

    @Override
    public AssignmentDto toDto(Assignment assignment) {

        AssignmentDto assignmentDto = new AssignmentDto();
        assignmentDto.setAssignmentId(assignment.getAssignmentId());
        assignmentDto.setLmsAssignmentId(assignment.getLmsAssignmentId());
        assignmentDto.setTitle(assignment.getTitle());
        assignmentDto.setAssignmentOrder(assignment.getAssignmentOrder());
        assignmentDto.setExposureId(assignment.getExposure().getExposureId());
        assignmentDto.setResourceLinkId(assignment.getResourceLinkId());
        assignmentDto.setSoftDeleted(assignment.getSoftDeleted());
        long submissions = allRepositories.submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(assignment.getAssignmentId());
        if(submissions > 0){
            assignmentDto.setStarted(true);
        }
        List<Treatment> treatments = allRepositories.treatmentRepository.findByAssignment_AssignmentId(assignment.getAssignmentId());
        List<TreatmentDto> treatmentDtos = new ArrayList<>();
        for (Treatment treatment:treatments){
            TreatmentDto treatmentDto = treatmentService.toDto(treatment);
            treatmentDtos.add(treatmentDto);
        }
        assignmentDto.setTreatments(treatmentDtos);
        return assignmentDto;
    }

    @Override
    public Assignment fromDto(AssignmentDto assignmentDto) throws DataServiceException {

        //Note: we don't want to allow the dto to change the LmsAssignmentId or the ResourceLinkId
        Assignment assignment = new Assignment();
        assignment.setAssignmentId(assignmentDto.getAssignmentId());
        assignment.setTitle(assignmentDto.getTitle());
        assignment.setAssignmentOrder(assignmentDto.getAssignmentOrder());
        assignment.setSoftDeleted(assignmentDto.getSoftDeleted());
        Optional<Exposure> exposure = allRepositories.exposureRepository.findById(assignmentDto.getExposureId());
        if(exposure.isPresent()) {
            assignment.setExposure(exposure.get());
        } else {
            throw new DataServiceException("The exposure for the assignment does not exist");
        }
        return assignment;
    }

    @Override
    public Assignment save(Assignment assignment) { return allRepositories.assignmentRepository.save(assignment); }

    @Override
    public Optional<Assignment> findById(Long id) { return allRepositories.assignmentRepository.findById(id); }

    @Override
    public void saveAndFlush(Assignment assignmentToChange) { allRepositories.assignmentRepository.saveAndFlush(assignmentToChange); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.assignmentRepository.deleteByAssignmentId(id); }

    @Override
    public boolean assignmentBelongsToExperimentAndExposure(Long experimentId, Long exposureId, Long assignmentId) {
        return allRepositories.assignmentRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(experimentId, exposureId, assignmentId);
    }

    @Override
    public boolean assignmentBelongsToExperiment(Long experimentId, Long assignmentId) {
        return allRepositories.assignmentRepository.existsByExposure_Experiment_ExperimentIdAndAssignmentId(experimentId,assignmentId); }

    @Override
    public String lineItemId(Assignment assignment) throws ConnectionException {
        Experiment experiment = assignment.getExposure().getExperiment();
        LTIToken ltiToken = advantageAGSService.getToken("lineitems", experiment.getPlatformDeployment());
        //find the right id to pass based on the assignment
        LineItems lineItems = advantageAGSService.getLineItems(ltiToken, experiment.getLtiContextEntity());
        for (LineItem lineItem:lineItems.getLineItemList()){
            if (lineItem.getResourceLinkId().equals(assignment.getResourceLinkId())){
                return lineItem.getId();
            }
        }
        return null;
    }

    @Override
    public void sendAssignmentGradeToCanvas(Assignment assignment) throws ConnectionException, DataServiceException {
        List<Submission> submissionList = allRepositories.submissionRepository.findByAssessment_Treatment_Assignment_AssignmentId(assignment.getAssignmentId());
        for (Submission submission:submissionList){
            submissionService.sendSubmissionGradeToCanvas(submission);
        }
    }

    @Override
    public Assessment getAssessmentbyGroupId(Long experimentId, String canvasAssignmentId, Long groupId) throws AssessmentNotMatchingException {
        Assignment assignment = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId, canvasAssignmentId);
        if (assignment==null){
            throw new AssessmentNotMatchingException("This assignment does not exist in Terracotta for this experiment");
        }
        Optional<ExposureGroupCondition> exposureGroupCondition = allRepositories.exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(groupId, assignment.getExposure().getExposureId());
        if (!exposureGroupCondition.isPresent()){
            throw new AssessmentNotMatchingException("This assignment has not a condition assigned for the participant group.");
        }
        List<Treatment> treatments = allRepositories.treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentId(exposureGroupCondition.get().getCondition().getConditionId(), assignment.getAssignmentId());
        if (treatments.isEmpty()){
            throw new AssessmentNotMatchingException("This assignment has not a treatment assigned.");
        }
        if (treatments.size()>1){  //Should never happen
            throw new AssessmentNotMatchingException("This assignment has ambiguous treatments. Please contact Terracotta administrator");
        }
        if (treatments.get(0).getAssessment()==null){
            throw new AssessmentNotMatchingException("The treatment for this assignment has not any assessment created");
        }
        return treatments.get(0).getAssessment();
    }

    @Override
    @Transactional
    public ResponseEntity<Object> launchAssignment(Long experimentId, SecuredInfo securedInfo) throws AssessmentNotMatchingException, ParticipantNotUpdatedException, AssignmentDatesException, DataServiceException, CanvasApiException, IOException {
        Optional<Experiment> experiment = experimentService.findById(experimentId);
        if (experiment.isPresent()) {
            List<Participant> participants = participantService.refreshParticipants(experimentId,securedInfo, experiment.get().getParticipants());
            Participant participant = participantService.findParticipant(participants, securedInfo.getUserId());
            //1. Check if the student has the consent signed.
            //    If not, return error with the right message
            if (participant.getConsent() ==null){
                return new ResponseEntity(TextConstants.CONSENT_PENDING, HttpStatus.UNAUTHORIZED);
            }
            //2. Check if the student is in a group (and if not assign it)
            //     If not add the student to the right group
            if (participant.getGroup()==null){
                if (experiment.get().getDistributionType().equals(DistributionTypes.CUSTOM)){
                    return new ResponseEntity(TextConstants.GROUP_PENDING, HttpStatus.UNAUTHORIZED);
                } else { // We assign it to the more unbalanced group
                    participant.setGroup(groupService.nextGroup(experiment.get()));
                    participant = participantService.save(participant);
                }
            }
            //3. Check the assessment that belongs to this student
            Assessment assessment = getAssessmentbyGroupId(experimentId, securedInfo.getCanvasAssignmentId() , participant.getGroup().getGroupId());
            //4. Maybe create the submission and return it (it must include info about the assessment)
            // First, try to find the submissions for this assessment and participant.
            List<Submission> submissionList = submissionService.findByParticipantIdAndAssessmentId(participant.getParticipantId(), assessment.getAssessmentId());
            if (!submissionList.isEmpty()) {
                for (Submission submission : submissionList) {
                    //   - if one of them is not submitted, (and we can use it, we need to return that one),
                    if (submission.getDateSubmitted() == null) {
                        if (!submissionService.datesAllowed(experimentId, assessment.getTreatment().getTreatmentId(), securedInfo)) {
                            submissionService.finalizeAndGrade(submission.getSubmissionId(), securedInfo);//We close it... and we need to save it.
                        } else {
                            //   if one is not submitted and you can't open it again,
                            if (submission.getAssessment().getAutoSubmit()) {
                                //if (submission.getAssessment().getNumOfSubmissions() == 0 || submission.getAssessment().getNumOfSubmissions() > submissionList.size()) {
                                    // TODO: it should ask the user (you have an ongoing submission, opening a new one will send the current... do you want to proceed?  or
                                //   submissionService.finalizeAndGrade(submission.getSubmissionId(), securedInfo);
                                //} else {
                                    // TODO: you have an ongoing submission that was not submitted. Do you want to submit it now)
                                submissionService.finalizeAndGrade(submission.getSubmissionId(), securedInfo);
                                // }
                            } else {
                                return new ResponseEntity<>(submissionService.toDto(submission, true, false), HttpStatus.OK);
                            }
                        }
                    }
                }
            }
            if (assessment.getNumOfSubmissions() == 0 || assessment.getNumOfSubmissions() > submissionList.size()) {
                //If it is the first submission in the experiment we mark it as started.
                if (experiment.get().getStarted()==null){
                    experiment.get().setStarted(Timestamp.valueOf(LocalDateTime.now()));
                    experimentService.save(experiment.get());
                }
                return createSubmission(experimentId, assessment, participant, securedInfo);
            } else {
                return new ResponseEntity(TextConstants.LIMIT_OF_SUBMISSIONS_REACHED, HttpStatus.UNAUTHORIZED);
            }
        } else { //Shouldn't happen
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING, HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public void checkAndRestoreAllAssignmentsInCanvas() throws CanvasApiException, DataServiceException, ConnectionException {
        List<PlatformDeployment> allDeployments = allRepositories.platformDeploymentRepository.findAll();
        for (PlatformDeployment platformDeployment:allDeployments){
            checkAndRestoreAssignmentsInCanvas(platformDeployment.getKeyId());
        }
    }

    @Override
    public void checkAndRestoreAssignmentsInCanvas(Long platformDeploymentKeyId) throws CanvasApiException, DataServiceException, ConnectionException {
        List<Assignment> assignmentsToCheck = allRepositories.assignmentRepository.findAssignmentsToCheckByPlatform(platformDeploymentKeyId);
        for (Assignment assignment:assignmentsToCheck){
            if (!checkCanvasAssignmentExists(assignment)){
                restoreAssignmentInCanvas(assignment);
            }
        }
    }

    @Override
    public void checkAndRestoreAssignmentsInCanvasByContext(Long contextId) throws CanvasApiException, DataServiceException, ConnectionException {
        List<Assignment> assignmentsToCheck = allRepositories.assignmentRepository.findAssignmentsToCheckByContext(contextId);
        for (Assignment assignment:assignmentsToCheck){
            if (!checkCanvasAssignmentExists(assignment)){
                restoreAssignmentInCanvas(assignment);
            }
        }
    }

    @Override
    public boolean checkCanvasAssignmentExists(Assignment assignment) throws CanvasApiException {
        String canvasCourseId = StringUtils.substringBetween(assignment.getExposure().getExperiment().getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");
        return canvasAPIClient.checkAssignmentExists(Integer.parseInt(assignment.getLmsAssignmentId()), canvasCourseId, assignment.getExposure().getExperiment().getPlatformDeployment() ).isPresent();
    }

    @Override
    public Assignment restoreAssignmentInCanvas(Assignment assignment) throws CanvasApiException, DataServiceException, ConnectionException {
        //1 Create the new Assignment in Canvas
        AssignmentExtended canvasAssignment = new AssignmentExtended();
        edu.ksu.canvas.model.assignment.Assignment.ExternalToolTagAttribute canvasExternalToolTagAttributes = canvasAssignment.new ExternalToolTagAttribute();
        canvasExternalToolTagAttributes.setUrl(localUrl + "/lti3?experiment=" + assignment.getExposure().getExperiment().getExperimentId() + "&assignment=" + assignment.getAssignmentId());
        canvasAssignment.setExternalToolTagAttributes(canvasExternalToolTagAttributes);
        canvasAssignment.setName(assignment.getTitle());
        canvasAssignment.setDescription(null); //We don't want a description for this assignment.
        //TODO... if we restore it... should we publish it? Only if it has submissions.
        long submissions = allRepositories.submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(assignment.getAssignmentId());
        canvasAssignment.setPublished(submissions > 0);
        canvasAssignment.setPointsPossible(100.0);
        canvasAssignment.setSubmissionTypes(Collections.singletonList("external_tool"));
        String canvasCourseId = StringUtils.substringBetween(assignment.getExposure().getExperiment().getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");

        Optional<AssignmentExtended> canvasAssignmentReturned = canvasAPIClient.createCanvasAssignment(canvasAssignment,
                canvasCourseId,
                assignment.getExposure().getExperiment().getPlatformDeployment());
        assignment.setLmsAssignmentId(Integer.toString(canvasAssignmentReturned.get().getId()));
        String jwtTokenAssignment = canvasAssignmentReturned.get().getSecureParams();
        String resourceLinkId = apijwtService.unsecureToken(jwtTokenAssignment).getBody().get("lti_assignment_id").toString();
        assignment.setResourceLinkId(resourceLinkId);
        save(assignment);

        // Now we should send the grades back to canvas...
        sendAssignmentGradeToCanvas(assignment);

        return assignment;
    }

    private ResponseEntity<Object> createSubmission(Long experimentId, Assessment assessment, Participant participant, SecuredInfo securedInfo) {
        if (submissionService.datesAllowed(experimentId,assessment.getTreatment().getTreatmentId(),securedInfo)){
            Submission submission = submissionService.createNewSubmission(assessment, participant, securedInfo);
            SubmissionDto submissionDto = submissionService.toDto(submission,true, false);
            return new ResponseEntity<>(submissionDto,HttpStatus.OK);
        } else {
            return new ResponseEntity<>(TextConstants.ASSIGNMENT_LOCKED, HttpStatus.UNAUTHORIZED);
        }
    }

}
