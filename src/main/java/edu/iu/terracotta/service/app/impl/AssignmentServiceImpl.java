package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ags.LineItem;
import edu.iu.terracotta.model.ags.LineItems;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
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
import edu.iu.terracotta.service.caliper.CaliperService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.service.lti.AdvantageAGSService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Exposure;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@SuppressWarnings({"rawtypes", "unchecked"})
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
    CaliperService caliperService;

    @Autowired
    CanvasAPIClient canvasAPIClient;

    @Autowired
    GroupService groupService;

    @Autowired
    APIJWTService apijwtService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${application.url}")
    private String localUrl;

    static final Logger log = LoggerFactory.getLogger(AssignmentServiceImpl.class);

    @Override
    public List<Assignment> findAllByExposureId(long exposureId, boolean includeDeleted) {
        return allRepositories.assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(exposureId, includeDeleted);
    }

    @Override
    public List<AssignmentDto> getAssignments(Long exposureId, String canvasCourseId, long platformDeploymentId, boolean submissions, boolean includeDeleted)
            throws AssessmentNotMatchingException, CanvasApiException{
        List<Assignment> assignments = findAllByExposureId(exposureId, includeDeleted);

        if (CollectionUtils.isEmpty(assignments)) {
            return Collections.emptyList();
        }

        List<AssignmentDto> assignmentDtoList = new ArrayList<>();

        for(Assignment assignment : assignments){
            setAssignmentDtoAttrs(assignment, canvasCourseId, platformDeploymentId);

            assignmentDtoList.add(toDto(assignment, submissions, true));
        }

        return assignmentDtoList;
    }

    @Override
    public AssignmentDto postAssignment(AssignmentDto assignmentDto, long experimentId, String canvasCourseId,
            long exposureId, long platformDeploymentId) throws IdInPostException, DataServiceException, TitleValidationException,
            AssignmentNotCreatedException, AssessmentNotMatchingException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException {
        if (assignmentDto.getAssignmentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }
        validateTitle(assignmentDto.getTitle());
        validateMultipleAttemptsSettings(assignmentDto);
        validateRevealAssignmentResponsesSettings(assignmentDto);
        assignmentDto.setExposureId(exposureId);
        Assignment assignment;
        try {
            assignment = fromDto(assignmentDto);
        } catch (DataServiceException e) {
            throw new DataServiceException("Error 105: Unable to create Assignment: " + e.getMessage());
        }
        Assignment assignmentSaved = save(assignment);
        createAssignmentInCanvas(assignmentSaved, experimentId, canvasCourseId);
        saveAndFlush(assignmentSaved);

        setAssignmentDtoAttrs(assignmentSaved, canvasCourseId, platformDeploymentId);

        return toDto(assignmentSaved, false, true);
    }

    @Override
    public AssignmentDto toDto(Assignment assignment, boolean submissions, boolean addTreatmentDto) throws AssessmentNotMatchingException {
        AssignmentDto assignmentDto = new AssignmentDto();
        assignmentDto.setAssignmentId(assignment.getAssignmentId());
        assignmentDto.setLmsAssignmentId(assignment.getLmsAssignmentId());
        assignmentDto.setTitle(assignment.getTitle());
        assignmentDto.setAssignmentOrder(assignment.getAssignmentOrder());
        assignmentDto.setExposureId(assignment.getExposure().getExposureId());
        assignmentDto.setResourceLinkId(assignment.getResourceLinkId());
        assignmentDto.setSoftDeleted(assignment.getSoftDeleted());
        assignmentDto.setNumOfSubmissions(assignment.getNumOfSubmissions());
        assignmentDto.setHoursBetweenSubmissions(assignment.getHoursBetweenSubmissions());
        assignmentDto.setMultipleSubmissionScoringScheme(assignment.getMultipleSubmissionScoringScheme().name());
        assignmentDto.setCumulativeScoringInitialPercentage(assignment.getCumulativeScoringInitialPercentage());
        assignmentDto.setAllowStudentViewResponses(assignment.isAllowStudentViewResponses());
        assignmentDto.setStudentViewResponsesAfter(assignment.getStudentViewResponsesAfter());
        assignmentDto.setStudentViewResponsesBefore(assignment.getStudentViewResponsesBefore());
        assignmentDto.setAllowStudentViewCorrectAnswers(assignment.isAllowStudentViewCorrectAnswers());
        assignmentDto.setStudentViewCorrectAnswersAfter(assignment.getStudentViewCorrectAnswersAfter());
        assignmentDto.setStudentViewCorrectAnswersBefore(assignment.getStudentViewCorrectAnswersBefore());

        long submissionsCount = allRepositories.submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(assignment.getAssignmentId());

        if(submissionsCount > 0){
            assignmentDto.setStarted(true);
        }

        if (addTreatmentDto) {
            List<Treatment> treatments = allRepositories.treatmentRepository.findByAssignment_AssignmentId(assignment.getAssignmentId());
            List<TreatmentDto> treatmentDtoList = new ArrayList<>();

            for (Treatment treatment : treatments){
                TreatmentDto treatmentDto = treatmentService.toDto(treatment, submissions, false);
                treatmentDtoList.add(treatmentDto);
            }

            assignmentDto.setTreatments(treatmentDtoList);
        }

        assignmentDto.setPublished(assignment.isPublished());
        assignmentDto.setDueDate(assignment.getDueDate());

        return assignmentDto;
    }

    @Override
    public Assignment fromDto(AssignmentDto assignmentDto) throws DataServiceException {

        //Note: we don't want to allow the dto to change the LmsAssignmentId or the ResourceLinkId
        Assignment assignment = new Assignment();
        assignment.setAssignmentId(assignmentDto.getAssignmentId());
        assignment.setTitle(assignmentDto.getTitle());
        assignment.setAssignmentOrder(assignmentDto.getAssignmentOrder());
        assignment.setNumOfSubmissions(assignmentDto.getNumOfSubmissions());
        assignment.setHoursBetweenSubmissions(assignmentDto.getHoursBetweenSubmissions());
        assignment.setMultipleSubmissionScoringScheme(
                MultipleSubmissionScoringScheme.valueOf(assignmentDto.getMultipleSubmissionScoringScheme()));
        assignment.setCumulativeScoringInitialPercentage(assignmentDto.getCumulativeScoringInitialPercentage());
        assignment.setAllowStudentViewResponses(assignmentDto.isAllowStudentViewResponses());
        assignment.setStudentViewResponsesAfter(assignmentDto.getStudentViewResponsesAfter());
        assignment.setStudentViewResponsesBefore(assignmentDto.getStudentViewResponsesBefore());
        assignment.setAllowStudentViewCorrectAnswers(assignmentDto.isAllowStudentViewCorrectAnswers());
        assignment.setStudentViewCorrectAnswersAfter(assignmentDto.getStudentViewCorrectAnswersAfter());
        assignment.setStudentViewCorrectAnswersBefore(assignmentDto.getStudentViewCorrectAnswersBefore());
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
    public Assignment getAssignment(Long id){ return allRepositories.assignmentRepository.findByAssignmentId(id); }

    @Override
    public List<AssignmentDto> updateAssignments(List<AssignmentDto> assignmentDtos, String canvasCourseId)
            throws TitleValidationException, CanvasApiException, AssignmentNotEditedException,
                RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException {
        List<AssignmentDto> updatedAssignmentDtos = new ArrayList<>();

        for (AssignmentDto assignmentDto : assignmentDtos) {
            updatedAssignmentDtos.add(updateAssignment(assignmentDto.getAssignmentId(), assignmentDto, canvasCourseId));
        }

        return updatedAssignmentDtos;
    }

    @Override
    public AssignmentDto updateAssignment(Long id, AssignmentDto assignmentDto, String canvasCourseId)
            throws TitleValidationException, CanvasApiException, AssignmentNotEditedException,
                RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException {
        Assignment assignment = allRepositories.assignmentRepository.findByAssignmentId(id);
        if(StringUtils.isAllBlank(assignmentDto.getTitle()) && StringUtils.isAllBlank(assignment.getTitle())){
            throw new TitleValidationException("Error 100: Please give the assignment a name.");
        }
        if(!StringUtils.isAllBlank(assignmentDto.getTitle()) && assignmentDto.getTitle().length() > 255) {
            throw new TitleValidationException("Error 101: The title must be 255 characters or less.");
        }
        if (!assignment.getTitle().equals(assignmentDto.getTitle())) {
            assignment.setTitle(assignmentDto.getTitle());
            editAssignmentNameInCanvas(assignment,canvasCourseId,assignmentDto.getTitle());
        }
        validateMultipleAttemptsSettings(assignmentDto);
        validateRevealAssignmentResponsesSettings(assignmentDto);
        assignment.setAssignmentOrder(assignmentDto.getAssignmentOrder());
        assignment.setSoftDeleted(assignmentDto.getSoftDeleted());
        assignment.setAllowStudentViewResponses(assignmentDto.isAllowStudentViewResponses());
        assignment.setStudentViewResponsesAfter(assignmentDto.getStudentViewResponsesAfter());
        assignment.setStudentViewResponsesBefore(assignmentDto.getStudentViewResponsesBefore());
        assignment.setAllowStudentViewCorrectAnswers(assignmentDto.isAllowStudentViewCorrectAnswers());
        assignment.setStudentViewCorrectAnswersAfter(assignmentDto.getStudentViewCorrectAnswersAfter());
        assignment.setStudentViewCorrectAnswersBefore(assignmentDto.getStudentViewCorrectAnswersBefore());
        assignment.setNumOfSubmissions(assignmentDto.getNumOfSubmissions());
        assignment.setHoursBetweenSubmissions(assignmentDto.getHoursBetweenSubmissions());
        MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme
                .valueOf(assignmentDto.getMultipleSubmissionScoringScheme());
        assignment.setMultipleSubmissionScoringScheme(multipleSubmissionScoringScheme);
        assignment.setCumulativeScoringInitialPercentage(assignmentDto.getCumulativeScoringInitialPercentage());
        Assignment updatedAssignment = saveAndFlush(assignment);

        // update the same settings on all of this assignment's assessments
        List<Assessment> assessments = allRepositories.assessmentRepository.findByTreatment_Assignment_AssignmentId(id);
        for (Assessment assessment : assessments) {
            assessment.setAllowStudentViewResponses(assignmentDto.isAllowStudentViewResponses());
            assessment.setStudentViewResponsesAfter(assignmentDto.getStudentViewResponsesAfter());
            assessment.setStudentViewResponsesBefore(assignmentDto.getStudentViewResponsesBefore());
            assessment.setAllowStudentViewCorrectAnswers(assignmentDto.isAllowStudentViewCorrectAnswers());
            assessment.setStudentViewCorrectAnswersAfter(assignmentDto.getStudentViewCorrectAnswersAfter());
            assessment.setStudentViewCorrectAnswersBefore(assignmentDto.getStudentViewCorrectAnswersBefore());
            assessment.setNumOfSubmissions(assignmentDto.getNumOfSubmissions());
            assessment.setHoursBetweenSubmissions(assignmentDto.getHoursBetweenSubmissions());
            assessment.setMultipleSubmissionScoringScheme(multipleSubmissionScoringScheme);
            assessment.setCumulativeScoringInitialPercentage(assignmentDto.getCumulativeScoringInitialPercentage());
        }

        return toDto(updatedAssignment, false, true);
    }

    @Override
    public Assignment saveAndFlush(Assignment assignmentToChange) { return allRepositories.assignmentRepository.saveAndFlush(assignmentToChange); }

    @Override
    public void deleteById(Long id, String canvasCourseId) throws EmptyResultDataAccessException, CanvasApiException, AssignmentNotEditedException {
        Assignment assignment = allRepositories.assignmentRepository.getOne(id);
        deleteAssignmentInCanvas(assignment, canvasCourseId);

        long submissionsCount = allRepositories.submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(id);

        if (submissionsCount  == 0l) {
            // no submissions; hard delete
            allRepositories.assignmentRepository.deleteByAssignmentId(id);
            return;
        }

        // has submissions; soft delete
        assignment.setSoftDeleted(true);
        saveAndFlush(assignment);
    }

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
    public void sendAssignmentGradeToCanvas(Assignment assignment) throws ConnectionException, DataServiceException, CanvasApiException, IOException {
        List<Submission> submissionList = allRepositories.submissionRepository.findByAssessment_Treatment_Assignment_AssignmentId(assignment.getAssignmentId());
        for (Submission submission:submissionList){
            submissionService.sendSubmissionGradeToCanvasWithLTI(submission, false);
        }
    }

    @Override
    public Assessment getAssessmentByGroupId(Long experimentId, String canvasAssignmentId, Long groupId) throws AssessmentNotMatchingException {
        Assignment assignment = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId, canvasAssignmentId);
        if (assignment==null){
            throw new AssessmentNotMatchingException("Error 127: This assignment does not exist in Terracotta for this experiment");
        }
        Optional<ExposureGroupCondition> exposureGroupCondition = allRepositories.exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(groupId, assignment.getExposure().getExposureId());
        if (!exposureGroupCondition.isPresent()){
            throw new AssessmentNotMatchingException("Error 130: This assignment does not have a condition assigned for the participant group.");
        }
        List<Treatment> treatments = allRepositories.treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentId(exposureGroupCondition.get().getCondition().getConditionId(), assignment.getAssignmentId());
        if (treatments.isEmpty()){
            throw new AssessmentNotMatchingException("Error 131: This assignment does not have a treatment assigned.");
        }
        if (treatments.size()>1){  //Should never happen
            throw new AssessmentNotMatchingException("Error 132: This assignment has ambiguous treatments. Please contact a Terracotta administrator");
        }
        if (treatments.get(0).getAssessment()==null){
            throw new AssessmentNotMatchingException("Error 133: The treatment for this assignment does not have an assessment created");
        }
        return treatments.get(0).getAssessment();
    }

    @Override
    public Assessment getAssessmentByConditionId(Long experimentId, String canvasAssignmentId, Long conditionId) throws AssessmentNotMatchingException {
        Assignment assignment = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId, canvasAssignmentId);
        if (assignment==null){
            throw new AssessmentNotMatchingException("Error 127: This assignment does not exist in Terracotta for this experiment");
        }
        List<Treatment> treatments = allRepositories.treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentId(conditionId, assignment.getAssignmentId());
        if (treatments.isEmpty()){
            throw new AssessmentNotMatchingException("Error 131: This assignment does not have a treatment assigned.");
        }
        if (treatments.size()>1){  //Should never happen
            throw new AssessmentNotMatchingException("Error 132: This assignment has ambiguous treatments. Please contact a Terracotta administrator");
        }
        if (treatments.get(0).getAssessment()==null){
            throw new AssessmentNotMatchingException("Error 133: The treatment for this assignment has not any assessment created");
        }
        return treatments.get(0).getAssessment();
    }

    @Override
    public Group getUniqueGroupByConditionId(Long experimentId, String canvasAssignmentId, Long conditionId) throws GroupNotMatchingException {
        Assignment assignment = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId, canvasAssignmentId);
        Optional<ExposureGroupCondition> exposureGroupCondition = allRepositories.exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(conditionId, assignment.getExposure().getExposureId());
        if (!exposureGroupCondition.isPresent()){
            throw new GroupNotMatchingException("Error 130: This assignment does not have a condition assigned for the participant group.");
        }
        return exposureGroupCondition.get().getGroup();
    }

    @Override
    @Transactional
    public ResponseEntity<Object> launchAssignment(Long experimentId, SecuredInfo securedInfo) throws
            AssessmentNotMatchingException, ParticipantNotUpdatedException, AssignmentDatesException,
            DataServiceException, CanvasApiException, IOException, GroupNotMatchingException,
            ParticipantNotMatchingException, ConnectionException, AssignmentAttemptException {
        Optional<Experiment> experiment = experimentService.findById(experimentId);
        if (experiment.isPresent()) {
            List<Participant> participants = participantService.refreshParticipants(experimentId,securedInfo, experiment.get().getParticipants());
            Participant participant = participantService.findParticipant(participants, securedInfo.getUserId());
            //1. Check if the student has the consent signed.
            //    If not, set it as no participant
            if (participant==null){
                throw new ParticipantNotMatchingException(TextConstants.PARTICIPANT_NOT_MATCHING);
            }
            if (participant.getConsent() ==null || (!participant.getConsent() && participant.getDateRevoked()==null)) {
                if (experiment.get().getParticipationType().equals(ParticipationTypes.AUTO)){
                    participant.setConsent(true);
                    participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
                } else {
                    participant.setConsent(false);
                    participant.setDateRevoked(new Timestamp(System.currentTimeMillis()));

                }
            }
            //2. Check if the student is in a group (and if not assign it to the right one if consent == true)
            if (participant.getConsent()) {
                if (participant.getGroup() == null) {
                    if (experiment.get().getDistributionType().equals(DistributionTypes.CUSTOM)) {
                        for (Condition condition : experiment.get().getConditions()) {
                            if (condition.getDefaultCondition()) {
                                participant.setGroup(getUniqueGroupByConditionId(experimentId, securedInfo.getCanvasAssignmentId(), condition.getConditionId()));
                                break;
                            }
                        }
                    } else { // We assign it to the more unbalanced group (if consent is true)
                        participant.setGroup(groupService.nextGroup(experiment.get()));
                    }
                }
            }
            participant = participantService.save(participant);
            //3. Check the assessment that belongs to this student
            Assessment assessment = null;
            if (!participant.getConsent()){
                //We need the default condition assessment
                for (Condition condition:experiment.get().getConditions()){
                    if (condition.getDefaultCondition()){
                        assessment = getAssessmentByConditionId(experimentId, securedInfo.getCanvasAssignmentId(), condition.getConditionId());
                        break;
                    }
                }
            } else {
                if (participant.getGroup() != null) {
                    assessment = getAssessmentByGroupId(experimentId, securedInfo.getCanvasAssignmentId(), participant.getGroup().getGroupId());
                }   // There is no possible else... but if we arrive to here, then assessment will be null
            }
            if (assessment == null) {
                throw new AssessmentNotMatchingException("There is no assessment available for this user");

            }
            //4. Maybe create the submission and return it (it must include info about the assessment)
            // First, try to find the submissions for this assessment and participant.
            List<Submission> submissionList = submissionService.findByParticipantIdAndAssessmentId(participant.getParticipantId(), assessment.getAssessmentId());

            if (!submissionList.isEmpty()) {
                for (Submission submission : submissionList) {
                    //   - if one of them is not submitted, (and we can use it, we need to return that one),
                    if (submission.getDateSubmitted() == null) {
                        // if (!submissionService.datesAllowed(experimentId, assessment.getTreatment().getTreatmentId(), securedInfo)) {
                        //     submissionService.finalizeAndGrade(submission.getSubmissionId(), securedInfo);//We close it... and we need to save it.
                        // } else {
                            //   if one is not submitted and you can't open it again,
                            // if (submission.getAssessment().getAutoSubmit()) {
                                //if (submission.getAssessment().getNumOfSubmissions() == 0 || submission.getAssessment().getNumOfSubmissions() > submissionList.size()) {
                                    // TODO: it should ask the user (you have an ongoing submission, opening a new one will send the current... do you want to proceed?  or
                                //   submissionService.finalizeAndGrade(submission.getSubmissionId(), securedInfo);
                                //} else {
                                    // TODO: you have an ongoing submission that was not submitted. Do you want to submit it now)
                                // submissionService.finalizeAndGrade(submission.getSubmissionId(), securedInfo);
                                // }
                            // } else {
                                caliperService.sendAssignmentRestarted(submission, securedInfo);
                                return new ResponseEntity<>(submissionService.toDto(submission, true, false), HttpStatus.OK);
                            // }
                        // }
                    }
                }
            }
            try {
                verifyAssignmentSubmissionLimit(assessment.getNumOfSubmissions(), submissionList.size());
                verifySubmissionWaitTime(assessment.getHoursBetweenSubmissions(), submissionList);

                // If it is the first submission in the experiment we mark it as started.
                if (experiment.get().getStarted()==null){
                    experiment.get().setStarted(Timestamp.valueOf(LocalDateTime.now()));
                    experimentService.save(experiment.get());
                }

                return createSubmission(experimentId, assessment, participant, securedInfo);
            } catch (AssignmentAttemptException e) {
                return new ResponseEntity(e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        } else { //Shouldn't happen
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING, HttpStatus.UNAUTHORIZED);
        }
    }

    private void verifyAssignmentSubmissionLimit(Integer limit, int existingSubmissionsCount) throws AssignmentAttemptException {
        if (limit == null || limit == 0) {
            return;
        }

        if (existingSubmissionsCount < limit) {
            return;
        }

        throw new AssignmentAttemptException(TextConstants.LIMIT_OF_SUBMISSIONS_REACHED);
    }

    private void verifySubmissionWaitTime(Float waitTime, List<Submission> submissionList) throws AssignmentAttemptException {
        if (waitTime == null || waitTime == 0F) {
            return;
        }

        if (CollectionUtils.isEmpty(submissionList)) {
            return;
        }

        // calculate the allowable submission time limit
        Timestamp limit = Timestamp.from(Instant.now().minus(Math.round(waitTime * 60 * 60), ChronoUnit.SECONDS));

        // check for any submissions after the allowable time
        long afterLimitCount = submissionList.stream().filter(submission -> { return submission.getDateSubmitted().after(limit); }).count();

        if (afterLimitCount != 0l) {
            // there are existing submissions that are not passed the time limit
            throw new AssignmentAttemptException(TextConstants.ASSIGNMENT_SUBMISSION_WAIT_TIME_NOT_REACHED);
        }
    }

    @Override
    public void checkAndRestoreAllAssignmentsInCanvas() throws CanvasApiException, DataServiceException, ConnectionException, IOException {
        List<PlatformDeployment> allDeployments = allRepositories.platformDeploymentRepository.findAll();
        for (PlatformDeployment platformDeployment:allDeployments){
            checkAndRestoreAssignmentsInCanvas(platformDeployment.getKeyId());
        }
    }

    @Override
    public void checkAndRestoreAssignmentsInCanvas(Long platformDeploymentKeyId) throws CanvasApiException, DataServiceException, ConnectionException, IOException {
        List<Assignment> assignmentsToCheck = allRepositories.assignmentRepository.findAssignmentsToCheckByPlatform(platformDeploymentKeyId);
        for (Assignment assignment:assignmentsToCheck){
            if (!checkCanvasAssignmentExists(assignment)){
                restoreAssignmentInCanvas(assignment);
            }
        }
    }

    @Override
    public void checkAndRestoreAssignmentsInCanvasByContext(Long contextId) throws CanvasApiException, DataServiceException, ConnectionException, IOException {
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
    public Assignment restoreAssignmentInCanvas(Assignment assignment) throws CanvasApiException, DataServiceException, ConnectionException, IOException {
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
        canvasAssignment.setGradingType("percent");
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
        if (!submissionService.datesAllowed(experimentId,assessment.getTreatment().getTreatmentId(),securedInfo)){
            return new ResponseEntity<>(TextConstants.ASSIGNMENT_LOCKED, HttpStatus.UNAUTHORIZED);
        }

        Submission submission = submissionService.createNewSubmission(assessment, participant, securedInfo);
        caliperService.sendAssignmentStarted(submission, securedInfo);
        SubmissionDto submissionDto = submissionService.toDto(submission, true, false);

        return new ResponseEntity<>(submissionDto,HttpStatus.OK);
    }

    @Override
    public void validateTitle(String title) throws TitleValidationException{
        if(!StringUtils.isAllBlank(title) && title.length() > 255){
            throw new TitleValidationException("Error 101: Assignment title must be 255 characters or less.");
        }
    }

    private void validateMultipleAttemptsSettings(AssignmentDto assignmentDto)
            throws MultipleAttemptsSettingsValidationException {
        MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme
                .valueOf(assignmentDto.getMultipleSubmissionScoringScheme());
        // validate that if multipleSubmissionScoringScheme is CUMULATIVE, then
        // cumulativeScoringInitialPercentage is not null
        if (multipleSubmissionScoringScheme == MultipleSubmissionScoringScheme.CUMULATIVE && assignmentDto.getCumulativeScoringInitialPercentage() == null){
            throw new MultipleAttemptsSettingsValidationException(
                    "Error 156: Must set cumulative scoring initial percentage when scoring scheme is CUMULATIVE");
        }

        // validate that if multipleSubmissionScoringScheme is CUMULATIVE, then
        // numOfSubmissions is not null and greater than 1
        if (multipleSubmissionScoringScheme == MultipleSubmissionScoringScheme.CUMULATIVE && (assignmentDto.getNumOfSubmissions() == null || assignmentDto.getNumOfSubmissions() <= 1)) {
            throw new MultipleAttemptsSettingsValidationException(
                    "Error 157: Number of submissions must be greater than 1, but not infinite, when scoring scheme is CUMULATIVE");
        }
    }

    private void validateRevealAssignmentResponsesSettings(AssignmentDto assignmentDto)
            throws RevealResponsesSettingValidationException {

        // validate that if allowStudentViewCorrectAnswers then also
        // allowStudentViewResponses must be true
        if (assignmentDto.isAllowStudentViewCorrectAnswers() && !assignmentDto.isAllowStudentViewResponses()) {
            throw new RevealResponsesSettingValidationException(
                    "Error 151: Cannot allow students to view correct answers if they are not allowed to view responses.");
        }
        // Validate that view responses 'after' date comes before the 'before' date
        if (assignmentDto.getStudentViewResponsesAfter() != null
                && assignmentDto.getStudentViewResponsesBefore() != null
                && !assignmentDto.getStudentViewResponsesAfter()
                        .before(assignmentDto.getStudentViewResponsesBefore())) {
            throw new RevealResponsesSettingValidationException(
                    "Error 152: Start date of revealing student responses must come before end date.");
        }
        // Validate that view correct answers 'after' date comes before the 'before'
        // date
        if (assignmentDto.getStudentViewCorrectAnswersAfter() != null
                && assignmentDto.getStudentViewCorrectAnswersBefore() != null
                && !assignmentDto.getStudentViewCorrectAnswersAfter()
                        .before(assignmentDto.getStudentViewCorrectAnswersBefore())) {
            throw new RevealResponsesSettingValidationException(
                    "Error 153: Start date of revealing correct answers must come before end date.");
        }
        // Validate studentViewCorrectAnswersAfter is greater than or equal to
        // studentViewResponsesAfter
        if (assignmentDto.getStudentViewCorrectAnswersAfter() != null
                && assignmentDto.getStudentViewResponsesAfter() != null && !(assignmentDto
                        .getStudentViewCorrectAnswersAfter().equals(assignmentDto.getStudentViewResponsesAfter())
                        || assignmentDto.getStudentViewCorrectAnswersAfter()
                                .after(assignmentDto.getStudentViewResponsesAfter()))) {

            throw new RevealResponsesSettingValidationException(
                    "Error 154: Start date of revealing correct answers must equal or come after start date of revealing student responses.");
        }
        // Validate studentViewCorrectAnswersBefore is less than or equal to
        // studentViewResponsesBefore
        if (assignmentDto.getStudentViewCorrectAnswersBefore() != null
                && assignmentDto.getStudentViewResponsesBefore() != null && !(assignmentDto
                        .getStudentViewCorrectAnswersBefore().equals(assignmentDto.getStudentViewResponsesBefore())
                        || assignmentDto.getStudentViewCorrectAnswersBefore()
                                .before(assignmentDto.getStudentViewResponsesBefore()))) {

            throw new RevealResponsesSettingValidationException(
                    "Error 155: End date of revealing correct answers must equal or come before end date of revealing student responses.");
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long exposureId, long assignmentId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/exposures/{exposure_id}/assignments/{assignment_id}")
                .buildAndExpand(experimentId, exposureId, assignmentId).toUri());
        return headers;
    }

    @Override
    public void createAssignmentInCanvas(Assignment assignment, long experimentId, String canvasCourseId) throws AssignmentNotCreatedException{
        AssignmentExtended canvasAssignment = new AssignmentExtended();
        edu.ksu.canvas.model.assignment.Assignment.ExternalToolTagAttribute canvasExternalToolTagAttributes = canvasAssignment.new ExternalToolTagAttribute();
        canvasExternalToolTagAttributes.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath().path("/lti3?experiment=" + experimentId + "&assignment=" + assignment.getAssignmentId()).build().toUriString());
        canvasAssignment.setExternalToolTagAttributes(canvasExternalToolTagAttributes);
        canvasAssignment.setName(assignment.getTitle());
        canvasAssignment.setDescription(null);
        canvasAssignment.setPublished(false);
        canvasAssignment.setGradingType("percent");
        canvasAssignment.setPointsPossible(100.0);
        canvasAssignment.setSubmissionTypes(Collections.singletonList("external_tool"));
        try {
            Optional<AssignmentExtended> canvasAssignmentReturned = canvasAPIClient.createCanvasAssignment(canvasAssignment,
                    canvasCourseId,
                    assignment.getExposure().getExperiment().getPlatformDeployment());
            assignment.setLmsAssignmentId(Integer.toString(canvasAssignmentReturned.get().getId()));
            String jwtTokenAssignment = canvasAssignmentReturned.get().getSecureParams();
            String resourceLinkId = apijwtService.unsecureToken(jwtTokenAssignment).getBody().get("lti_assignment_id").toString();
            assignment.setResourceLinkId(resourceLinkId);
        } catch (CanvasApiException e) {
            log.info("Create the assignment failed");
            e.printStackTrace();
            throw new AssignmentNotCreatedException("Error 137: The assignment was not created.");
        }
    }

    @Override
    public void editAssignmentNameInCanvas(Assignment assignment, String canvasCourseId, String newName) throws AssignmentNotEditedException, CanvasApiException {
        int assignmentId = Integer.parseInt(assignment.getLmsAssignmentId());
        Optional<AssignmentExtended> assignmentExtendedOptional = canvasAPIClient.listAssignment(canvasCourseId,assignmentId,assignment.getExposure().getExperiment().getPlatformDeployment());
        if (!assignmentExtendedOptional.isPresent()){
            throw new AssignmentNotEditedException("Error 136: The assignment is not linked to any Canvas assignment");
        }
        AssignmentExtended assignmentExtended = assignmentExtendedOptional.get();
        assignmentExtended.setName(newName);
        try {
            canvasAPIClient.editAssignment(assignmentExtended,
                    canvasCourseId,
                    assignment.getExposure().getExperiment().getPlatformDeployment());
        } catch (CanvasApiException e) {
            log.info("Edit the assignment failed");
            e.printStackTrace();
            throw new AssignmentNotEditedException("Error 137: The assignment was not created.");
        }
    }

    @Override
    public void deleteAssignmentInCanvas(Assignment assignment, String canvasCourseId) throws AssignmentNotEditedException, CanvasApiException {
        int assignmentId = Integer.parseInt(assignment.getLmsAssignmentId());
        Optional<AssignmentExtended> assignmentExtendedOptional = canvasAPIClient.listAssignment(canvasCourseId,assignmentId,assignment.getExposure().getExperiment().getPlatformDeployment());
        if (!assignmentExtendedOptional.isPresent()){
            log.warn("The assignment " + assignment.getTitle() + " (canvas id:" + assignment.getLmsAssignmentId() + ") was already deleted");
            return;
        }
        AssignmentExtended assignmentExtended = assignmentExtendedOptional.get();
        try {
            canvasAPIClient.deleteAssignment(assignmentExtended,
                    canvasCourseId,
                    assignment.getExposure().getExperiment().getPlatformDeployment());
        } catch (CanvasApiException e) {
            log.info("Deleting the assignment failed");
            e.printStackTrace();
            throw new AssignmentNotEditedException("Error 137: The assignment was not created.");
        }
    }

    @Override
    public void deleteAllFromExperiment(Long id, SecuredInfo securedInfo) {
        List<Assignment> assignmentList = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentId(id);
        for (Assignment assignment:assignmentList) {
            try {
                deleteAssignmentInCanvas(assignment,securedInfo.getCanvasCourseId());
            } catch (CanvasApiException | AssignmentNotEditedException e) {
                log.warn("Assignment : " + assignment.getTitle() + "was not deleted in canvas");
                e.printStackTrace();
            }
        }
    }

    @Override
    public AssignmentDto duplicateAssignment(long assignmentId, String canvasCourseId, long platformDeploymentId) throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                                                                        AssignmentNotCreatedException, RevealResponsesSettingValidationException,
                                                                        MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException {
        Assignment from = getAssignment(assignmentId);

        if (from == null) {
            throw new DataServiceException("The assignment with the given ID does not exist");
        }

        entityManager.detach(from);

        // reset ID
        from.setAssignmentId(null);

        Assignment newAssignment = save(from);
        setAssignmentDtoAttrs(newAssignment, canvasCourseId, platformDeploymentId);
        AssignmentDto assignmentDto = toDto(newAssignment, false, true);

        return assignmentDto;
    }

    @Override
    public void setAssignmentDtoAttrs(Assignment assignment, String canvasCourseId, long platformDeploymentId) throws NumberFormatException, CanvasApiException {
        PlatformDeployment platformDeployment = allRepositories.platformDeploymentRepository.getOne(platformDeploymentId);
        Optional<AssignmentExtended> canvasAssignment = canvasAPIClient.listAssignment(canvasCourseId, Integer.parseInt(assignment.getLmsAssignmentId()), platformDeployment);

        if (canvasAssignment.isPresent()) {
            assignment.setPublished(canvasAssignment.get().isPublished());
            assignment.setDueDate(canvasAssignment.get().getDueAt());
        }
    }

}
