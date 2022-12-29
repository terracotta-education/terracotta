package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.model.ags.Score;
import edu.iu.terracotta.model.app.*;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionDto;
import edu.iu.terracotta.model.app.dto.SubmissionCommentDto;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.oauth2.LTIToken;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.*;
import edu.iu.terracotta.service.caliper.CaliperService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.service.lti.AdvantageAGSService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.stream.Collectors;

@Component
public class SubmissionServiceImpl implements SubmissionService {

    @Value("${application.url}")
    private String localUrl;

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    QuestionSubmissionService questionSubmissionService;

    @Autowired
    SubmissionCommentService submissionCommentService;

    @Autowired
    AssignmentService assignmentService;

    @Autowired
    AssessmentService assessmentService;

    @Autowired
    AnswerSubmissionService answerSubmissionService;

    @Autowired
    AdvantageAGSService advantageAGSService;

    @Autowired
    CaliperService caliperService;

    @Autowired
    CanvasAPIClient canvasAPIClient;

    @Autowired
    private APIJWTService apijwtService;

    @Override
    public List<Submission> findAllByAssessmentId(Long assessmentId) {
        return allRepositories.submissionRepository.findByAssessment_AssessmentId(assessmentId);
    }

    @Override
    public List<SubmissionDto> getSubmissions(Long experimentId, String userId, Long assessmentId, boolean student) throws NoSubmissionsException {
        //for instructor
        if (!student) {
            List<Submission> submissions = findAllByAssessmentId(assessmentId);
            List<SubmissionDto> submissionDtoList = new ArrayList<>();
            for (Submission submission : submissions) {
                submissionDtoList.add(toDto(submission, false, false));
            }
            return submissionDtoList;
        }
        //for student
        Participant participant = findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);
        List<Submission> submissions = findByParticipantId(participant.getParticipantId());
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
            return allRepositories.submissionRepository.findBySubmissionId(submissionId);
        }
        //for student
        Participant participant = findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);
        Optional<Submission> submission = findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
        if (!submission.isPresent()) {
            throw new NoSubmissionsException("A submission for participant " + participant.getParticipantId() + "  with id " + submissionId + " not found");
        }
        return submission.get();
    }

    @Override
    public SubmissionDto postSubmission(SubmissionDto submissionDto, long experimentId, SecuredInfo securedInfo, long assessmentId, boolean student)
            throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException {
        if (submissionDto.getSubmissionId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        submissionDto.setAssessmentId(assessmentId);
        validateDto(experimentId, securedInfo.getUserId(), submissionDto);
        Submission submission;

        try {
            submission = fromDto(submissionDto, student);
        } catch (DataServiceException ex) {
            throw new DataServiceException(String.format("Error 105: Unable to create Submission: %s", ex.getMessage()));
        }

        setAssignmentStart(submission.getAssessment().getTreatment().getAssignment(), securedInfo);

        return toDto(save(submission), false, false);
    }

    @Override
    @Transactional
    public void updateSubmissions(Map<Submission, SubmissionDto> map, boolean student) throws ConnectionException, DataServiceException {
        if (!student) {
            for (Map.Entry<Submission, SubmissionDto> entry : map.entrySet()) {
                Submission submission = entry.getKey();
                SubmissionDto submissionDto = entry.getValue();
                submission.setAlteredCalculatedGrade(submissionDto.getAlteredCalculatedGrade());
                submission.setTotalAlteredGrade(submissionDto.getTotalAlteredGrade());
                save(submission);
            }
        }
    }

    @Override
    public SubmissionDto toDto(Submission submission, boolean questionSubmissions, boolean submissionComments) {
        SubmissionDto submissionDto = new SubmissionDto();
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

        if (questionSubmissions) {
            List<QuestionSubmission> questionSubmissionList = allRepositories.questionSubmissionRepository
                    .findBySubmission_SubmissionId(submission.getSubmissionId());
            // If submission has not been submitted for grading yet, also return the answer
            // submissions (so the frontend can tell whether it needs to create or update
            // answer submissions)
            boolean hasSubmitted = submission.getDateSubmitted() != null;
            List<QuestionSubmissionDto> questionSubmissionDtoList = questionSubmissionList.stream()
                    .map(questionSubmission -> questionSubmissionService.toDto(questionSubmission, !hasSubmitted,
                            false))
                .collect(Collectors.toList());
            submissionDto.setQuestionSubmissionDtoList(questionSubmissionDtoList);
        }

        if (submissionComments) {
            List<SubmissionComment> submissionCommentList = allRepositories.submissionCommentRepository.findBySubmission_SubmissionId(submission.getSubmissionId());
            List<SubmissionCommentDto> submissionCommentDtoList = submissionCommentList.stream()
                .map(submissionComment -> submissionCommentService.toDto(submissionComment))
                .collect(Collectors.toList());
            submissionDto.setSubmissionCommentDtoList(submissionCommentDtoList);
        }

        StringBuilder path = new StringBuilder(localUrl)
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
        }
        submission.setDateSubmitted(submissionDto.getDateSubmitted());
        submission.setLateSubmission(submissionDto.isLateSubmission());
        Optional<Participant> participant = allRepositories.participantRepository.findById(submissionDto.getParticipantId());
        if (participant.isPresent()) {
            submission.setParticipant(participant.get());
        } else {
            throw new DataServiceException("The participant for the submission does not exist.");
        }
        Optional<Assessment> assessment = allRepositories.assessmentRepository.findById(submissionDto.getAssessmentId());
        if (assessment.isPresent()) {
            submission.setAssessment(assessment.get());
        } else {
            throw new DataServiceException("The assessment for the submission does not exist.");
        }

        return submission;
    }

    @Override
    public Submission save(Submission submission) {
        return allRepositories.submissionRepository.save(submission);
    }

    @Override
    public Optional<Submission> findById(Long id) {
        return allRepositories.submissionRepository.findById(id);
    }

    @Override
    public List<Submission> findByParticipantId(Long participantId) {
        return allRepositories.submissionRepository.findByParticipant_ParticipantId(participantId);
    }

    @Override
    public Optional<Submission> findByParticipantIdAndSubmissionId(Long participantId, Long submissionId) {
        return allRepositories.submissionRepository.findByParticipant_ParticipantIdAndSubmissionId(participantId, submissionId);
    }

    @Override
    public List<Submission> findByParticipantIdAndAssessmentId(Long participantId, Long assessmentId) {
        return allRepositories.submissionRepository.findByParticipant_ParticipantIdAndAssessment_AssessmentId(participantId, assessmentId);
    }

    @Override
    public Participant findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(Long experimentId, String userId) {
        return allRepositories.participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);
    }

    @Override
    public void saveAndFlush(Submission submissionToChange) {
        allRepositories.submissionRepository.saveAndFlush(submissionToChange);
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        allRepositories.submissionRepository.deleteBySubmissionId(id);
    }

    @Override
    public boolean submissionBelongsToAssessment(Long assessmentId, Long submissionId) {
        return allRepositories.submissionRepository.existsByAssessment_AssessmentIdAndSubmissionId(assessmentId, submissionId);
    }

    @Override
    @Transactional
    public void finalizeAndGrade(Long submissionId, SecuredInfo securedInfo, boolean student)
            throws DataServiceException, AssignmentDatesException, CanvasApiException, IOException,
            ConnectionException {
        Optional<Submission> submissionOptional = allRepositories.submissionRepository.findById(submissionId);
        if (submissionOptional.isPresent()) {
            //We are not changing the submission date once it is set.
            //^^^ maybe if we are allowing resubmissions we should allow this to change?
            if (submissionOptional.get().getDateSubmitted() == null) {
                if (securedInfo.getDueAt() != null && submissionOptional.get().getUpdatedAt().after(securedInfo.getDueAt())) {
                    submissionOptional.get().setLateSubmission(true);
                }
                submissionOptional.get().setDateSubmitted(getLastUpdatedTimeForSubmission(submissionOptional.get()));
            }
            if (securedInfo.getLockAt() == null || submissionOptional.get().getDateSubmitted().after(securedInfo.getLockAt())) {
                saveAndFlush(gradeSubmission(submissionOptional.get()));
                caliperService.sendAssignmentSubmitted(submissionOptional.get(), securedInfo);
                sendSubmissionGradeToCanvasWithLTI(submissionOptional.get(), student);
            } else {
                throw new AssignmentDatesException("Error 128: Canvas Assignment is locked, we can not generate/grade a submission with a date later than the lock date");
            }
        } else {
            throw new DataServiceException("Error 105: Submission not found");
        }
    }

    @Override
    public boolean datesAllowed(Long experimentId, Long treatmentId, SecuredInfo securedInfo) {

        if (securedInfo.getUnlockAt() == null || securedInfo.getUnlockAt().before(new Date())) {
            return securedInfo.getLockAt() == null || securedInfo.getLockAt().after(new Date());
        } else {
            return false;
        }
    }

    @Override
    public Submission createNewSubmission(Assessment assessment, Participant participant, SecuredInfo securedInfo) {
        Submission submission = new Submission();
        submission.setAssessment(assessment);
        submission.setParticipant(participant);
        final Submission newSubmission = save(submission);

        // for each randomized MC question, create a QuestionSubmission and randomized list of AnswerMcSubmissionOptions
        assessment.getQuestions().stream()
            .filter(
                question -> {
                    if (question.getQuestionType() != QuestionTypes.MC) {
                        return false;
                    }

                    return ((QuestionMc) question).isRandomizeAnswers();
                })
            .forEach(
                question -> {
                    QuestionSubmission questionSubmission = new QuestionSubmission();
                    questionSubmission.setQuestion(question);
                    questionSubmission.setSubmission(newSubmission);
                    final QuestionSubmission newQuestionSubmission = allRepositories.questionSubmissionRepository.save(questionSubmission);
                    List<AnswerMc> answers = allRepositories.answerMcRepository.findByQuestion_QuestionId(question.getQuestionId());
                    Collections.shuffle(answers);
                    AtomicInteger order = new AtomicInteger(0);

                    answers.forEach(
                        answerMc -> {
                            AnswerMcSubmissionOption answerMcSubmissionOption = new AnswerMcSubmissionOption();
                            answerMcSubmissionOption.setAnswerMc(answerMc);
                            answerMcSubmissionOption.setAnswerOrder(order.getAndIncrement());
                            answerMcSubmissionOption.setQuestionSubmission(newQuestionSubmission);
                            allRepositories.answerMcSubmissionOptionRepository.save(answerMcSubmissionOption);
                        });
                });

        setAssignmentStart(assessment.getTreatment().getAssignment(), securedInfo);

        return newSubmission;
    }

    @Override
    @Transactional
    public void grade(Long submissionId, SecuredInfo securedInfo) throws DataServiceException {
        Optional<Submission> submissionOptional = allRepositories.submissionRepository.findById(submissionId);
        if (submissionOptional.isPresent()) {
            saveAndFlush(gradeSubmission(submissionOptional.get()));
        } else {
            throw new DataServiceException("Error 105: Submission not found");
        }
    }


    @Override
    public Submission gradeSubmission(Submission submission) throws DataServiceException {
        //We need to calculate the 2 the possible grades. Automatic and manual
        float automatic = Float.parseFloat("0");
        float manual = Float.parseFloat("0");
        boolean altered = this.isGradeAltered(submission);
        for (QuestionSubmission questionSubmission : submission.getQuestionSubmissions()) {
            //We need to grade the question first automatically it it was not graded before.
            //If multiple choice, we take the automatic score for automatic and the manual if any for manual, and if no manual, then the automatic for manual
            QuestionSubmission questionGraded = new QuestionSubmission();
            switch (questionSubmission.getQuestion().getQuestionType()) {
                case MC:
                    List<AnswerMcSubmission> answerMcSubmissions = answerSubmissionService.findByQuestionSubmissionIdMC(questionSubmission.getQuestionSubmissionId());
                    if (answerMcSubmissions.size() == 1) {
                        questionGraded = questionSubmissionService.automaticGradingMC(questionSubmission, answerMcSubmissions.get(0));
                    } else if (answerMcSubmissions.size() > 1) {
                        throw new DataServiceException("Error 135: Cannot have more than one answer submission for a multiple choice question.");
                    } else {
                        questionGraded.setCalculatedPoints(Float.valueOf("0"));
                    }
                    break;
                case ESSAY:
                    questionGraded = questionSubmission;
                    questionGraded.setCalculatedPoints(Float.valueOf("0"));
                    break;
                default:
                    break;

            }
            automatic = automatic + questionGraded.getCalculatedPoints();
            if (questionGraded.getAlteredGrade() != null && !questionGraded.getAlteredGrade().isNaN()) {
                manual = manual + questionSubmission.getAlteredGrade();
            } else {
                manual = manual + questionSubmission.getCalculatedPoints();
            }
            //TODO: If open question, we take the manual score for both, because the automatic will be always 0
        }
        submission.setCalculatedGrade(automatic);
        submission.setAlteredCalculatedGrade(manual);
        if (!altered) {
            submission.setTotalAlteredGrade(manual);
        }
        return submission;
    }

    @Override
    public void sendSubmissionGradeToCanvasWithLTI(Submission submission, boolean studentSubmission)
            throws ConnectionException, DataServiceException {
        //We need, the assignment, and the iss configuration...
        Assessment assessment = submission.getAssessment();
        Assignment assignment = assessment.getTreatment().getAssignment();
        Experiment experiment = assignment.getExposure().getExperiment();
        LTIToken ltiTokenScore = advantageAGSService.getToken("scores", experiment.getPlatformDeployment());
        LTIToken ltiTokenResults = advantageAGSService.getToken("results", experiment.getPlatformDeployment());
        //find the right id to pass based on the assignment
        String lineitemId = assignmentService.lineItemId(assignment);

        if (lineitemId == null) {
            throw new DataServiceException("Error 136: The assignment is not linked to any Canvas assignment");
        }
        Score score = new Score();
        Participant participant = submission.getParticipant();
        score.setUserId(participant.getLtiUserEntity().getUserKey());
        boolean manualGradingNeeded = this.isManualGradingNeeded(submission);

        Float scoreGiven = getScoreFromMultipleSubmissions(participant, assessment);
        if (scoreGiven != null) {
            score.setScoreGiven(scoreGiven.toString());
        }
        Float maxTerracottaScore = assessmentService.calculateMaxScore(assessment);
        if (maxTerracottaScore == 0) {
            // zero point assignments full credit (1 point) is given for completion so the
            // maximum is 1 point
            score.setScoreMaximum(Float.valueOf(1).toString());
        } else {
            score.setScoreMaximum(maxTerracottaScore.toString());
        }
        score.setActivityProgress("Completed");
        if (manualGradingNeeded) {
            score.setGradingProgress("PendingManual");
        } else {
            score.setGradingProgress("FullyGraded");
        }

        Date date = new Date();
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String strDate = dt.format(date);
        score.setTimestamp(strDate);

        addCanvasExtensions(score, submission, studentSubmission);
        advantageAGSService.postScore(ltiTokenScore, ltiTokenResults, experiment.getLtiContextEntity(), lineitemId, score);
    }

    private void addCanvasExtensions(Score score, Submission submission, boolean studentSubmission) {
        Map<String, Object> submissionData = new HashMap<>();
        // See
        // https://canvas.instructure.com/doc/api/score.html#method.lti/ims/scores.create
        // for more information about these extension fields

        // Only treat a score as a new submission when it comes from a student and NOT
        // when graded by an instructor
        submissionData.put("new_submission", studentSubmission);

        // Include date originally submitted so that late grading doesn't result in late
        // submissions
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // ISO8601 format
        String dateSubmittedFormatted = dt.format(submission.getDateSubmitted());
        submissionData.put("submitted_at", dateSubmittedFormatted);

        score.setCanvasSubmissionExtension(submissionData);
    }

    private boolean isManualGradingNeeded(Submission submission) {

        // If the submission's grade has been altered, then the entire
        // submission has been manually graded.
        // If any of the ESSAY questions with positive max points have a null
        // alteredGrade, then the assessment still needs to be manually graded.
        return !this.isGradeAltered(submission)
                && submission.getQuestionSubmissions().stream().anyMatch(qs -> {
                    return qs.getQuestion().getQuestionType() == QuestionTypes.ESSAY
                            && qs.getQuestion().getPoints() > 0
                            && qs.getAlteredGrade() == null;
                });
    }

    /**
     * Calculate a score, possibly considering multiple submissions.
     *
     * @param participant
     * @param assessment
     * @return null if all submissions require manual grading
     */
    public Float getScoreFromMultipleSubmissions(Participant participant, Assessment assessment) {
        List<Submission> submissionList = allRepositories.submissionRepository
                .findByParticipant_ParticipantIdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(
                        participant.getParticipantId(), assessment.getAssessmentId());

        // Handle case where only one submission is allowed
        if (assessment.getNumOfSubmissions() != null && assessment.getNumOfSubmissions() == 1) {
            Submission soleSubmission = submissionList.get(0);
            if (!isManualGradingNeeded(soleSubmission)) {
                return getSubmissionScore(soleSubmission);
            } else {
                return null;
            }
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
                if (submissionList.size() > 0) {
                    Submission submission = submissionList.get(0);
                    if (!isManualGradingNeeded(submission)) {
                        score = getSubmissionScore(submission)
                                * assessment.getCumulativeScoringInitialPercentage() / 100f;
                    }
                }
                // All subsequent submission scores contribute an evenly distributed amount of
                // the remaining percentage
                if (submissionList.size() > 1) {
                    float subsequentPercentage = (100f - assessment.getCumulativeScoringInitialPercentage())
                            / 100f / (assessment.getNumOfSubmissions() - 1);
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
        }
        return score;
    }

    @Override
    public Float getSubmissionScore(Submission submission) {

        Assessment assessment = submission.getAssessment();
        Float maxTerracottaScore = assessmentService.calculateMaxScore(assessment);
        // zero point assignments should be given full credit for completion
        if (maxTerracottaScore == 0) {
            return 1f;
        }
        if (submission.getTotalAlteredGrade() != null) {
            return submission.getTotalAlteredGrade();
        } else {
            return submission.getAlteredCalculatedGrade();
        }
    }

    private boolean isGradeAltered(Submission submission) {

        Float totalAlteredGrade = submission.getTotalAlteredGrade();
        return totalAlteredGrade != null && (!totalAlteredGrade.equals(submission.getAlteredCalculatedGrade()));
    }

    private Timestamp getLastUpdatedTimeForSubmission(Submission submission) {

        Timestamp lastTimestamp = submission.getUpdatedAt();
        for (QuestionSubmission questionSubmission : submission.getQuestionSubmissions()) {
            if (questionSubmission.getUpdatedAt().after(lastTimestamp)) {
                lastTimestamp = questionSubmission.getUpdatedAt();
            }
        }
        if (lastTimestamp.equals(submission.getCreatedAt())) {
            //We need to do this because the caliper event won't allow a submission time equals to the creation time,
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
        Participant participant = allRepositories.participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);
        Optional<Submission> submission = allRepositories.submissionRepository.findByParticipant_ParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
        if (!submission.isPresent()) {
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
            Optional<Submission> submissionOptional = allRepositories.submissionRepository.findById(submissionId);
            if (submissionOptional.isPresent()) {
                if (!submissionOptional.get().getParticipant().getLtiUserEntity().getUserKey().equals(securedInfo.getUserId())) {
                    throw new SubmissionNotMatchingException("Submission don't belong to the user");
                }
                boolean consent = BooleanUtils.isTrue(submissionOptional.get().getParticipant().getConsent());
                // Group is only used for determining treatment when participant
                // has consented. Students that haven't given consent should
                // always get the default condition's treatment.
                Group group = consent ? submissionOptional.get().getParticipant().getGroup() : null;
                Treatment treatment = submissionOptional.get().getAssessment().getTreatment();
                Condition condition = treatment.getCondition();
                if (group == null) {
                    if (condition.getDefaultCondition()) {
                        return;
                    } else {
                        throw new SubmissionNotMatchingException("Student not in a group, but not sending the default condition");
                    }
                } else {
                    Optional<ExposureGroupCondition> exposureGroupCondition = allRepositories.exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(condition.getConditionId(), treatment.getAssignment().getExposure().getExposureId());
                    if (exposureGroupCondition.isPresent() && group == exposureGroupCondition.get().getGroup()) {
                        return;
                    } else {
                        throw new SubmissionNotMatchingException("Student sending an assessment that does not belongs to the expected treatment");
                    }
                }
            }
        } catch (Exception ex) {
            throw new SubmissionNotMatchingException("Error 147: Not allowed to submit this submission: " + ex.getMessage());
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

        if (apijwtService.isTestStudent(securedInfo)) {
            // is a test student
            return;
        }

        assignment.setStarted(Timestamp.valueOf(LocalDateTime.now()));
        assignmentService.save(assignment);
    }

}
