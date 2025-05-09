package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiTokenEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiTokenRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.dto.AssignmentDto;
import edu.iu.terracotta.dao.model.dto.SubmissionDto;
import edu.iu.terracotta.dao.model.enums.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.dao.repository.AnswerEssaySubmissionRepository;
import edu.iu.terracotta.dao.repository.AnswerFileSubmissionRepository;
import edu.iu.terracotta.dao.repository.AnswerMcSubmissionRepository;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.AssignmentMoveException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.AssignmentTreatmentService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.integrations.IntegrationLaunchService;
import edu.iu.terracotta.service.app.integrations.IntegrationTokenService;
import edu.iu.terracotta.service.caliper.CaliperService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@SuppressWarnings(
    {
        "rawtypes", "unchecked", "PMD.GuardLogStatement", "PMD.PreserveStackTrace",
        "squid:S125", "squid:S2229", "squid:S2629", "squid:S1192", "squid:S1612"
    }
)
public class AssignmentServiceImpl implements AssignmentService {

    private static final int MAX_TITLE_LENGTH = 255;

    @Autowired private AnswerEssaySubmissionRepository answerEssaySubmissionRepository;
    @Autowired private AnswerFileSubmissionRepository answerFileSubmissionRepository;
    @Autowired private AnswerMcSubmissionRepository answerMcSubmissionRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ApiTokenRepository apiTokenRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ExposureRepository exposureRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private ApiJwtService apiJwtService;
    @Autowired private AssessmentService assessmentService;
    @Autowired private AssignmentTreatmentService assignmentTreatmentService;
    @Autowired private CaliperService caliperService;
    @Autowired private ApiClient apiClient;
    @Autowired private IntegrationLaunchService integrationLaunchService;
    @Autowired private IntegrationTokenService integrationTokenService;
    @Autowired private ParticipantService participantService;
    @Autowired private SubmissionService submissionService;

    @PersistenceContext private EntityManager entityManager;

    @Override
    public List<Assignment> findAllByExposureId(long exposureId, boolean includeDeleted) {
        return assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(exposureId, includeDeleted);
    }

    @Override
    public List<AssignmentDto> getAssignments(Long exposureId, boolean submissions, boolean includeDeleted, SecuredInfo securedInfo)
            throws AssessmentNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        List<Assignment> assignments = findAllByExposureId(exposureId, includeDeleted);

        if (CollectionUtils.isEmpty(assignments)) {
            return Collections.emptyList();
        }

        List<AssignmentDto> assignmentDtoList = new ArrayList<>();
        LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        for (Assignment assignment : assignments) {
            if (instructorUser != null) {
                assignmentTreatmentService.setAssignmentDtoAttrs(assignment, securedInfo.getLmsCourseId(), instructorUser);
            }

            assignmentDtoList.add(assignmentTreatmentService.toAssignmentDto(assignment, submissions, true));
        }

