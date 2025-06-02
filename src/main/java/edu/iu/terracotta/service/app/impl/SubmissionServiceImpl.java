package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageAgsService;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.AnswerMc;
import edu.iu.terracotta.dao.entity.AnswerMcSubmissionOption;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.QuestionMc;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.entity.RegradeDetails;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.dto.SubmissionDto;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.model.enums.RegradeOption;
import edu.iu.terracotta.dao.repository.AnswerMcRepository;
import edu.iu.terracotta.dao.repository.AnswerMcSubmissionOptionRepository;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.dao.repository.SubmissionCommentRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.service.app.AssessmentSubmissionService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
import edu.iu.terracotta.service.app.SubmissionCommentService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.integrations.IntegrationLaunchService;
import edu.iu.terracotta.service.app.integrations.IntegrationTokenService;
import edu.iu.terracotta.service.caliper.CaliperService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@SuppressWarnings({"PMD.PreserveStackTrace", "PMD.GuardLogStatement", "PMD.MethodNamingConventions"})
public class SubmissionServiceImpl implements SubmissionService {

    @Autowired private AnswerMcRepository answerMcRepository;
    @Autowired private AnswerMcSubmissionOptionRepository answerMcSubmissionOptionRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private QuestionSubmissionRepository questionSubmissionRepository;
    @Autowired private SubmissionCommentRepository submissionCommentRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private IntegrationLaunchService integrationLaunchService;
    @Autowired private QuestionSubmissionService questionSubmissionService;
    @Autowired private SubmissionCommentService submissionCommentService;
    @Autowired private AssessmentSubmissionService assessmentSubmissionService;
    @Autowired private AdvantageAgsService advantageAgsService;
    @Autowired private CaliperService caliperService;
    @Autowired private ApiJwtService apiJwtService;
    @Autowired private IntegrationTokenService integrationTokenService;
    @Autowired private ApiClient apiClient;

    @Override
    public List<SubmissionDto> getSubmissions(Long experimentId, String userId, Long assessmentId, boolean student) throws NoSubmissionsException {
        //for instructor
        if (!student) {
            List<Submission> submissions = submissionRepository.findByAssessment_AssessmentId(assessmentId).stream()
                .filter(submission -> !submission.getParticipant().isTestStudent())
                .toList();
            List<SubmissionDto> submissionDtoList = new ArrayList<>();

            for (Submission submission : submissions) {
                submissionDtoList.add(toDto(submission, false, false));
            }

            return submissionDtoList;
        }

        //for student
        Participant participant = findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);
        List<Submission> submissions = submissionRepository.findByParticipant_Id(participant.getParticipantId());

        if (submissions.isEmpty()) {
            throw new NoSubmissionsException("There are no existing submissions for current user.");
        }

        List<SubmissionDto> submissionDtoList = new ArrayList<>();

        for (Submission submission : submissions) {
            submissionDtoList.add(toDto(submission, false, false));
        }

