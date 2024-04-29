package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.AssignmentMoveException;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.canvas.CanvasAPITokenEntity;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.AssignmentTreatmentService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.caliper.CaliperService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@SuppressWarnings(
    {
        "rawtypes", "unchecked", "PMD.GuardLogStatement", "PMD.PreserveStackTrace",
        "squid:S125", "squid:S2229", "squid:S2629", "squid:S1192", "squid:S1612"
    }
)
public class AssignmentServiceImpl implements AssignmentService {

    @Autowired private AllRepositories allRepositories;
    @Autowired private APIJWTService apijwtService;
    @Autowired private AssessmentService assessmentService;
    @Autowired private AssignmentTreatmentService assignmentTreatmentService;
    @Autowired private CaliperService caliperService;
    @Autowired private CanvasAPIClient canvasAPIClient;
    @Autowired private ParticipantService participantService;
    @Autowired private SubmissionService submissionService;

    @PersistenceContext private EntityManager entityManager;

    private static final int MAX_TITLE_LENGTH = 255;

    @Override
    public List<Assignment> findAllByExposureId(long exposureId, boolean includeDeleted) {
        return allRepositories.assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(exposureId, includeDeleted);
    }

    @Override
    public List<AssignmentDto> getAssignments(Long exposureId, boolean submissions, boolean includeDeleted, SecuredInfo securedInfo)
            throws AssessmentNotMatchingException, CanvasApiException{
        List<Assignment> assignments = findAllByExposureId(exposureId, includeDeleted);

        if (CollectionUtils.isEmpty(assignments)) {
            return Collections.emptyList();
        }

        List<AssignmentDto> assignmentDtoList = new ArrayList<>();
        LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        for (Assignment assignment : assignments) {
            if (instructorUser != null) {
                assignmentTreatmentService.setAssignmentDtoAttrs(assignment, securedInfo.getCanvasCourseId(), instructorUser);
            }

            assignmentDtoList.add(assignmentTreatmentService.toAssignmentDto(assignment, submissions, true));
        }

        return assignmentDtoList;
    }

