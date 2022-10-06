package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.RetakeDetails;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AssessmentServiceImpl implements AssessmentService {

    public static final int TITLE_MAX_LENGTH = 255;

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ParticipantService participantService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Assessment> findAllByTreatmentId(Long treatmentId) {
        return allRepositories.assessmentRepository.findByTreatment_TreatmentId(treatmentId);
    }

    @Override
    public List<AssessmentDto> getAllAssessmentsByTreatment(Long treatmentId, boolean submissions) throws AssessmentNotMatchingException {
        List<Assessment> assessmentList = findAllByTreatmentId(treatmentId);
        List<AssessmentDto> assessmentDtoList = new ArrayList<>();
        for (Assessment assessment : assessmentList) {
            assessmentDtoList.add(toDto(assessment, false, false, submissions, false));
        }
        return assessmentDtoList;
    }

    @Override
    public AssessmentDto postAssessment(AssessmentDto assessmentDto, long treatmentId)
            throws IdInPostException, DataServiceException, TitleValidationException, AssessmentNotMatchingException {
        if (assessmentDto.getAssessmentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        validateTitle(assessmentDto.getTitle());
        // treatment level settings are ignored in the DTO when creating an
        // assessment and will be copied from the Treatment's Assignment. These
        // settings can be updated for an assessment by calling
        // updateAssessment.
        assessmentDto = defaultAssessment(assessmentDto, treatmentId);
        Assessment assessment;
        try {
            assessment = fromDto(assessmentDto);
            assessment.setQuestions(new ArrayList<>());
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create Assessment: " + ex.getMessage());
        }

        Assessment assessmentSaved = save(assessment);
        updateTreatment(treatmentId, assessmentSaved);
        return toDto(assessmentSaved, false, false, false, false);
    }

    private AssessmentDto toDto(Assessment assessment, Participant participant, boolean canViewSubmissions) throws AssessmentNotMatchingException {
        AssessmentDto assessmentDto = toDto(assessment, null, false, false, false, false);

        List<Submission> participantSubmissions = submissionService.findByParticipantId(participant.getParticipantId());

        List<Submission> participantSubmissionsSubmitted = CollectionUtils.emptyIfNull(participantSubmissions).stream()
            .filter(submission -> submission.getDateSubmitted() != null)
            .collect(Collectors.toList());

        List<SubmissionDto> submissionDtosSubmitted = CollectionUtils.emptyIfNull(participantSubmissionsSubmitted).stream()
            .map(submission -> submissionService.toDto(submission, false, false))
            .collect(Collectors.toList());

        if (canViewSubmissions) {
            assessmentDto.setSubmissions(submissionDtosSubmitted);
        }

        RetakeDetails retakeDetails = new RetakeDetails();

        try {
            verifySubmissionLimit(assessment.getNumOfSubmissions(), submissionDtosSubmitted.size());
            verifySubmissionWaitTime(assessment.getHoursBetweenSubmissions(), participantSubmissionsSubmitted);

            retakeDetails.setRetakeAllowed(true);
        } catch (AssignmentAttemptException e) {
            retakeDetails.setRetakeAllowed(false);
        }

        retakeDetails.setKeptScore(submissionService.getScoreFromMultipleSubmissions(participant, assessment));
        retakeDetails.setSubmissionAttemptsCount(submissionDtosSubmitted.size());
        assessmentDto.setRetakeDetails(retakeDetails);

        return assessmentDto;
    }

    @Override
    public AssessmentDto toDto(Assessment assessment, boolean questions, boolean answers, boolean submissions, boolean student) throws AssessmentNotMatchingException {
        return toDto(assessment, null, questions, answers, submissions, student);
    }

    @Override
    public AssessmentDto toDto(Assessment assessment, Long submissionId, boolean questions, boolean answers,
            boolean submissions, boolean student) throws AssessmentNotMatchingException {

        Long submissionsCompletedCount = null;
        Long submissionsInProgressCount = null;
        AssessmentDto assessmentDto = new AssessmentDto();
        assessmentDto.setAssessmentId(assessment.getAssessmentId());
        assessmentDto.setHtml(fileStorageService.parseHTMLFiles(assessment.getHtml()));
        assessmentDto.setTitle(assessment.getTitle());
        assessmentDto.setAutoSubmit(assessment.getAutoSubmit());
        assessmentDto.setNumOfSubmissions(assessment.getNumOfSubmissions());
        assessmentDto.setHoursBetweenSubmissions(assessment.getHoursBetweenSubmissions());
        assessmentDto.setMultipleSubmissionScoringScheme(assessment.getMultipleSubmissionScoringScheme().name());
        assessmentDto.setCumulativeScoringInitialPercentage(assessment.getCumulativeScoringInitialPercentage());
        assessmentDto.setAllowStudentViewResponses(assessment.isAllowStudentViewResponses());
        assessmentDto.setStudentViewResponsesAfter(assessment.getStudentViewResponsesAfter());
        assessmentDto.setStudentViewResponsesBefore(assessment.getStudentViewResponsesBefore());
        assessmentDto.setAllowStudentViewCorrectAnswers(assessment.isAllowStudentViewCorrectAnswers());
        assessmentDto.setStudentViewCorrectAnswersAfter(assessment.getStudentViewCorrectAnswersAfter());
        assessmentDto.setStudentViewCorrectAnswersBefore(assessment.getStudentViewCorrectAnswersBefore());
        List<QuestionDto> questionDtoList = new ArrayList<>();
        if (questions) {
            List<Question> questionList = allRepositories.questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(assessment.getAssessmentId());
            for (Question question : questionList) {
                if (submissionId != null) {
                    // apply submission specific ordering to of answers
                    questionDtoList.add(questionService.toDto(question, submissionId, answers, student));
                } else {
                    questionDtoList.add(questionService.toDto(question, answers, student));
                }
            }
        }
        assessmentDto.setQuestions(questionDtoList);
        List<SubmissionDto> submissionDtoList = new ArrayList<>();
        Long conditionId = assessment.getTreatment().getCondition().getConditionId();
        Long exposureId = assessment.getTreatment().getAssignment().getExposure().getExposureId();
        Optional<ExposureGroupCondition> exposureGroupCondition =
                allRepositories.exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(conditionId, exposureId);
        Long groupId = null;
        if (exposureGroupCondition.isPresent()) {
            groupId = exposureGroupCondition.get().getGroup().getGroupId();
        } else {
            throw new AssessmentNotMatchingException("Error 124: Assessment " + assessment.getAssessmentId() + " is without a Group");
        }
        Map<Participant, Boolean> participantStatus = new HashMap<>();

        if (submissions) {
            for (Submission submission : assessment.getSubmissions()) {
                // We add the status. False if in progress, true if submitted.
                if (submission.getDateSubmitted() != null) {
                    participantStatus.put(submission.getParticipant(), true);
                    // Only include the submission if it was submitted
                    submissionDtoList.add(submissionService.toDto(submission, false, false));
                } else { //We considered submitted an assessment if it has been submitted at least one time by the user
                    //including if he is in the middle of taking it again.
                    if (!participantStatus.containsKey(submission.getParticipant())) {
                        participantStatus.put(submission.getParticipant(), false);
                    }
                }
            }
            submissionsCompletedCount = 0L;
            submissionsInProgressCount = 0L;
            for (Map.Entry<Participant, Boolean> status : participantStatus.entrySet()) {
                if (status.getValue()) {
                    submissionsCompletedCount = submissionsCompletedCount + 1;
                } else {
                    submissionsInProgressCount = submissionsInProgressCount + 1;
                }
            }
            assessmentDto.setSubmissionsExpected(allRepositories.participantRepository.countDistinctByGroup_GroupId(groupId));
            assessmentDto.setSubmissionsCompletedCount(submissionsCompletedCount);
            assessmentDto.setSubmissionsInProgressCount(submissionsInProgressCount);
        }
        if (assessment.getSubmissions() != null && !assessment.getSubmissions().isEmpty()) {
            assessmentDto.setStarted(true);
        }
        assessmentDto.setSubmissions(submissionDtoList);
        assessmentDto.setTreatmentId(assessment.getTreatment().getTreatmentId());
        assessmentDto.setMaxPoints(calculateMaxScore(assessment));

        return assessmentDto;
    }


    @Override
    public Assessment fromDto(AssessmentDto assessmentDto) throws DataServiceException {

        Assessment assessment = new Assessment();
        assessment.setAssessmentId(assessmentDto.getAssessmentId());
        assessment.setHtml(assessmentDto.getHtml());
        assessment.setTitle(assessmentDto.getTitle());
        assessment.setAutoSubmit(assessmentDto.getAutoSubmit());
        assessment.setNumOfSubmissions(assessmentDto.getNumOfSubmissions());
        assessment.setHoursBetweenSubmissions(assessmentDto.getHoursBetweenSubmissions());
        assessment.setMultipleSubmissionScoringScheme(
                MultipleSubmissionScoringScheme.valueOf(assessmentDto.getMultipleSubmissionScoringScheme()));
        assessment.setCumulativeScoringInitialPercentage(assessmentDto.getCumulativeScoringInitialPercentage());
        assessment.setAllowStudentViewResponses(assessmentDto.isAllowStudentViewResponses());
        assessment.setStudentViewResponsesAfter(assessmentDto.getStudentViewResponsesAfter());
        assessment.setStudentViewResponsesBefore(assessmentDto.getStudentViewResponsesBefore());
        assessment.setAllowStudentViewCorrectAnswers(assessmentDto.isAllowStudentViewCorrectAnswers());
        assessment.setStudentViewCorrectAnswersAfter(assessmentDto.getStudentViewCorrectAnswersAfter());
        assessment.setStudentViewCorrectAnswersBefore(assessmentDto.getStudentViewCorrectAnswersBefore());
        Optional<Treatment> treatment = allRepositories.treatmentRepository.findById(assessmentDto.getTreatmentId());
        if (treatment.isPresent()) {
            assessment.setTreatment(treatment.get());
        } else {
            throw new DataServiceException("The treatment for the assessment does not exist");
        }
        return assessment;
    }

    @Override
    public Assessment save(Assessment assessment) {
        return allRepositories.assessmentRepository.save(assessment);
    }

    @Override
    public Optional<Assessment> findById(Long id) {
        return allRepositories.assessmentRepository.findById(id);
    }

    @Override
    public Assessment getAssessment(Long id) {
        return allRepositories.assessmentRepository.findByAssessmentId(id);
    }

    @Override
    public AssessmentDto updateAssessment(Long id, AssessmentDto assessmentDto, boolean processQuestions)
            throws TitleValidationException, RevealResponsesSettingValidationException,
                MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, IdInPostException, DataServiceException,
                NegativePointsException, QuestionNotMatchingException {
        Assessment assessment = allRepositories.assessmentRepository.findByAssessmentId(id);

        if (assessment == null) {
            throw new AssessmentNotMatchingException(TextConstants.ASSESSMENT_NOT_MATCHING);
        }

        if (StringUtils.isAllBlank(assessmentDto.getTitle(), assessment.getTitle())) {
            throw new TitleValidationException("Error 100: Please give the assessment a title.");
        }

        validateMultipleAttemptsSettings(assessmentDto);
        validateTitle(assessmentDto.getTitle());
        validateRevealAssignmentResponsesSettings(assessmentDto);
        assessment.setAllowStudentViewResponses(assessmentDto.isAllowStudentViewResponses());
        assessment.setStudentViewResponsesAfter(assessmentDto.getStudentViewResponsesAfter());
        assessment.setStudentViewResponsesBefore(assessmentDto.getStudentViewResponsesBefore());
        assessment.setAllowStudentViewCorrectAnswers(assessmentDto.isAllowStudentViewCorrectAnswers());
        assessment.setStudentViewCorrectAnswersAfter(assessmentDto.getStudentViewCorrectAnswersAfter());
        assessment.setStudentViewCorrectAnswersBefore(assessmentDto.getStudentViewCorrectAnswersBefore());
        assessment.setHtml(assessmentDto.getHtml());
        assessment.setTitle(assessmentDto.getTitle());
        assessment.setAutoSubmit(assessmentDto.getAutoSubmit());
        assessment.setNumOfSubmissions(assessmentDto.getNumOfSubmissions());
        assessment.setHoursBetweenSubmissions(assessmentDto.getHoursBetweenSubmissions());
        MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme
                .valueOf(assessmentDto.getMultipleSubmissionScoringScheme());
        assessment.setMultipleSubmissionScoringScheme(multipleSubmissionScoringScheme);
        assessment.setCumulativeScoringInitialPercentage(assessmentDto.getCumulativeScoringInitialPercentage());

        if (processQuestions) {
            processAssessmentQuestions(assessmentDto);
        }

        return toDto(save(assessment), true, false, false, false);
    }

    private void processAssessmentQuestions(AssessmentDto assessmentDto) throws IdInPostException, DataServiceException, QuestionNotMatchingException, NegativePointsException {
        if (CollectionUtils.isNotEmpty(assessmentDto.getQuestions())) {
            List<Question> questions = questionService.findAllByAssessmentId(assessmentDto.getAssessmentId());

            List<Long> existingQuestionIds = CollectionUtils.emptyIfNull(questions).stream()
                .map(Question::getQuestionId).collect(Collectors.toList());

            Map<Question, QuestionDto> questionMap = new HashMap<>();

            for (QuestionDto questionDto : assessmentDto.getQuestions()) {
                if (questionDto.getQuestionId() == null) {
                    // create new question
                    questionService.postQuestion(questionDto, assessmentDto.getAssessmentId(), false);
                    continue;
                }

                // update question
                Question question = questionService.getQuestion(questionDto.getQuestionId());

                if (question == null) {
                    throw new QuestionNotMatchingException(TextConstants.QUESTION_NOT_MATCHING);
                }

                // remove question ID from list, as it exists in the question DTO list
                existingQuestionIds.remove(question.getQuestionId());
                questionMap.put(question, questionDto);
            }

            if (MapUtils.isNotEmpty(questionMap)) {
                questionService.updateQuestion(questionMap);
            }

            // remove questions not passed in
            CollectionUtils.emptyIfNull(existingQuestionIds).stream()
                .forEach(existingQuestionId -> questionService.deleteById(existingQuestionId));
        } else {
            // delete all questions from the assessment; none were passed in
            List<Question> questions = questionService.findAllByAssessmentId(assessmentDto.getAssessmentId());

            CollectionUtils.emptyIfNull(questions).stream()
                .forEach(question -> questionService.deleteById(question.getQuestionId()));
        }
    }

    @Override
    public void saveAndFlush(Assessment assessmentToChange) {
        allRepositories.assessmentRepository.saveAndFlush(assessmentToChange);
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        allRepositories.assessmentRepository.deleteByAssessmentId(id);
    }

    @Override
    public boolean assessmentBelongsToExperimentAndConditionAndTreatment(Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) {
        return allRepositories.assessmentRepository
                .existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(
                        experimentId, conditionId, treatmentId, assessmentId);
    }

    @Override
    public Float calculateMaxScore(Assessment assessment) {
        float score = Float.parseFloat("0");
        for (Question question : assessment.getQuestions()) {
            score = score + question.getPoints();
        }
        return score;
    }

    @Override
    public void validateTitle(String title) throws TitleValidationException {
        if (!StringUtils.isAllBlank(title) && title.length() > TITLE_MAX_LENGTH) {
            throw new TitleValidationException(String.format("Error 101: Assessment title must be %s characters or less.", TITLE_MAX_LENGTH));
        }
    }

    private void validateMultipleAttemptsSettings(AssessmentDto assessmentDto)
            throws MultipleAttemptsSettingsValidationException {
        MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme
                .valueOf(assessmentDto.getMultipleSubmissionScoringScheme());
        // validate that if multipleSubmissionScoringScheme is CUMULATIVE, then
        // cumulativeScoringInitialPercentage is not null
        if (multipleSubmissionScoringScheme == MultipleSubmissionScoringScheme.CUMULATIVE && assessmentDto.getCumulativeScoringInitialPercentage() == null){
            throw new MultipleAttemptsSettingsValidationException(
                    "Error 156: Must set cumulative scoring initial percentage when scoring scheme is CUMULATIVE");
        }

        // validate that if multipleSubmissionScoringScheme is CUMULATIVE, then
        // numOfSubmissions is not null and greater than 1
        if (multipleSubmissionScoringScheme == MultipleSubmissionScoringScheme.CUMULATIVE && (assessmentDto.getNumOfSubmissions() == null || assessmentDto.getNumOfSubmissions() <= 1)) {
            throw new MultipleAttemptsSettingsValidationException(
                    "Error 157: Number of submissions must be greater than 1, but not infinite, when scoring scheme is CUMULATIVE");
        }
    }

    private void validateRevealAssignmentResponsesSettings(AssessmentDto assessmentDto)
            throws RevealResponsesSettingValidationException {

        // validate that if allowStudentViewCorrectAnswers then also
        // allowStudentViewResponses must be true
        if (assessmentDto.isAllowStudentViewCorrectAnswers() && !assessmentDto.isAllowStudentViewResponses()) {
            throw new RevealResponsesSettingValidationException(
                    "Error 151: Cannot allow students to view correct answers if they are not allowed to view responses.");
        }
        // Validate that view responses 'after' date comes before the 'before' date
        if (assessmentDto.getStudentViewResponsesAfter() != null
                && assessmentDto.getStudentViewResponsesBefore() != null
                && !assessmentDto.getStudentViewResponsesAfter()
                        .before(assessmentDto.getStudentViewResponsesBefore())) {
            throw new RevealResponsesSettingValidationException(
                    "Error 152: Start date of revealing student responses must come before end date.");
        }
        // Validate that view correct answers 'after' date comes before the 'before'
        // date
        if (assessmentDto.getStudentViewCorrectAnswersAfter() != null
                && assessmentDto.getStudentViewCorrectAnswersBefore() != null
                && !assessmentDto.getStudentViewCorrectAnswersAfter()
                        .before(assessmentDto.getStudentViewCorrectAnswersBefore())) {
            throw new RevealResponsesSettingValidationException(
                    "Error 153: Start date of revealing correct answers must come before end date.");
        }
        // Validate studentViewCorrectAnswersAfter is greater than or equal to
        // studentViewResponsesAfter
        if (assessmentDto.getStudentViewCorrectAnswersAfter() != null
                && assessmentDto.getStudentViewResponsesAfter() != null && !(assessmentDto
                        .getStudentViewCorrectAnswersAfter().equals(assessmentDto.getStudentViewResponsesAfter())
                        || assessmentDto.getStudentViewCorrectAnswersAfter()
                                .after(assessmentDto.getStudentViewResponsesAfter()))) {

            throw new RevealResponsesSettingValidationException(
                    "Error 154: Start date of revealing correct answers must equal or come after start date of revealing student responses.");
        }
        // Validate studentViewCorrectAnswersBefore is less than or equal to
        // studentViewResponsesBefore
        if (assessmentDto.getStudentViewCorrectAnswersBefore() != null
                && assessmentDto.getStudentViewResponsesBefore() != null && !(assessmentDto
                        .getStudentViewCorrectAnswersBefore().equals(assessmentDto.getStudentViewResponsesBefore())
                        || assessmentDto.getStudentViewCorrectAnswersBefore()
                                .before(assessmentDto.getStudentViewResponsesBefore()))) {

            throw new RevealResponsesSettingValidationException(
                    "Error 155: End date of revealing correct answers must equal or come before end date of revealing student responses.");
        }
    }

    @Override
    public AssessmentDto defaultAssessment(AssessmentDto assessmentDto, Long treatmentId) {
        assessmentDto.setTreatmentId(treatmentId);

        Treatment treatment = allRepositories.treatmentRepository.findByTreatmentId(treatmentId);
        Assignment assignment = treatment.getAssignment();

        // Default multiple attempts settings to assignment level settings
        assessmentDto.setNumOfSubmissions(assignment.getNumOfSubmissions());
        assessmentDto.setHoursBetweenSubmissions(assignment.getHoursBetweenSubmissions());
        assessmentDto.setMultipleSubmissionScoringScheme(assignment.getMultipleSubmissionScoringScheme().name());
        assessmentDto.setCumulativeScoringInitialPercentage(assignment.getCumulativeScoringInitialPercentage());

        assessmentDto.setAutoSubmit(true);

        // Default reveal treatment responses settings to assignment level settings.
        assessmentDto.setAllowStudentViewResponses(assignment.isAllowStudentViewResponses());
        assessmentDto.setStudentViewResponsesAfter(assignment.getStudentViewResponsesAfter());
        assessmentDto.setStudentViewResponsesBefore(assignment.getStudentViewResponsesBefore());
        assessmentDto.setAllowStudentViewCorrectAnswers(assignment.isAllowStudentViewCorrectAnswers());
        assessmentDto.setStudentViewCorrectAnswersAfter(assignment.getStudentViewCorrectAnswersAfter());
        assessmentDto.setStudentViewCorrectAnswersBefore(assignment.getStudentViewCorrectAnswersBefore());

        return assessmentDto;
    }

    @Override
    public void updateTreatment(Long treatmentId, Assessment assessment) {
        Treatment treatment = allRepositories.treatmentRepository.findByTreatmentId(treatmentId);
        treatment.setAssessment(assessment);
        allRepositories.treatmentRepository.saveAndFlush(treatment);
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId).toUri());
        return headers;
    }

    @Override
    public AssessmentDto duplicateAssessment(long assessmentId, long treatmentId) throws DataServiceException, AssessmentNotMatchingException, TreatmentNotMatchingException {
        Treatment treatment = allRepositories.treatmentRepository.findByTreatmentId(treatmentId);

        if (treatment == null) {
            throw new TreatmentNotMatchingException(TextConstants.TREATMENT_NOT_MATCHING);
        }

        return duplicateAssessment(assessmentId, treatment, null);
    }

    @Override
    public AssessmentDto duplicateAssessment(long assessmentId, Treatment treatment, Assignment assignment) throws DataServiceException, AssessmentNotMatchingException {
        Assessment from = getAssessment(assessmentId);

        if (from == null) {
            throw new DataServiceException("The assessment with the given ID does not exist");
        }

        entityManager.detach(from);

        from.setQuestions(Collections.emptyList());
        from.setSubmissions(Collections.emptyList());

        // reset ID
        Long oldAssessmentId = from.getAssessmentId();
        from.setAssessmentId(null);

        from.setTreatment(treatment);

        Assessment newAssessment = save(from);

        // update the treatment
        updateTreatment(treatment.getTreatmentId(), newAssessment);

        // duplicate questions
        List<QuestionDto> questionDtos = questionService.duplicateQuestionsForAssessment(oldAssessmentId, newAssessment.getAssessmentId());

        AssessmentDto assessmentDto = toDto(newAssessment, false, false, false, false);
        assessmentDto.setQuestions(questionDtos);

        return assessmentDto;
    }

    @Override
    public Assessment getAssessmentForParticipant(Participant participant, SecuredInfo securedInfo) throws AssessmentNotMatchingException {
        Assessment assessment = null;

        if (!participant.getConsent()) {
            //We need the default condition assessment
            for (Condition condition : participant.getExperiment().getConditions()) {
                if (condition.getDefaultCondition()){
                    assessment = getAssessmentByConditionId(participant.getExperiment().getExperimentId(), securedInfo.getCanvasAssignmentId(), condition.getConditionId());
                    break;
                }
            }
        } else {
            if (participant.getGroup() != null) {
                assessment = getAssessmentByGroupId(participant.getExperiment().getExperimentId(), securedInfo.getCanvasAssignmentId(), participant.getGroup().getGroupId());
            }
        }

        if (assessment == null) {
            throw new AssessmentNotMatchingException("There is no assessment available for this user");
        }

        return assessment;
    }

    @Override
    public Assessment getAssessmentByGroupId(Long experimentId, String canvasAssignmentId, Long groupId) throws AssessmentNotMatchingException {
        Assignment assignment = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId, canvasAssignmentId);

        if (assignment == null) {
            throw new AssessmentNotMatchingException("Error 127: This assignment does not exist in Terracotta for this experiment");
        }

        Optional<ExposureGroupCondition> exposureGroupCondition = allRepositories.exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(groupId, assignment.getExposure().getExposureId());

        if (!exposureGroupCondition.isPresent()) {
            throw new AssessmentNotMatchingException("Error 130: This assignment does not have a condition assigned for the participant group.");
        }

        return retrieveTreatmentAssessment(exposureGroupCondition.get().getCondition().getConditionId(), assignment.getAssignmentId());
    }

    @Override
    public Assessment getAssessmentByConditionId(Long experimentId, String canvasAssignmentId, Long conditionId) throws AssessmentNotMatchingException {
        Assignment assignment = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId, canvasAssignmentId);

        if (assignment == null) {
            throw new AssessmentNotMatchingException("Error 127: This assignment does not exist in Terracotta for this experiment");
        }

        return retrieveTreatmentAssessment(conditionId, assignment.getAssignmentId());
    }

    private Assessment retrieveTreatmentAssessment(long conditionId, long assignmentId) throws AssessmentNotMatchingException {
        List<Treatment> treatments = allRepositories.treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentId(conditionId, assignmentId);

        if (treatments.isEmpty()) {
            throw new AssessmentNotMatchingException("Error 131: This assignment does not have a treatment assigned.");
        }

        if (treatments.size() > 1) {
            throw new AssessmentNotMatchingException("Error 132: This assignment has ambiguous treatments. Please contact a Terracotta administrator");
        }

        if (treatments.get(0).getAssessment() == null) {
            throw new AssessmentNotMatchingException("Error 133: The treatment for this assignment does not have an assessment created");
        }

        return treatments.get(0).getAssessment();
    }

    @Override
    public AssessmentDto viewAssessment(long experimentId, SecuredInfo securedInfo)
            throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException,
                GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException {
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(experimentId);

        if (!experiment.isPresent()) {
            throw new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING);
        }

        Participant participant = participantService.handleExperimentParticipant(experiment.get(), securedInfo);

        Assessment assessment = null;

        if (BooleanUtils.isNotTrue(participant.getConsent())) {
            // need the default condition assessment
            Optional<Long> conditionId = experiment.get().getConditions().stream()
                .filter(Condition::getDefaultCondition)
                .map(Condition::getConditionId)
                .findFirst();

            if (conditionId.isPresent()) {
                assessment = getAssessmentByConditionId(experimentId, securedInfo.getCanvasAssignmentId(), conditionId.get());
            }
        } else {
            if (participant.getGroup() != null) {
                assessment = getAssessmentByGroupId(experimentId, securedInfo.getCanvasAssignmentId(), participant.getGroup().getGroupId());
            }
        }

        if (assessment == null) {
            throw new AssessmentNotMatchingException("There is no assessment available for this user");
        }

        return toDto(assessment, participant, assessment.isAllowStudentViewResponses());
    }

    @Override
    public void verifySubmissionLimit(Integer limit, int existingSubmissionsCount) throws AssignmentAttemptException {
        if (limit == null || limit == 0) {
            return;
        }

        if (existingSubmissionsCount < limit) {
            return;
        }

        throw new AssignmentAttemptException(TextConstants.LIMIT_OF_SUBMISSIONS_REACHED);
    }

    @Override
    public void verifySubmissionWaitTime(Float waitTime, List<Submission> submissionList) throws AssignmentAttemptException {
        if (waitTime == null || waitTime == 0F) {
            return;
        }

        if (CollectionUtils.isEmpty(submissionList)) {
            return;
        }

        // calculate the allowable submission time limit
        Timestamp limit = Timestamp.from(Instant.now().minus(Math.round(waitTime * 60 * 60), ChronoUnit.SECONDS));

        // check for any submissions after the allowable time
        Optional<Submission> invalidSubmission = submissionList.stream()
            .filter(submission -> submission.getDateSubmitted().after(limit))
            .findAny();

        if (!invalidSubmission.isPresent()) {
            return;
        }

        // there are existing submissions that are not passed the time limit
        throw new AssignmentAttemptException(TextConstants.ASSIGNMENT_SUBMISSION_WAIT_TIME_NOT_REACHED);
    }

}