        return submissionDtoList;
    }

    @Override
    public Submission getSubmission(Long experimentId, String userId, Long submissionId, boolean student) throws NoSubmissionsException {
        //for instructor
        if (!student) {
            return submissionRepository.findBySubmissionId(submissionId);
        }

        //for student
        Participant participant = findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);
        Optional<Submission> submission = submissionRepository.findByParticipant_IdAndSubmissionId(participant.getParticipantId(), submissionId);

        if (submission.isEmpty()) {
            throw new NoSubmissionsException("A submission for participant " + participant.getParticipantId() + "  with id " + submissionId + " not found");
        }

        return submission.get();
    }

    @Override
    public SubmissionDto postSubmission(SubmissionDto submissionDto, long experimentId, SecuredInfo securedInfo, long assessmentId, boolean student)
            throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException {
        if (submissionDto.getSubmissionId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        submissionDto.setAssessmentId(assessmentId);
        validateDto(experimentId, securedInfo.getUserId(), submissionDto);
        Submission submission;

        try {
            submission = fromDto(submissionDto, student);
        } catch (DataServiceException ex) {
            throw new DataServiceException(String.format("Error 105: Unable to create Submission: %s", ex.getMessage()), ex);
        }

        submission = save(submission);

        integrationTokenService.create(submission, securedInfo);
        createIntegrationLaunchUrl(submission);
        setAssignmentStart(submission.getAssessment().getTreatment().getAssignment(), securedInfo);

        return toDto(submission, false, false);
    }

    @Override
    @Transactional
    public void updateSubmissions(Map<Submission, SubmissionDto> map, boolean student) throws ConnectionException, DataServiceException, ApiException, IOException, TerracottaConnectorException {
        if (student) {
            // students cannot update submission scores
            return;
        }

        if (MapUtils.isEmpty(map)) {
            // no submissions to process
            return;
        }

        for (Map.Entry<Submission, SubmissionDto> entry : map.entrySet()) {
            Submission submission = entry.getKey();
            SubmissionDto submissionDto = entry.getValue();
            submission.setAlteredCalculatedGrade(submissionDto.getAlteredCalculatedGrade());
            submission.setTotalAlteredGrade(submissionDto.getTotalAlteredGrade());
            submission.setGradeOverridden(submissionDto.isGradeOverridden());
            submission = save(submission);
        }

        // only submit to LMS once after all submissions have been saved
        Submission submissionToSend = map.keySet().iterator().next();
        log.info("Sending updated grade to LMS for assessment ID: [{}] and participant ID: [{}]", submissionToSend.getAssessment().getAssessmentId(), submissionToSend.getParticipant().getParticipantId());
        sendSubmissionGradeToLmsWithLti(submissionToSend, student);
    }

    @Override
    public SubmissionDto toDto(Submission submission, boolean questionSubmissions, boolean submissionComments) {
        SubmissionDto submissionDto = SubmissionDto.builder().build();
        submissionDto.setSubmissionId(submission.getSubmissionId());
        submissionDto.setParticipantId(submission.getParticipant().getParticipantId());
        submissionDto.setAssessmentId(submission.getAssessment().getAssessmentId());
        submissionDto.setConditionId(submission.getAssessment().getTreatment().getCondition().getConditionId());
        submissionDto.setTreatmentId(submission.getAssessment().getTreatment().getTreatmentId());
        submissionDto.setExperimentId(submission.getAssessment().getTreatment().getCondition().getExperiment().getExperimentId());
        submissionDto.setCalculatedGrade(submission.getCalculatedGrade());
        submissionDto.setAlteredCalculatedGrade(submission.getAlteredCalculatedGrade());
        submissionDto.setTotalAlteredGrade(submission.getTotalAlteredGrade());
        submissionDto.setDateSubmitted(submission.getDateSubmitted());
        submissionDto.setLateSubmission(submission.isLateSubmission());
        submissionDto.setDateCreated(submission.getCreatedAt());
        submissionDto.setQuestionSubmissionDtoList(Collections.emptyList());
        submissionDto.setSubmissionCommentDtoList(Collections.emptyList());
        submissionDto.setGradeOverridden(submission.isGradeOverridden());
        submissionDto.setIntegrationFeedbackEnabled(false);

        // build launch URL for any integration-type submissions
        if (submission.isIntegration()) {
            integrationLaunchService.buildUrl(submission, 0, submission.getIntegration());
            submissionDto.setIntegrationLaunchUrl(submission.getIntegrationLaunchUrl());
            submissionDto.setIntegrationFeedbackEnabled(submission.isIntegrationFeedbackEnabled());
        }

        if (questionSubmissions) {
            List<QuestionSubmission> questionSubmissionList = questionSubmissionRepository
                    .findBySubmission_SubmissionId(submission.getSubmissionId());
            // If submission has not been submitted for grading yet, also return the answer
            // submissions (so the frontend can tell whether it needs to create or update
            // answer submissions)
            boolean hasSubmitted = submission.getDateSubmitted() != null;
            submissionDto.setQuestionSubmissionDtoList(
                questionSubmissionList.stream()
                    .map(
                        questionSubmission -> {
                            try {
                                return questionSubmissionService.toDto(questionSubmission, !hasSubmitted, false);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                    .toList()
            );
        }

        if (submissionComments) {
            submissionDto.setSubmissionCommentDtoList(
                CollectionUtils.emptyIfNull(submissionCommentRepository.findBySubmission_SubmissionId(submission.getSubmissionId())).stream()
                    .map(submissionComment -> submissionCommentService.toDto(submissionComment))
                    .toList()
            );
        }

        StringBuilder path = new StringBuilder(submission.getParticipant().getLtiUserEntity().getPlatformDeployment().getLocalUrl())
            .append("/api/experiments/")
            .append(submission.getAssessment().getTreatment().getCondition().getExperiment().getExperimentId())
            .append("/conditions/")
            .append(submission.getAssessment().getTreatment().getCondition().getConditionId())
            .append("/treatments/")
            .append(submission.getAssessment().getTreatment().getTreatmentId())
            .append("/assessments/")
            .append(submission.getAssessment().getAssessmentId());
        submissionDto.setAssessmentLink(path.toString());

        return submissionDto;
    }

    @Override
    public Submission fromDto(SubmissionDto submissionDto, boolean student) throws DataServiceException {
        Submission submission = new Submission();
        submission.setSubmissionId(submissionDto.getSubmissionId());

        if (!student) {  //Students can't post a submissions and change the grades.
            submission.setCalculatedGrade(submissionDto.getCalculatedGrade());
            submission.setAlteredCalculatedGrade(submissionDto.getAlteredCalculatedGrade());
            submission.setTotalAlteredGrade(submissionDto.getTotalAlteredGrade());
            submission.setGradeOverridden(submissionDto.isGradeOverridden());
        }

        submission.setDateSubmitted(submissionDto.getDateSubmitted());
        submission.setLateSubmission(submissionDto.isLateSubmission());
        Optional<Participant> participant = participantRepository.findById(submissionDto.getParticipantId());

        if (participant.isEmpty()) {
            throw new DataServiceException("The participant for the submission does not exist.");
        }

        submission.setParticipant(participant.get());

        Optional<Assessment> assessment = assessmentRepository.findById(submissionDto.getAssessmentId());

        if (assessment.isEmpty()) {
            throw new DataServiceException("The assessment for the submission does not exist.");
        }

        submission.setAssessment(assessment.get());

        return submission;
    }

    private Submission save(Submission submission) {
        return submissionRepository.save(submission);
    }

    private Participant findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(Long experimentId, String userId) {
        return participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);
    }

    private void saveAndFlush(Submission submissionToChange) {
        submissionRepository.saveAndFlush(submissionToChange);
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        submissionRepository.deleteBySubmissionId(id);
    }

    @Override
    public void finalizeAndGrade(Long submissionId, SecuredInfo securedInfo, boolean student)
            throws DataServiceException, AssignmentDatesException, IOException, ConnectionException, ApiException, TerracottaConnectorException {
        finalizeAndGrade(submissionId, securedInfo, student, RegradeOption.NA);
    }

    @Transactional
    public void finalizeAndGrade(Long submissionId, SecuredInfo securedInfo, boolean student, RegradeOption regradeOption)
            throws DataServiceException, AssignmentDatesException, IOException, ConnectionException, ApiException, TerracottaConnectorException {
        Optional<Submission> submission = submissionRepository.findById(submissionId);

        if (submission.isEmpty()) {
            throw new DataServiceException("Error 105: Submission not found");
        }

        //We are not changing the submission date once it is set.
        //^^^ maybe if we are allowing resubmissions we should allow this to change?
        if (submission.get().getDateSubmitted() == null) {
            if (securedInfo.getDueAt() != null && submission.get().getUpdatedAt().after(securedInfo.getDueAt())) {
                submission.get().setLateSubmission(true);
            }

            submission.get().setDateSubmitted(getLastUpdatedTimeForSubmission(submission.get()));
        }

        if (securedInfo.getLockAt() == null || submission.get().getDateSubmitted().after(securedInfo.getLockAt())) {
            saveAndFlush(assessmentSubmissionService.gradeSubmission(submission.get(), new RegradeDetails()));
            caliperService.sendAssignmentSubmitted(submission.get(), securedInfo);
            sendSubmissionGradeToLmsWithLti(submission.get(), student);
        } else {
            throw new AssignmentDatesException("Error 128: LMS Assignment is locked, we can not generate/grade a submission with a date later than the lock date");
        }
    }

    @Override
    public boolean datesAllowed(Long experimentId, Long treatmentId, SecuredInfo securedInfo) {
        if (securedInfo.getUnlockAt() == null || securedInfo.getUnlockAt().before(new Date())) {
            return securedInfo.getLockAt() == null || securedInfo.getLockAt().after(new Date());
        }

        return false;
    }

    @Override
    public Submission createNewSubmission(Assessment assessment, Participant participant, SecuredInfo securedInfo) throws IntegrationTokenNotFoundException {
        Submission submission = new Submission();
        submission.setAssessment(assessment);
        submission.setParticipant(participant);
        final Submission newSubmission = save(submission);

        // for each randomized MC question, create a QuestionSubmission and randomized list of AnswerMcSubmissionOptions
        assessment.getQuestions().stream()
            .filter(
                question -> {
                    return question.getQuestionType() == QuestionTypes.MC && ((QuestionMc) question).isRandomizeAnswers();
                })
            .forEach(
                question -> {
                    QuestionSubmission questionSubmission = new QuestionSubmission();
                    questionSubmission.setQuestion(question);
                    questionSubmission.setSubmission(newSubmission);
                    final QuestionSubmission newQuestionSubmission = questionSubmissionRepository.save(questionSubmission);
                    List<AnswerMc> answers = answerMcRepository.findByQuestion_QuestionId(question.getQuestionId());
                    Collections.shuffle(answers);
                    AtomicInteger order = new AtomicInteger(0);

                    answers.forEach(
                        answerMc -> {
                            AnswerMcSubmissionOption answerMcSubmissionOption = new AnswerMcSubmissionOption();
                            answerMcSubmissionOption.setAnswerMc(answerMc);
                            answerMcSubmissionOption.setAnswerOrder(order.getAndIncrement());
                            answerMcSubmissionOption.setQuestionSubmission(newQuestionSubmission);
                            answerMcSubmissionOptionRepository.save(answerMcSubmissionOption);
                        });
                });

        setAssignmentStart(assessment.getTreatment().getAssignment(), securedInfo);
        integrationTokenService.create(newSubmission, securedInfo);
        createIntegrationLaunchUrl(newSubmission);

        return newSubmission;
    }

    @Override
    @Transactional
    public void grade(Long submissionId, SecuredInfo securedInfo) throws DataServiceException {
        Optional<Submission> submission = submissionRepository.findById(submissionId);

        if (submission.isEmpty()) {
            throw new DataServiceException("Error 105: Submission not found");
        }

        saveAndFlush(assessmentSubmissionService.gradeSubmission(submission.get(), new RegradeDetails()));
    }

    @Override
    public void sendSubmissionGradeToLmsWithLti(Submission submission, boolean studentSubmission) throws ConnectionException, DataServiceException, ApiException, IOException, TerracottaConnectorException {
        //We need, the assignment, and the iss configuration...
        Assessment assessment = submission.getAssessment();
        Assignment assignment = assessment.getTreatment().getAssignment();
        Experiment experiment = assignment.getExposure().getExperiment();
        LtiToken ltiTokenScore = advantageAgsService.getToken("scores", experiment.getPlatformDeployment());
        LtiToken ltiTokenResults = advantageAgsService.getToken("results", experiment.getPlatformDeployment());
        //find the right id to pass based on the assignment
        String lineitemId = lineItemId(assignment);

        if (lineitemId == null) {
            throw new DataServiceException("Error 136: The assignment is not linked to any LMS assignment");
        }

        Score score = new Score();
        Participant participant = submission.getParticipant();
        score.setUserId(participant.getLtiUserEntity().getUserKey());
        Float scoreGiven = getScoreFromMultipleSubmissions(participant, assessment);

        if (scoreGiven != null) {
            score.setScoreGiven(scoreGiven);
        }

        Float maxTerracottaScore = assessmentSubmissionService.calculateMaxScore(assessment);

        if (maxTerracottaScore == 0) {
            // zero point assignments full credit (1 point) is given for completion so the
            // maximum is 1 point
            score.setScoreMaximum(1F);
        } else {
            score.setScoreMaximum(maxTerracottaScore);
        }

        score.setActivityProgress("Completed");

        if (isManualGradingNeeded(submission)) {
            score.setGradingProgress("PendingManual");
        } else {
            score.setGradingProgress("FullyGraded");
        }

        Date date = new Date();
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String strDate = dt.format(date);
        score.setTimestamp(strDate);

        apiClient.addLmsExtensions(score, submission, studentSubmission);
        advantageAgsService.postScore(ltiTokenScore, ltiTokenResults, experiment.getLtiContextEntity(), lineitemId, score);
    }

    @Override
    public boolean isManualGradingNeeded(Submission submission) {
        // If the submission's grade has been altered, then the entire submission has been manually graded.
        // If any of the ESSAY/FILE questions with positive max points have a null alteredGrade, then the assessment still needs to be manually graded.
        return !assessmentSubmissionService.isGradeAltered(submission)
            && submission.getQuestionSubmissions().stream()
                .anyMatch(
                    qs -> {
                        return (qs.getQuestion().getQuestionType() == QuestionTypes.ESSAY || qs.getQuestion().getQuestionType() == QuestionTypes.FILE)
                        && qs.getQuestion().getPoints() > 0
                        && qs.getAlteredGrade() == null;
                    }
                );
    }

    /**
     * Calculate a score, possibly considering multiple submissions.
     *
     * @param participant
     * @param assessment
     * @return null if all submissions require manual grading
     */
    public Float getScoreFromMultipleSubmissions(Participant participant, Assessment assessment) {
        List<Submission> submissionList = submissionRepository.findByParticipant_IdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(
                participant.getParticipantId(), assessment.getAssessmentId());

        // Handle case where only one submission is allowed
        if (assessment.getNumOfSubmissions() != null && assessment.getNumOfSubmissions() == 1) {
            Submission soleSubmission = submissionList.get(0);

            if (!isManualGradingNeeded(soleSubmission)) {
                return getSubmissionScore(soleSubmission);
            }

            return null;
        }

        // Only submissions that are fully graded will be considered for calculating
        // score
        Float score = null;

        switch (assessment.getMultipleSubmissionScoringScheme()) {
            case MOST_RECENT:
                // consider the most recently fully graded submission, if there is one
                for (int i = submissionList.size() - 1; i >= 0; i--) {
                    Submission submission = submissionList.get(i);

                    if (!isManualGradingNeeded(submission)) {
                        score = getSubmissionScore(submission);
                        break;
                    }
                }
                break;
            case AVERAGE:
                // average all fully graded submissions
                int count = 0;

                for (Submission submission : submissionList) {
                    if (!isManualGradingNeeded(submission)) {
                        if (score == null) {
                            score = 0f;
                        }

                        score += getSubmissionScore(submission);
                        count++;
                    }
                }

                if (score != null) {
                    score = score / count;
                }

                break;
            case HIGHEST:
                // take the highest of the fully graded submissions
                for (Submission submission : submissionList) {
                    if (!isManualGradingNeeded(submission)) {
                        Float submissionScore = getSubmissionScore(submission);

                        if (score == null || submissionScore > score) {
                            score = submissionScore;
                        }
                    }
                }
                break;
            case CUMULATIVE:
                // only include fully graded submissions, but consider them in order

                // The first submission's score contributes
                // 'cumulativeScoringInitialPercentage' to the total score
                if (CollectionUtils.isNotEmpty(submissionList)) {
                    Submission submission = submissionList.get(0);

                    if (!isManualGradingNeeded(submission)) {
                        score = getSubmissionScore(submission) * assessment.getCumulativeScoringInitialPercentage() / 100f;
                    }
                }
                // All subsequent submission scores contribute an evenly distributed amount of
                // the remaining percentage
                if (submissionList.size() > 1) {
                    float subsequentPercentage = (100f - assessment.getCumulativeScoringInitialPercentage()) / 100f / (assessment.getNumOfSubmissions() - 1);

                    for (Submission submission : submissionList.subList(1, submissionList.size())) {
                        if (!isManualGradingNeeded(submission)) {
                            if (score == null) {
                                score = 0f;
                            }

                            score = score + getSubmissionScore(submission) * subsequentPercentage;
                        }
                    }
                }
                break;
            default:
                break;
        }

        return score;
    }

    @Override
    public Float getSubmissionScore(Submission submission) {
        Assessment assessment = submission.getAssessment();
        Float maxTerracottaScore = assessmentSubmissionService.calculateMaxScore(assessment);

        // zero point assignments should be given full credit for completion
        if (maxTerracottaScore == 0) {
            return 1f;
        }

        if (submission.isGradeOverridden()) {
            return submission.getTotalAlteredGrade();
        }

        return submission.getAlteredCalculatedGrade();
    }

    private Timestamp getLastUpdatedTimeForSubmission(Submission submission) {
        Timestamp lastTimestamp = submission.getUpdatedAt();

        for (QuestionSubmission questionSubmission : submission.getQuestionSubmissions()) {
            if (questionSubmission.getUpdatedAt().after(lastTimestamp)) {
                lastTimestamp = questionSubmission.getUpdatedAt();
            }
        }

        if (lastTimestamp.equals(submission.getCreatedAt())) {
            // We need to do this because the caliper event won't allow a submission time equals to the creation time,
            // so we add 1 ms. This is not a very elegant solution, but it is needed.
            lastTimestamp = new Timestamp(lastTimestamp.getTime() + 1);
        }

        return lastTimestamp;
    }

    @Override
    public void validateDto(Long experimentId, String userId, SubmissionDto submissionDto) throws InvalidUserException, ParticipantNotMatchingException {
        Participant participant = findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);

        if (participant == null) {
            throw new ParticipantNotMatchingException(TextConstants.PARTICIPANT_NOT_MATCHING);
        }

        submissionDto.setParticipantId(participant.getParticipantId());

        if (submissionDto.getAlteredCalculatedGrade() != null || submissionDto.getTotalAlteredGrade() != null) {
            throw new InvalidUserException(TextConstants.NOT_ENOUGH_PERMISSIONS + " Students cannot alter the grades.");
        }
    }

    @Override
    public void validateUser(Long experimentId, String userId, Long submissionId) throws InvalidUserException {
        Participant participant = participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);
        Optional<Submission> submission = submissionRepository.findByParticipant_IdAndSubmissionId(participant.getParticipantId(), submissionId);

        if (submission.isEmpty()) {
            throw new InvalidUserException("Error 121: Students can only access answer submissions from their own submissions. Submission with id "
                    + submissionId + " does not belong to participant with id " + participant.getParticipantId());
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId, long assessmentId, long submissionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId).toUri());

        return headers;
    }

    @Override
    public void allowedSubmission(Long submissionId, SecuredInfo securedInfo) throws SubmissionNotMatchingException {
        try {
            Optional<Submission> submission = submissionRepository.findById(submissionId);

            if (submission.isPresent()) {
                if (!submission.get().getParticipant().getLtiUserEntity().getUserKey().equals(securedInfo.getUserId())) {
                    throw new SubmissionNotMatchingException("Submission don't belong to the user");
                }

                boolean consent = BooleanUtils.isTrue(submission.get().getParticipant().getConsent());
                // Group is only used for determining treatment when participant has consented. Students that haven't given consent should always get the default condition's treatment.
                Group group = consent ? submission.get().getParticipant().getGroup() : null;
                Treatment treatment = submission.get().getAssessment().getTreatment();
                Condition condition = treatment.getCondition();

                if (group == null) {
                    if (condition.getDefaultCondition()) {
                        return;
                    }

                    throw new SubmissionNotMatchingException("Student not in a group, but not sending the default condition");
                } else {
                    Optional<ExposureGroupCondition> exposureGroupCondition = exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(condition.getConditionId(), treatment.getAssignment().getExposure().getExposureId());

                    if (exposureGroupCondition.isPresent() && group == exposureGroupCondition.get().getGroup()) {
                        return;
                    }

                    List<Treatment> treatments = treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(submission.get().getAssessment().getTreatment().getAssignment().getAssignmentId());

                    if (treatments.size() == 1) {
                        // this is a single treatment assignment; allow submission from other exposure groups
                        return;
                    }

                    throw new SubmissionNotMatchingException("Student sending an assessment that does not belong to the expected treatment");
                }
            }
        } catch (Exception e) {
            throw new SubmissionNotMatchingException(String.format("Error 147: Not allowed to submit this submission: %s", e.getMessage()));
        }

        throw new SubmissionNotMatchingException("Error 147: Not allowed to submit this submission");
    }

    /**
     * If this is the first submission mark the assignment as started.
     *
     * @param assignment the {@link Assignment}
     * @param securedInfo the {@link SecuredInfo}
     */
    private void setAssignmentStart(Assignment assignment, SecuredInfo securedInfo) {
        if (assignment.isStarted()) {
            // already started
            return;
        }

        if (apiJwtService.isTestStudent(securedInfo)) {
            // is a test student
            return;
        }

        assignment.setStarted(Timestamp.valueOf(LocalDateTime.now()));
        assignmentRepository.save(assignment);
    }

    private String lineItemId(Assignment assignment) throws ConnectionException, TerracottaConnectorException {
        Experiment experiment = assignment.getExposure().getExperiment();
        LtiToken ltiToken = advantageAgsService.getToken("lineitems", experiment.getPlatformDeployment());
        //find the right id to pass based on the assignment
        LineItems lineItems = advantageAgsService.getLineItems(ltiToken, experiment.getLtiContextEntity());

        for (LineItem lineItem : lineItems.getLineItemList()) {
            if (lineItem.getResourceLinkId().equals(assignment.getResourceLinkId())) {
                return lineItem.getId();
            }
        }

        return null;
    }

    private void createIntegrationLaunchUrl(Submission submission) {
        if (!submission.isIntegration()) {
            return;
        }

        // assessment is an integration; create URL for launch
        int submissionsCount = 0;

        try {
            submissionsCount = getSubmissions(
                submission.getParticipant().getExperiment().getExperimentId(),
                submission.getParticipant().getLtiUserEntity().getUserKey(),
                submission.getAssessment().getAssessmentId(),
                true
            )
            .size();
        } catch (NoSubmissionsException e) {
            log.info(
                "Error retrieving submissions count for assessment ID: [{}] and participant ID: [{}]",
                submission.getAssessment().getAssessmentId(),
                submission.getParticipant().getLtiUserEntity().getUserKey()
            );
        }

        integrationLaunchService.buildUrl(
            submission,
            submissionsCount,
            submission.getIntegration()
        );
    }

    @Override
    public Map<String, List<LmsSubmission>> getAllSubmissionsForMultipleAssignments(LtiUserEntity ltiUserEntity, String lmsCourseId, List<String> lmsAssignmentIds) throws ApiException, TerracottaConnectorException, IOException {
        Map<String, List<LmsSubmission>> submissionsMap = new HashMap<>();

        for (String lmsAssignmentId : lmsAssignmentIds) {
            List<LmsSubmission> submissions = apiClient.listSubmissions(ltiUserEntity, lmsAssignmentId, lmsCourseId);
            submissionsMap.put(lmsAssignmentId, submissions);
        }

        return submissionsMap;
    }

}