    @Override
    public AssignmentDto postAssignment(AssignmentDto assignmentDto, long experimentId, long exposureId, SecuredInfo securedInfo)
            throws IdInPostException, DataServiceException, TitleValidationException,
                AssignmentNotCreatedException, AssessmentNotMatchingException, RevealResponsesSettingValidationException,
                MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException {
        if (assignmentDto.getAssignmentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        Assignment assignment = createAssignment(assignmentDto, experimentId, securedInfo.getCanvasCourseId(), exposureId, instructorUser);

        assignmentTreatmentService.setAssignmentDtoAttrs(assignment, securedInfo.getCanvasCourseId(), instructorUser);

        return assignmentTreatmentService.toAssignmentDto(assignment, false, true);
    }

    private Assignment createAssignment(AssignmentDto assignmentDto, long experimentId, String canvasCourseId, long exposureId, LtiUserEntity instructorUser)
            throws IdInPostException, DataServiceException, TitleValidationException, AssignmentNotCreatedException, RevealResponsesSettingValidationException,
                MultipleAttemptsSettingsValidationException, NumberFormatException {
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
        createAssignmentInCanvas(instructorUser, assignmentSaved, experimentId, canvasCourseId);

        return saveAndFlush(assignmentSaved);
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
        assignment.setMultipleSubmissionScoringScheme(MultipleSubmissionScoringScheme.valueOf(assignmentDto.getMultipleSubmissionScoringScheme()));
        assignment.setCumulativeScoringInitialPercentage(assignmentDto.getCumulativeScoringInitialPercentage());
        assignment.setAllowStudentViewResponses(assignmentDto.isAllowStudentViewResponses());
        assignment.setStudentViewResponsesAfter(assignmentDto.getStudentViewResponsesAfter());
        assignment.setStudentViewResponsesBefore(assignmentDto.getStudentViewResponsesBefore());
        assignment.setAllowStudentViewCorrectAnswers(assignmentDto.isAllowStudentViewCorrectAnswers());
        assignment.setStudentViewCorrectAnswersAfter(assignmentDto.getStudentViewCorrectAnswersAfter());
        assignment.setStudentViewCorrectAnswersBefore(assignmentDto.getStudentViewCorrectAnswersBefore());
        Optional<Exposure> exposure = allRepositories.exposureRepository.findById(assignmentDto.getExposureId());

        if (!exposure.isPresent()) {
            throw new DataServiceException("The exposure for the assignment does not exist");
        }

        assignment.setExposure(exposure.get());

        return assignment;
    }

    @Override
    public Assignment save(Assignment assignment) {
        return allRepositories.assignmentRepository.save(assignment);
    }

    @Override
    public Optional<Assignment> findById(Long id) {
        return allRepositories.assignmentRepository.findById(id);
    }

    @Override
    public Assignment getAssignment(Long id) {
        return allRepositories.assignmentRepository.findByAssignmentId(id);
    }

    @Override
    public List<AssignmentDto> updateAssignments(List<AssignmentDto> assignmentDtos, SecuredInfo securedInfo)
            throws TitleValidationException, CanvasApiException, AssignmentNotEditedException, RevealResponsesSettingValidationException,
                    MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException {
        List<AssignmentDto> updatedAssignmentDtos = new ArrayList<>();

        for (AssignmentDto assignmentDto : assignmentDtos) {
            updatedAssignmentDtos.add(putAssignment(assignmentDto.getAssignmentId(), assignmentDto, securedInfo));
        }

        return updatedAssignmentDtos;
    }

    @Override
    public AssignmentDto putAssignment(Long id, AssignmentDto assignmentDto, SecuredInfo securedInfo)
            throws TitleValidationException, CanvasApiException, AssignmentNotEditedException,
                RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException {
        return assignmentTreatmentService.toAssignmentDto(updateAssignment(id, assignmentDto, securedInfo), false, true);
    }

    @Override
    public Assignment updateAssignment(Long id, AssignmentDto assignmentDto, SecuredInfo securedInfo)
            throws TitleValidationException, CanvasApiException, AssignmentNotEditedException,
                RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException {
        Assignment assignment = allRepositories.assignmentRepository.findByAssignmentId(id);

        if (assignment == null) {
            throw new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING);
        }

        if (StringUtils.isAllBlank(assignmentDto.getTitle(), assignment.getTitle())) {
            throw new TitleValidationException("Error 100: Please give the assignment a name.");
        }

        if (StringUtils.isNotBlank(assignmentDto.getTitle()) && assignmentDto.getTitle().length() > 255) {
            throw new TitleValidationException("Error 101: The title must be 255 characters or less.");
        }

        if (!assignment.getTitle().equals(assignmentDto.getTitle())) {
            assignment.setTitle(assignmentDto.getTitle());
            LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
            editAssignmentNameInCanvas(assignment, securedInfo.getCanvasCourseId(), assignmentDto.getTitle(), instructorUser);
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
        MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.valueOf(assignmentDto.getMultipleSubmissionScoringScheme());
        assignment.setMultipleSubmissionScoringScheme(multipleSubmissionScoringScheme);
        assignment.setCumulativeScoringInitialPercentage(assignmentDto.getCumulativeScoringInitialPercentage());
        Assignment updatedAssignment = saveAndFlush(assignment);

        // update the same settings on all of this assignment's assessments
        for (Assessment assessment : allRepositories.assessmentRepository.findByTreatment_Assignment_AssignmentId(id)) {
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

        return updatedAssignment;
    }

    @Override
    public Assignment saveAndFlush(Assignment assignmentToChange) {
        return allRepositories.assignmentRepository.saveAndFlush(assignmentToChange);
    }

    @Override
    public void deleteById(Long id, SecuredInfo securedInfo) throws EmptyResultDataAccessException, CanvasApiException, AssignmentNotEditedException {
        Assignment assignment = allRepositories.assignmentRepository.getReferenceById(id);
        LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        deleteAssignmentInCanvas(assignment, securedInfo.getCanvasCourseId(), instructorUser);

        long submissionsCount = allRepositories.submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(id);

        if (submissionsCount == 0l) {
            // no submissions; hard delete

            // first, delete questions
            List<Assessment> assessments = allRepositories.assessmentRepository.findByTreatment_Assignment_AssignmentId(id);

            CollectionUtils.emptyIfNull(assessments).stream()
                .filter(assessment -> CollectionUtils.isNotEmpty(assessment.getQuestions()))
                .forEach(assessment ->
                    assessment.getQuestions().forEach(question -> {
                        allRepositories.questionRepository.deleteByQuestionId(question.getQuestionId());
                    }
                )
            );

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
        return allRepositories.assignmentRepository.existsByExposure_Experiment_ExperimentIdAndAssignmentId(experimentId,assignmentId);
    }

    @Override
    public void sendAssignmentGradeToCanvas(Assignment assignment) throws ConnectionException, DataServiceException, CanvasApiException, IOException {
        List<Submission> submissionList = allRepositories.submissionRepository.findByAssessment_Treatment_Assignment_AssignmentId(assignment.getAssignmentId());
        for (Submission submission:submissionList) {
            submissionService.sendSubmissionGradeToCanvasWithLTI(submission, false);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Object> launchAssignment(Long experimentId, SecuredInfo securedInfo)
            throws AssessmentNotMatchingException, ParticipantNotUpdatedException, AssignmentDatesException, DataServiceException, CanvasApiException, IOException, GroupNotMatchingException,
                ParticipantNotMatchingException, ConnectionException, AssignmentAttemptException, AssignmentNotMatchingException, ExperimentNotMatchingException {
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(experimentId);

        if (!experiment.isPresent()) {
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING, HttpStatus.UNAUTHORIZED);
        }

        Participant participant = participantService.handleExperimentParticipant(experiment.get(), securedInfo);

        //3. Check the assessment that belongs to this student
        Assessment assessment = assessmentService.getAssessmentForParticipant(participant, securedInfo);

        //4. Maybe create the submission and return it (it must include info about the assessment)
        // First, try to find the submissions for this assessment and participant.
        List<Submission> submissionList = allRepositories.submissionRepository.findByParticipant_ParticipantIdAndAssessment_AssessmentId(participant.getParticipantId(), assessment.getAssessmentId());

        if (CollectionUtils.isNotEmpty(submissionList)) {
            for (Submission submission : submissionList) {
                //   - if one of them is not submitted, (and we can use it, we need to return that one),
                if (submission.getDateSubmitted() == null) {
                    AtomicInteger answerSubmissionCount = new AtomicInteger(0);
                    submission.getQuestionSubmissions()
                        .forEach(
                            questionSubmission -> {
                                answerSubmissionCount.addAndGet(allRepositories.answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId()).size());
                                answerSubmissionCount.addAndGet(allRepositories.answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId()).size());
                                answerSubmissionCount.addAndGet(allRepositories.answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId()).size());
                            }
                        );

                    if (answerSubmissionCount.get() == assessment.getQuestions().size()) {
                        // all questions have an answer; finalize and grade
                        submissionService.finalizeAndGrade(
                            submission.getSubmissionId(),
                            securedInfo,
                            apijwtService.isLearner(securedInfo) && !apijwtService.isInstructorOrHigher(securedInfo)
                        );
                        log.info("Previous assessment ID: [{}] has an incomplete submission ID: [{}]. Regrading and finalizing.", assessment.getAssessmentId(), submission.getSubmissionId());
                        continue;
                    }

                    caliperService.sendAssignmentRestarted(submission, securedInfo);

                    return new ResponseEntity<>(submissionService.toDto(submission, true, false), HttpStatus.OK);
                }
            }
        }

        try {
            assessmentService.verifySubmissionLimit(assessment.getNumOfSubmissions(), submissionList.size());
            assessmentService.verifySubmissionWaitTime(assessment.getHoursBetweenSubmissions(), submissionList);

            // If it is the first submission in the experiment mark it as started.
            if (experiment.get().getStarted() == null) {
                experiment.get().setStarted(Timestamp.valueOf(LocalDateTime.now()));
                allRepositories.experimentRepository.save(experiment.get());
            }

            return createSubmission(experimentId, assessment, participant, securedInfo);
        } catch (AssignmentAttemptException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public void checkAndRestoreAllAssignmentsInCanvas() throws CanvasApiException, DataServiceException, ConnectionException, IOException {
        List<PlatformDeployment> allDeployments = allRepositories.platformDeploymentRepository.findAll();

        for (PlatformDeployment platformDeployment:allDeployments) {
            checkAndRestoreAssignmentsInCanvas(platformDeployment.getKeyId());
        }
    }

    @Override
    @Transactional(rollbackFor = { CanvasApiException.class })
    public void checkAndRestoreAssignmentsInCanvas(Long platformDeploymentKeyId) throws CanvasApiException, DataServiceException, ConnectionException, IOException {
        List<Assignment> assignmentsToCheck = allRepositories.assignmentRepository.findAssignmentsToCheckByPlatform(platformDeploymentKeyId);

        for (Assignment assignment:assignmentsToCheck) {
            LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(
                assignment.getExposure().getExperiment().getCreatedBy().getUserKey(),
                platformDeploymentKeyId
            );

            if (!checkCanvasAssignmentExists(assignment, instructorUser)) {
                restoreAssignmentInCanvas(assignment);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = { CanvasApiException.class })
    public List<AssignmentExtended> checkAndRestoreAssignmentsInCanvasByContext(SecuredInfo securedInfo) throws CanvasApiException, DataServiceException, ConnectionException, IOException {
        List<Assignment> assignmentsToCheck = allRepositories.assignmentRepository.findAssignmentsToCheckByContext(securedInfo.getContextId());

        if (CollectionUtils.isEmpty(assignmentsToCheck)) {
            log.info("No assignments exist in Terracotta for context ID: '{}' to check in Canvas. Aborting.", securedInfo.getContextId());
            return Collections.emptyList();
        }

        log.info("Checking Terracotta assignment IDs for context ID: '{}' in Canvas: {}",
            securedInfo.getContextId(),
            Arrays.toString(assignmentsToCheck.stream().map(Assignment::getLmsAssignmentId).toArray())
        );

        List<AssignmentExtended> canvasAssignments = getAllAssignmentsForCanvasCourse(securedInfo);

        if (CollectionUtils.isEmpty(canvasAssignments)) {
            log.info("No assignments exist in Canvas for context ID: '{}. Aborting.", securedInfo.getContextId());
            return Collections.emptyList();
        }

        List<Integer> canvasAssignmentIds = canvasAssignments.stream()
            .map(AssignmentExtended::getId)
            .toList();

        List<Long> assignmentsRecreated = assignmentsToCheck.stream()
            .filter(assignmentToCheck -> !canvasAssignmentIds.contains(Integer.parseInt(assignmentToCheck.getLmsAssignmentId())))
            .map(
                assignmentToCreate -> {
                    log.info("Creating assignment in Canvas: {}", assignmentToCreate.getAssignmentId());

                    try {
                        restoreAssignmentInCanvas(assignmentToCreate);
                    } catch (CanvasApiException | DataServiceException | ConnectionException | IOException e) {
                        log.error("Error restoring assignments in Canvas");
                    }

                    return assignmentToCreate.getAssignmentId();
                }
            )
            .toList();

        log.info("Checking Terracotta assignments for context ID: '{}' in Canvas COMPLETE. Assignments recreated: {}.",
            securedInfo.getContextId(),
            CollectionUtils.isNotEmpty(assignmentsRecreated) ?
                assignmentsRecreated.stream()
                    .map(l -> { return Long.toString(l); })
                    .collect(Collectors.joining(", ")) :
                "N/A"
        );

        return canvasAssignments;
    }

    @Override
    public List<AssignmentExtended> getAllAssignmentsForCanvasCourse(SecuredInfo securedInfo) throws CanvasApiException {
        LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        return canvasAPIClient.listAssignments(instructorUser, securedInfo.getCanvasCourseId());
    }

    @Override
    public Optional<AssignmentExtended> getCanvasAssignmentById(int assignmentId, SecuredInfo securedInfo) throws CanvasApiException {
        LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        return canvasAPIClient.listAssignment(instructorUser, securedInfo.getCanvasCourseId(), assignmentId);
    }

    @Override
    public boolean checkCanvasAssignmentExists(Assignment assignment, LtiUserEntity instructorUser) throws CanvasApiException {
        String canvasCourseId = StringUtils.substringBetween(
                assignment.getExposure().getExperiment().getLtiContextEntity().getContext_memberships_url(), "courses/",
                "/names");

        Optional<CanvasAPITokenEntity> apiToken = allRepositories.canvasAPITokenRepository.findByUser(instructorUser);

        if (!apiToken.isPresent()) {
            // instructor does not have an API token; skip processing
            return true;
        }

        return canvasAPIClient.checkAssignmentExists(instructorUser, Integer.parseInt(assignment.getLmsAssignmentId()), canvasCourseId).isPresent();
    }

    @Override
    public Assignment restoreAssignmentInCanvas(Assignment assignment)
            throws CanvasApiException, DataServiceException, ConnectionException, IOException {
        //1 Create the new Assignment in Canvas
        AssignmentExtended canvasAssignment = new AssignmentExtended();
        edu.ksu.canvas.model.assignment.Assignment.ExternalToolTagAttribute canvasExternalToolTagAttributes = canvasAssignment.new ExternalToolTagAttribute();
        canvasExternalToolTagAttributes.setUrl(assignment.getExposure().getExperiment().getPlatformDeployment().getLocalUrl() + "/lti3?experiment=" + assignment.getExposure().getExperiment().getExperimentId() + "&assignment=" + assignment.getAssignmentId());
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

        Optional<AssignmentExtended> canvasAssignmentReturned = canvasAPIClient.createCanvasAssignment(assignment.getExposure().getExperiment().getCreatedBy(), canvasAssignment, canvasCourseId);
        assignment.setLmsAssignmentId(Integer.toString(canvasAssignmentReturned.get().getId()));
        String jwtTokenAssignment = canvasAssignmentReturned.get().getSecureParams();
        String resourceLinkId = apijwtService.unsecureToken(jwtTokenAssignment).get("lti_assignment_id").toString();
        assignment.setResourceLinkId(resourceLinkId);
        save(assignment);

        // Now we should send the grades back to canvas...
        sendAssignmentGradeToCanvas(assignment);

        return assignment;
    }

    private ResponseEntity<Object> createSubmission(Long experimentId, Assessment assessment, Participant participant, SecuredInfo securedInfo) {
        if (!submissionService.datesAllowed(experimentId,assessment.getTreatment().getTreatmentId(),securedInfo)) {
            return new ResponseEntity<>(TextConstants.ASSIGNMENT_LOCKED, HttpStatus.UNAUTHORIZED);
        }

        Submission submission = submissionService.createNewSubmission(assessment, participant, securedInfo);
        caliperService.sendAssignmentStarted(submission, securedInfo);
        SubmissionDto submissionDto = submissionService.toDto(submission, true, false);

        return new ResponseEntity<>(submissionDto,HttpStatus.OK);
    }

    @Override
    public void validateTitle(String title) throws TitleValidationException {
        if (StringUtils.isNotBlank(title) && title.length() > MAX_TITLE_LENGTH) {
            throw new TitleValidationException(String.format("Error 101: Assignment title must not be empty and %s characters or less.", MAX_TITLE_LENGTH));
        }
    }

    private void validateMultipleAttemptsSettings(AssignmentDto assignmentDto)
            throws MultipleAttemptsSettingsValidationException {
        MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.valueOf(assignmentDto.getMultipleSubmissionScoringScheme());

        // validate that if multipleSubmissionScoringScheme is CUMULATIVE, then cumulativeScoringInitialPercentage is not null
        if (multipleSubmissionScoringScheme == MultipleSubmissionScoringScheme.CUMULATIVE && assignmentDto.getCumulativeScoringInitialPercentage() == null) {
            throw new MultipleAttemptsSettingsValidationException(
                    "Error 156: Must set cumulative scoring initial percentage when scoring scheme is CUMULATIVE");
        }

        // validate that if multipleSubmissionScoringScheme is CUMULATIVE, then numOfSubmissions is not null and greater than 1
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
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long exposureId, long assignmentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/exposures/{exposureId}/assignments/{assignmentId}")
                .buildAndExpand(experimentId, exposureId, assignmentId).toUri());

        return headers;
    }

    @Override
    public void createAssignmentInCanvas(LtiUserEntity instructorUser, Assignment assignment, long experimentId, String canvasCourseId) throws AssignmentNotCreatedException {
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
            Optional<AssignmentExtended> canvasAssignmentReturned = canvasAPIClient.createCanvasAssignment(instructorUser, canvasAssignment, canvasCourseId);
            assignment.setLmsAssignmentId(Integer.toString(canvasAssignmentReturned.get().getId()));
            String jwtTokenAssignment = canvasAssignmentReturned.get().getSecureParams();
            String resourceLinkId = apijwtService.unsecureToken(jwtTokenAssignment).get("lti_assignment_id").toString();
            assignment.setResourceLinkId(resourceLinkId);
        } catch (CanvasApiException e) {
            log.error("Create the assignment failed", e);
            throw new AssignmentNotCreatedException("Error 137: The assignment was not created.");
        }
    }

    @Override
    public void editAssignmentNameInCanvas(Assignment assignment, String canvasCourseId, String newName, LtiUserEntity instructorUser) throws AssignmentNotEditedException, CanvasApiException {
        int assignmentId = Integer.parseInt(assignment.getLmsAssignmentId());
        Optional<AssignmentExtended> assignmentExtendedOptional = canvasAPIClient.listAssignment(instructorUser, canvasCourseId, assignmentId);

        if (!assignmentExtendedOptional.isPresent()) {
            throw new AssignmentNotEditedException("Error 136: The assignment is not linked to any Canvas assignment");
        }

        AssignmentExtended assignmentExtended = assignmentExtendedOptional.get();
        assignmentExtended.setName(newName);

        try {
            canvasAPIClient.editAssignment(instructorUser, assignmentExtended, canvasCourseId);
        } catch (CanvasApiException e) {
            log.error("Edit the assignment failed", e);
            throw new AssignmentNotEditedException("Error 137: The assignment was not created.");
        }
    }

    @Override
    public void deleteAssignmentInCanvas(Assignment assignment, String canvasCourseId, LtiUserEntity instructorUser) throws AssignmentNotEditedException, CanvasApiException {
        int assignmentId = Integer.parseInt(assignment.getLmsAssignmentId());
        Optional<AssignmentExtended> assignmentExtendedOptional = canvasAPIClient.listAssignment(instructorUser, canvasCourseId, assignmentId);

        if (!assignmentExtendedOptional.isPresent()) {
            log.warn("The assignment '{}' (canvas id: '{}') was already deleted", assignment.getTitle(), assignment.getLmsAssignmentId());
            return;
        }

        try {
            canvasAPIClient.deleteAssignment(instructorUser, assignmentExtendedOptional.get(), canvasCourseId);
        } catch (CanvasApiException e) {
            log.error("Deleting the assignment failed", e);
            throw new AssignmentNotEditedException("Error 137: The assignment was not created.");
        }
    }

    @Override
    public void deleteAllFromExperiment(Long id, SecuredInfo securedInfo) {
        List<Assignment> assignmentList = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentId(id);

        if (CollectionUtils.isEmpty(assignmentList)) {
            return;
        }

        for (Assignment assignment : assignmentList) {
            try {
                deleteAssignmentInCanvas(
                    assignment,
                    securedInfo.getCanvasCourseId(),
                    allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId())
                );
            } catch (CanvasApiException | AssignmentNotEditedException e) {
                log.warn("Assignment : {} was not deleted in canvas", assignment.getTitle());
            }
        }
    }

    @Override
    public AssignmentDto duplicateAssignment(long assignmentId, SecuredInfo securedInfo)
            throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                    AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
                    NumberFormatException, CanvasApiException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException {
        Assignment from = getAssignment(assignmentId);

        if (from == null) {
            throw new DataServiceException("The assignment with the given ID does not exist");
        }

        AssignmentDto fromDto = assignmentTreatmentService.toAssignmentDto(from, false, false);

        // reset assignment details
        fromDto.setAssignmentId(null);
        fromDto.setLmsAssignmentId(null);
        fromDto.setPublished(false);
        fromDto.setResourceLinkId(null);
        fromDto.setStarted(false);

        // add the "Copy of" prefix; truncate to max title length if needed
        fromDto.setTitle(StringUtils.truncate(String.format("%s %s", TextConstants.DUPLICATE_PREFIX, from.getTitle()), MAX_TITLE_LENGTH));

        Assignment newAssignment = createAssignment(
            fromDto,
            from.getExposure().getExperiment().getExperimentId(),
            securedInfo.getCanvasCourseId(),
            from.getExposure().getExposureId(),
            allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId())
        );

        // duplicate treatments
        for (Treatment treatment : allRepositories.treatmentRepository.findByAssignment_AssignmentId(assignmentId)) {
            assignmentTreatmentService.duplicateTreatment(treatment.getTreatmentId(), newAssignment, securedInfo);
        }

        assignmentTreatmentService.setAssignmentDtoAttrs(newAssignment, securedInfo.getCanvasCourseId(), allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId()));

        return assignmentTreatmentService.toAssignmentDto(newAssignment, false, true);
    }