        return assignmentDtoList;
    }

    @Override
    public AssignmentDto postAssignment(AssignmentDto assignmentDto, long experimentId, long exposureId, SecuredInfo securedInfo)
            throws IdInPostException, DataServiceException, TitleValidationException,
                AssignmentNotCreatedException, AssessmentNotMatchingException, RevealResponsesSettingValidationException,
                MultipleAttemptsSettingsValidationException, NumberFormatException, ApiException, TerracottaConnectorException {
        if (assignmentDto.getAssignmentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        Assignment assignment = createAssignment(assignmentDto, experimentId, securedInfo.getLmsCourseId(), exposureId, instructorUser);

        assignmentTreatmentService.setAssignmentDtoAttrs(assignment, securedInfo.getLmsCourseId(), instructorUser);

        return assignmentTreatmentService.toAssignmentDto(assignment, false, true);
    }

    private Assignment createAssignment(AssignmentDto assignmentDto, long experimentId, String lmsCourseId, long exposureId, LtiUserEntity instructorUser)
            throws IdInPostException, DataServiceException, TitleValidationException, AssignmentNotCreatedException, RevealResponsesSettingValidationException,
                MultipleAttemptsSettingsValidationException, NumberFormatException, TerracottaConnectorException {
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
        createAssignmentInLms(instructorUser, assignmentSaved, experimentId, lmsCourseId);

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
        Optional<Exposure> exposure = exposureRepository.findById(assignmentDto.getExposureId());

        if (exposure.isEmpty()) {
            throw new DataServiceException("The exposure for the assignment does not exist");
        }

        assignment.setExposure(exposure.get());

        return assignment;
    }

    @Override
    public Assignment save(Assignment assignment) {
        return assignmentRepository.save(assignment);
    }

    @Override
    public Optional<Assignment> findById(Long id) {
        return assignmentRepository.findById(id);
    }

    @Override
    public Assignment getAssignment(Long id) {
        return assignmentRepository.findByAssignmentId(id);
    }

    @Override
    public List<AssignmentDto> updateAssignments(List<AssignmentDto> assignmentDtos, SecuredInfo securedInfo)
            throws TitleValidationException, ApiException, AssignmentNotEditedException, RevealResponsesSettingValidationException,
                    MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException, TerracottaConnectorException {
        List<AssignmentDto> updatedAssignmentDtos = new ArrayList<>();

        for (AssignmentDto assignmentDto : assignmentDtos) {
            updatedAssignmentDtos.add(putAssignment(assignmentDto.getAssignmentId(), assignmentDto, securedInfo));
        }

        return updatedAssignmentDtos;
    }

    @Override
    public AssignmentDto putAssignment(Long id, AssignmentDto assignmentDto, SecuredInfo securedInfo)
            throws TitleValidationException, ApiException, AssignmentNotEditedException,
                RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException, TerracottaConnectorException {
        return assignmentTreatmentService.toAssignmentDto(updateAssignment(id, assignmentDto, securedInfo), false, true);
    }

    @Override
    public Assignment updateAssignment(Long id, AssignmentDto assignmentDto, SecuredInfo securedInfo)
            throws TitleValidationException, ApiException, AssignmentNotEditedException,
                RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException, TerracottaConnectorException {
        Assignment assignment = assignmentRepository.findByAssignmentId(id);

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
            LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
            editAssignmentNameInLms(assignment, securedInfo.getLmsCourseId(), assignmentDto.getTitle(), instructorUser);
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
        for (Assessment assessment : assessmentRepository.findByTreatment_Assignment_AssignmentId(id)) {
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
        return assignmentRepository.saveAndFlush(assignmentToChange);
    }

    @Override
    public void deleteById(Long id, SecuredInfo securedInfo) throws EmptyResultDataAccessException, ApiException, AssignmentNotEditedException, TerracottaConnectorException {
        Optional<Assignment> assignment = assignmentRepository.findById(id);

        if (assignment.isEmpty()) {
            return;
        }

        LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        deleteAssignmentInLms(assignment.get(), securedInfo.getLmsCourseId(), instructorUser);

        long submissionsCount = submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(id);

        if (submissionsCount == 0l) {
            // no submissions; hard delete
            assignmentRepository.deleteById(id);
            return;
        }

        // has submissions; soft delete
        assignment.get().setSoftDeleted(true);
        saveAndFlush(assignment.get());
    }

    @Override
    public boolean assignmentBelongsToExperimentAndExposure(Long experimentId, Long exposureId, Long assignmentId) {
        return assignmentRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(experimentId, exposureId, assignmentId);
    }

    @Override
    public boolean assignmentBelongsToExperiment(Long experimentId, Long assignmentId) {
        return assignmentRepository.existsByExposure_Experiment_ExperimentIdAndAssignmentId(experimentId,assignmentId);
    }

    @Override
    public void sendAssignmentGradeToLms(Assignment assignment) throws ConnectionException, DataServiceException, ApiException, IOException, TerracottaConnectorException {
        List<Submission> submissionList = submissionRepository.findByAssessment_Treatment_Assignment_AssignmentId(assignment.getAssignmentId()).stream()
            .filter(Submission::isSubmitted)
            .toList();

        for (Submission submission : submissionList) {
            submissionService.sendSubmissionGradeToLmsWithLti(submission, false);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Object> launchAssignment(Long experimentId, SecuredInfo securedInfo)
            throws AssessmentNotMatchingException, ParticipantNotUpdatedException, AssignmentDatesException, DataServiceException, ApiException, IOException, GroupNotMatchingException,
                ParticipantNotMatchingException, ConnectionException, AssignmentAttemptException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException, IntegrationTokenNotFoundException {
        Optional<Experiment> experiment = experimentRepository.findById(experimentId);

        if (experiment.isEmpty()) {
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING, HttpStatus.UNAUTHORIZED);
        }

        Participant participant = participantService.handleExperimentParticipant(experiment.get(), securedInfo);

        //3. Check the assessment that belongs to this student
        Assessment assessment = assessmentService.getAssessmentForParticipant(participant, securedInfo);

        //4. Maybe create the submission and return it (it must include info about the assessment)
        // First, try to find the submissions for this assessment and participant.
        List<Submission> submissionList = submissionRepository.findByParticipant_ParticipantIdAndAssessment_AssessmentId(participant.getParticipantId(), assessment.getAssessmentId());

        if (CollectionUtils.isNotEmpty(submissionList)) {
            for (Submission submission : submissionList) {
                //   - if one of them is not submitted, (and we can use it, we need to return that one),
                if (submission.getDateSubmitted() == null) {
                    AtomicInteger answerSubmissionCount = new AtomicInteger(0);
                    submission.getQuestionSubmissions()
                        .forEach(
                            questionSubmission -> {
                                answerSubmissionCount.addAndGet(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId()).size());
                                answerSubmissionCount.addAndGet(answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId()).size());
                                answerSubmissionCount.addAndGet(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId()).size());
                            }
                        );

                    if (answerSubmissionCount.get() == assessment.getQuestions().size()) {
                        // all questions have an answer; finalize and grade
                        submissionService.finalizeAndGrade(
                            submission.getSubmissionId(),
                            securedInfo,
                            apiJwtService.isLearner(securedInfo) && !apiJwtService.isInstructorOrHigher(securedInfo)
                        );
                        log.info("Previous assessment ID: [{}] has an incomplete submission ID: [{}]. Regrading and finalizing.", assessment.getAssessmentId(), submission.getSubmissionId());
                        continue;
                    }

                    // create integration launch token
                    integrationTokenService.create(
                        submission,
                        securedInfo
                    );

                    // create integration launch URL
                    integrationLaunchService.buildUrl(
                        submission,
                        submissionList.size(),
                        submission.getIntegration()
                    );

                    caliperService.sendAssignmentRestarted(submission, securedInfo);

                    return new ResponseEntity<>(submissionService.toDto(submission, true, false), HttpStatus.OK);
                }
            }
        }

        try {
            assessmentService.verifySubmissionLimit(assessment.getNumOfSubmissions(), submissionList.size());
            assessmentService.verifySubmissionWaitTime(assessment.getHoursBetweenSubmissions(), submissionList);

            // if it is the first non-test student submission in the experiment mark it as started.
            if (!participant.isTestStudent() && !experiment.get().isStarted()) {
                experiment.get().setStarted(Timestamp.valueOf(LocalDateTime.now()));
                experimentRepository.save(experiment.get());
            }

            return createSubmission(experimentId, assessment, participant, securedInfo);
        } catch (AssignmentAttemptException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public void checkAndRestoreAllAssignmentsInLms() throws ApiException, DataServiceException, ConnectionException, IOException, NumberFormatException, TerracottaConnectorException {
        List<PlatformDeployment> allDeployments = platformDeploymentRepository.findAll();

        for (PlatformDeployment platformDeployment:allDeployments) {
            checkAndRestoreAssignmentsInLms(platformDeployment.getKeyId());
        }
    }

    @Override
    @Transactional(rollbackFor = { ApiException.class })
    public void checkAndRestoreAssignmentsInLms(Long platformDeploymentKeyId) throws ApiException, DataServiceException, ConnectionException, IOException, NumberFormatException, TerracottaConnectorException {
        List<Assignment> assignmentsToCheck = assignmentRepository.findAssignmentsToCheckByPlatform(platformDeploymentKeyId);

        for (Assignment assignment : assignmentsToCheck) {
            LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(
                assignment.getExposure().getExperiment().getCreatedBy().getUserKey(),
                platformDeploymentKeyId
            );

            if (!checkLmsAssignmentExists(assignment, instructorUser)) {
                restoreAssignmentInLms(assignment);
            }
        }
    }

    @Override
    public List<LmsAssignment> getAllAssignmentsForLmsCourse(SecuredInfo securedInfo) throws ApiException, TerracottaConnectorException {
        LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        return apiClient.listAssignments(instructorUser, securedInfo.getLmsCourseId());
    }

    @Override
    public Optional<LmsAssignment> getLmsAssignmentById(String lmsAssignmentId, SecuredInfo securedInfo) throws ApiException, TerracottaConnectorException {
        LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        return apiClient.listAssignment(instructorUser, securedInfo.getLmsCourseId(), lmsAssignmentId);
    }

    @Override
    public boolean checkLmsAssignmentExists(Assignment assignment, LtiUserEntity instructorUser) throws ApiException, NumberFormatException, TerracottaConnectorException {
        String lmsCourseId = StringUtils.substringBetween(
                assignment.getExposure().getExperiment().getLtiContextEntity().getContext_memberships_url(), "courses/",
                "/names");

        Optional<ApiTokenEntity> apiTokenEntity = apiTokenRepository.findByUser(instructorUser);

        return apiTokenEntity.isEmpty() || apiClient.checkAssignmentExists(instructorUser, assignment.getLmsAssignmentId(), lmsCourseId).isPresent();
    }

    @Override
    public Assignment restoreAssignmentInLms(Assignment assignment) throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        LmsAssignment lmsAssignmentReturned = apiClient.restoreAssignment(assignment);
        assignment.setLmsAssignmentId(lmsAssignmentReturned.getId());
        String jwtTokenAssignment = lmsAssignmentReturned.getSecureParams();
        String resourceLinkId = apiJwtService.unsecureToken(jwtTokenAssignment, assignment.getExposure().getExperiment().getPlatformDeployment()).get("lti_assignment_id").toString();
        assignment.setResourceLinkId(resourceLinkId);
        save(assignment);

        try {
            // send the grades back to LMS if assignment is published
            if (assignment.isPublished()) {
                sendAssignmentGradeToLms(assignment);
            }
        } catch (ApiException | ConnectionException | DataServiceException | IOException e) {
            log.error(String.format("An exception occurred sending submission grades to LMS from assignment ID: [%s] to LMS ID: [%s]", assignment.getAssignmentId(), assignment.getLmsAssignmentId()), e);
        }

        return assignment;
    }

    private ResponseEntity<Object> createSubmission(Long experimentId, Assessment assessment, Participant participant, SecuredInfo securedInfo) throws IntegrationTokenNotFoundException {
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
    public void createAssignmentInLms(LtiUserEntity instructorUser, Assignment assignment, long experimentId, String lmsCourseId) throws AssignmentNotCreatedException, TerracottaConnectorException {
        try {
            LmsAssignment lmsAssignmentReturned = apiClient.createLmsAssignment(instructorUser, assignment, lmsCourseId);
            assignment.setLmsAssignmentId(lmsAssignmentReturned.getId());
            String jwtTokenAssignment = lmsAssignmentReturned.getSecureParams();
            String resourceLinkId = apiJwtService.unsecureToken(jwtTokenAssignment, instructorUser.getPlatformDeployment()).get("lti_assignment_id").toString();
            assignment.setResourceLinkId(resourceLinkId);
        } catch (ApiException e) {
            log.error("Creating the assignment in the LMS failed", e);
            throw new AssignmentNotCreatedException("Error 137: The assignment was not created in the LMS.");
        }
    }

    @Override
    public void editAssignmentNameInLms(Assignment assignment, String lmsCourseId, String newName, LtiUserEntity instructorUser) throws AssignmentNotEditedException, ApiException, TerracottaConnectorException {
        try {
            apiClient.editAssignmentNameInLms(assignment, lmsCourseId, newName, instructorUser);
        } catch (ApiException | IOException e) {
            log.error("Editing the LMS assignment ID: [{}] failed", assignment.getAssignmentId(), e);
            throw new AssignmentNotEditedException("Error 137: The assignment was not created in the LMS.");
        }
    }

    @Override
    public void deleteAssignmentInLms(Assignment assignment, String lmsCourseId, LtiUserEntity instructorUser) throws AssignmentNotEditedException, ApiException, TerracottaConnectorException {
        try {
            apiClient.deleteAssignmentInLms(assignment, lmsCourseId, instructorUser);
        } catch (ApiException | IOException e) {
            log.error("Deleting the assignment failed", e);
            throw new AssignmentNotEditedException("Error 137: The assignment was not created.");
        }
    }

    @Override
    public void deleteAllFromExperiment(Long id, SecuredInfo securedInfo) throws TerracottaConnectorException {
        List<Assignment> assignmentList = assignmentRepository.findByExposure_Experiment_ExperimentId(id);

        if (CollectionUtils.isEmpty(assignmentList)) {
            return;
        }

        for (Assignment assignment : assignmentList) {
            try {
                deleteAssignmentInLms(
                    assignment,
                    securedInfo.getLmsCourseId(),
                    ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId())
                );
            } catch (ApiException | AssignmentNotEditedException e) {
                log.warn("Assignment : {} was not deleted in the LMS", assignment.getTitle());
            }
        }
    }

    @Override
    public AssignmentDto duplicateAssignment(long assignmentId, SecuredInfo securedInfo)
            throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                    AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
                    NumberFormatException, ApiException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException, TerracottaConnectorException {
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
            securedInfo.getLmsCourseId(),
            from.getExposure().getExposureId(),
            ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId())
        );

        // duplicate treatments
        for (Treatment treatment : treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(assignmentId)) {
            assignmentTreatmentService.duplicateTreatment(treatment.getTreatmentId(), newAssignment, securedInfo);
        }

        assignmentTreatmentService.setAssignmentDtoAttrs(newAssignment, securedInfo.getLmsCourseId(), ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId()));

        return assignmentTreatmentService.toAssignmentDto(newAssignment, false, true);
    }

    @Override
    public AssignmentDto moveAssignment(long originalAssignmentId, AssignmentDto targetAssignmentDto, long experimentId, long originalExposureId, SecuredInfo securedInfo)
            throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                AssignmentNotCreatedException, RevealResponsesSettingValidationException,
                MultipleAttemptsSettingsValidationException, NumberFormatException, ApiException, AssignmentNotMatchingException,
                ExceedingLimitException, TreatmentNotMatchingException, ExposureNotMatchingException, AssignmentMoveException, AssignmentNotEditedException, QuestionNotMatchingException {
        if (originalExposureId == targetAssignmentDto.getExposureId().longValue()) {
            // cannot move assignment; original and target exposures are the same
            throw new AssignmentMoveException(TextConstants.UNABLE_TO_MOVE_ASSIGNMENT_EXPOSURE_SAME);
        }

        Assignment assignment = assignmentRepository.findByAssignmentId(originalAssignmentId);

        if (assignment == null) {
            throw new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING);
        }

        Exposure exposure = exposureRepository.findByExposureId(targetAssignmentDto.getExposureId());

        if (exposure == null) {
            throw new ExposureNotMatchingException(TextConstants.EXPOSURE_NOT_MATCHING);
        }

        assignment.setExposure(exposure);
        assignmentRepository.save(assignment);

        return assignmentTreatmentService.toAssignmentDto(assignment, false, true);
    }

    @Override
    public boolean isSingleVersion(long assignmentId) {
        return isSingleVersion(getAssignment(assignmentId));
    }

    @Override
    public boolean isSingleVersion(Assignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment cannot be null");
        }

        List<Treatment> assignmentTreatments = treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(assignment.getAssignmentId());

        return assignmentTreatments.size() <= 1;
    }

}
