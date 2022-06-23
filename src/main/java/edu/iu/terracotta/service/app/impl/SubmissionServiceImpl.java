package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.*;
import edu.iu.terracotta.model.ags.LineItem;
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
import java.util.*;

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
    public SubmissionDto postSubmission(SubmissionDto submissionDto, long experimentId, String userId, long assessmentId, boolean student) throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException {
        if (submissionDto.getSubmissionId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }
        submissionDto.setAssessmentId(assessmentId);
        validateDto(experimentId, userId, submissionDto);
        Submission submission;
        try {
            submission = fromDto(submissionDto, student);
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create Submission: " + ex.getMessage());
        }
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
//                sendSubmissionGradeToCanvasWithLTI(submission);
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
        submissionDto.setLateSubmission(submission.getLateSubmission());
        List<QuestionSubmissionDto> questionSubmissionDtoList = new ArrayList<>();
        if (questionSubmissions) {
            List<QuestionSubmission> questionSubmissionList = allRepositories.questionSubmissionRepository.findBySubmission_SubmissionId(submission.getSubmissionId());
            for (QuestionSubmission questionSubmission : questionSubmissionList) {
                questionSubmissionDtoList.add(questionSubmissionService.toDto(questionSubmission, false, false));
            }
        }
        submissionDto.setQuestionSubmissionDtoList(questionSubmissionDtoList);
        List<SubmissionCommentDto> submissionCommentDtoList = new ArrayList<>();
        if (submissionComments) {
            List<SubmissionComment> submissionCommentList = allRepositories.submissionCommentRepository.findBySubmission_SubmissionId(submission.getSubmissionId());
            for (SubmissionComment submissionComment : submissionCommentList) {
                submissionCommentDtoList.add(submissionCommentService.toDto(submissionComment));
            }
        }
        submissionDto.setSubmissionCommentDtoList(submissionCommentDtoList);
        String path = localUrl +
                "/api/experiments/" +
                submission.getAssessment().getTreatment().getCondition().getExperiment().getExperimentId() +
                "/conditions/" +
                submission.getAssessment().getTreatment().getCondition().getConditionId() +
                "/treatments/" +
                submission.getAssessment().getTreatment().getTreatmentId() +
                "/assessments/" +
                submission.getAssessment().getAssessmentId();
        submissionDto.setAssessmentLink(path);
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
        submission.setLateSubmission(submissionDto.getLateSubmission());
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
    public void finalizeAndGrade(Long submissionId, SecuredInfo securedInfo) throws DataServiceException, AssignmentDatesException, CanvasApiException, IOException, ConnectionException {
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
                sendSubmissionGradeToCanvasWithLTI(submissionOptional.get());
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
        submission = save(submission);

        // for each randomized MC question, create a QuestionSubmission and
        // randomized list of AnswerMcSubmissionOptions
        for (Question question : assessment.getQuestions()) {
            if (question.getQuestionType() == QuestionTypes.MC) {
                QuestionMc questionMc = (QuestionMc) question;
                if (questionMc.isRandomizeAnswers()) {
                    QuestionSubmission questionSubmission = new QuestionSubmission();
                    questionSubmission.setQuestion(question);
                    questionSubmission.setSubmission(submission);
                    questionSubmission = allRepositories.questionSubmissionRepository.save(questionSubmission);
                    List<AnswerMc> answers = allRepositories.answerMcRepository
                            .findByQuestion_QuestionId(question.getQuestionId());
                    Collections.shuffle(answers);
                    int order = 0;
                    for (AnswerMc answerMc : answers) {
                        AnswerMcSubmissionOption answerMcSubmissionOption = new AnswerMcSubmissionOption();
                        answerMcSubmissionOption.setAnswerMc(answerMc);
                        answerMcSubmissionOption.setAnswerOrder(order++);
                        answerMcSubmissionOption.setQuestionSubmission(questionSubmission);
                        allRepositories.answerMcSubmissionOptionRepository.save(answerMcSubmissionOption);
                    }
                }
            }
        }

        return submission;
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
    public void sendSubmissionGradeToCanvasWithLTI(Submission submission) throws ConnectionException, DataServiceException {
        //We need, the assignment, and the iss configuration...
        Assignment assignment = submission.getAssessment().getTreatment().getAssignment();
        Experiment experiment = assignment.getExposure().getExperiment();
        LTIToken ltiTokenScore = advantageAGSService.getToken("scores", experiment.getPlatformDeployment());
        LTIToken ltiTokenResults = advantageAGSService.getToken("results", experiment.getPlatformDeployment());
        //find the right id to pass based on the assignment
        String lineitemId = assignmentService.lineItemId(assignment);
        LTIToken ltiLineItemToken = advantageAGSService.getToken("lineItem",experiment.getPlatformDeployment());

        if (lineitemId == null) {
            throw new DataServiceException("Error 136: The assignment is not linked to any Canvas assignment");
        }
        String[] ids = lineitemId.split("/");
        LineItem lineItem = advantageAGSService.getLineItem(ltiLineItemToken,experiment.getLtiContextEntity(),ids[ids.length-1]);
        Score score = new Score();
        score.setUserId(submission.getParticipant().getLtiUserEntity().getUserKey());
        boolean manualGradingNeeded = this.isManualGradingNeeded(submission);
        // Only set the score if additional manual grading isn't needed, i.e.,
        // if the grade is complete
        if (!manualGradingNeeded) {
            if (submission.getTotalAlteredGrade() != null) {
                score.setScoreGiven(submission.getTotalAlteredGrade().toString());
            } else {
                score.setScoreGiven(submission.getAlteredCalculatedGrade().toString());
            }
        }
        Float maxTerracottaScore = assessmentService.calculateMaxScore(submission.getAssessment());
        score.setScoreMaximum(maxTerracottaScore.toString());
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
        advantageAGSService.postScore(ltiTokenScore, ltiTokenResults, experiment.getLtiContextEntity(), lineitemId, score);
    }

    private boolean isManualGradingNeeded(Submission submission) {

        // If the submission's grade has been altered, then the entire
        // submission has been manually graded.
        // If any of the ESSAY questions have a null alteredGrade, then the
        // assessment still needs to be manually graded.
        return !this.isGradeAltered(submission)
                && submission.getQuestionSubmissions().stream().anyMatch(qs -> {
                    return qs.getQuestion().getQuestionType() == QuestionTypes.ESSAY && qs.getAlteredGrade() == null;
                });
    }

    private boolean isGradeAltered(Submission submission) {

        Float totalAlteredGrade = submission.getTotalAlteredGrade();
        return totalAlteredGrade != null && (!totalAlteredGrade.equals(submission.getAlteredCalculatedGrade()));
    }

    @Override
    public void sendSubmissionGradeToCanvas(Submission submission) throws ConnectionException, DataServiceException, CanvasApiException, IOException {
        //We need, the assignment, and the iss configuration...
        Assignment assignment = submission.getAssessment().getTreatment().getAssignment();
        Experiment experiment = assignment.getExposure().getExperiment();

//        canvasAPIClient.postSubmission(submission, assessmentService.calculateMaxScore(submission.getAssessment()));
        sendSubmissionGradeToCanvasWithLTI(submission);
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
                Group group = submissionOptional.get().getParticipant().getGroup();
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

}