    @Override
    public AssignmentDto moveAssignment(long originalAssignmentId, AssignmentDto targetAssignmentDto, long experimentId, long originalExposureId, SecuredInfo securedInfo)
            throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                AssignmentNotCreatedException, RevealResponsesSettingValidationException,
                MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException,
                ExceedingLimitException, TreatmentNotMatchingException, ExposureNotMatchingException, AssignmentMoveException, AssignmentNotEditedException, QuestionNotMatchingException {
        if (originalExposureId == targetAssignmentDto.getExposureId().longValue()) {
            // cannot move assignment; original and target exposures are the same
            throw new AssignmentMoveException(TextConstants.UNABLE_TO_MOVE_ASSIGNMENT_EXPOSURE_SAME);
        }

        Exposure exposure = allRepositories.exposureRepository.findByExposureId(targetAssignmentDto.getExposureId());

        if (exposure == null) {
            throw new ExposureNotMatchingException(TextConstants.EXPOSURE_NOT_MATCHING);
        }

        // reset ID
        targetAssignmentDto.setAssignmentId(null);

        // create new assignment
        LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        Assignment newAssignment = createAssignment(targetAssignmentDto, experimentId, securedInfo.getCanvasCourseId(), targetAssignmentDto.getExposureId(), instructorUser);
        assignmentTreatmentService.setAssignmentDtoAttrs(newAssignment, securedInfo.getCanvasCourseId(), instructorUser);

        // duplicate treatments from original assignment
       for (Treatment treatment : allRepositories.treatmentRepository.findByAssignment_AssignmentId(originalAssignmentId)) {
            assignmentTreatmentService.duplicateTreatment(treatment.getTreatmentId(), newAssignment, securedInfo);
        }

        // delete original assignment
        deleteById(originalAssignmentId, securedInfo);

        return assignmentTreatmentService.toAssignmentDto(newAssignment, false, true);
    }

}
