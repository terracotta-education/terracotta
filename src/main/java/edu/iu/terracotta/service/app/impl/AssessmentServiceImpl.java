package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.*;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.*;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public void updateAssessment(Long id, AssessmentDto assessmentDto)
            throws TitleValidationException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException {
        Assessment assessment = allRepositories.assessmentRepository.findByAssessmentId(id);
        if (StringUtils.isAllBlank(assessmentDto.getTitle()) && StringUtils.isAllBlank(assessment.getTitle())) {
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

        saveAndFlush(assessment);
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
    public AssessmentDto duplicateAssessment(long assessmentId, long treatmentId) throws DataServiceException, AssessmentNotMatchingException {
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

        Assessment newAssessment = save(from);

        // update the treatment
        updateTreatment(treatmentId, newAssessment);

        // duplicate questions
        List<QuestionDto> questionDtos = questionService.duplicateQuestionsForAssessment(oldAssessmentId, newAssessment.getAssessmentId());

        AssessmentDto assessmentDto = toDto(newAssessment, false, false, false, false);
        assessmentDto.setQuestions(questionDtos);

        return assessmentDto;
    }

}
