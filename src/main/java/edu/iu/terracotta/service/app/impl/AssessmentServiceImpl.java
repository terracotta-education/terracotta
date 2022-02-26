package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.*;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.*;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Component
public class AssessmentServiceImpl implements AssessmentService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    TreatmentService treatmentService;

    @Autowired
    QuestionService questionService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    FileStorageService fileStorageService;


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
    public AssessmentDto postAssessment(AssessmentDto assessmentDto, long treatmentId) throws IdInPostException, DataServiceException, TitleValidationException, AssessmentNotMatchingException {
        if (assessmentDto.getAssessmentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        validateTitle(assessmentDto.getTitle());
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

        Long submissionsCompletedCount = null;
        Long submissionsInProgressCount = null;
        AssessmentDto assessmentDto = new AssessmentDto();
        assessmentDto.setAssessmentId(assessment.getAssessmentId());
        assessmentDto.setHtml(fileStorageService.parseHTMLFiles(assessment.getHtml()));
        assessmentDto.setTitle(assessment.getTitle());
        assessmentDto.setAutoSubmit(assessment.getAutoSubmit());
        assessmentDto.setNumOfSubmissions(assessment.getNumOfSubmissions());
        List<QuestionDto> questionDtoList = new ArrayList<>();
        if (questions) {
            List<Question> questionList = allRepositories.questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(assessment.getAssessmentId());
            for (Question question : questionList) {
                questionDtoList.add(questionService.toDto(question, answers, student));
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
                submissionDtoList.add(submissionService.toDto(submission, false, false));
                // We add the status. False if in progress, true if submitted.
                if (submission.getDateSubmitted() != null) {
                    participantStatus.put(submission.getParticipant(), true);
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
    public void updateAssessment(Long id, AssessmentDto assessmentDto) throws TitleValidationException {
        Assessment assessment = allRepositories.assessmentRepository.findByAssessmentId(id);
        if (StringUtils.isAllBlank(assessmentDto.getTitle()) && StringUtils.isAllBlank(assessment.getTitle())) {
            throw new TitleValidationException("Error 100: Please give the assessment a title.");
        }
        if (!StringUtils.isAllBlank(assessmentDto.getTitle()) && assessmentDto.getTitle().length() > 255) {
            throw new TitleValidationException("Error 101: Assessment title must be 255 characters or less.");
        }
        assessment.setHtml(assessmentDto.getHtml());
        assessment.setTitle(assessmentDto.getTitle());
        assessment.setAutoSubmit(assessmentDto.getAutoSubmit());
        assessment.setNumOfSubmissions(assessmentDto.getNumOfSubmissions());

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
        if (!StringUtils.isAllBlank(title) && title.length() > 255) {
            throw new TitleValidationException("Error 101: Assessment title must be 255 characters or less.");
        }
    }

    @Override
    public AssessmentDto defaultAssessment(AssessmentDto assessmentDto, Long treatmentId) {
        assessmentDto.setTreatmentId(treatmentId);
        if (assessmentDto.getNumOfSubmissions() == null) {
            assessmentDto.setNumOfSubmissions(1);
        }
        assessmentDto.setAutoSubmit(true);

        return assessmentDto;
    }

    @Override
    public void updateTreatment(Long treatmentId, Assessment assessment) {
        Treatment treatment = allRepositories.treatmentRepository.findByTreatmentId(treatmentId);
        treatment.setAssessment(assessment);
        treatmentService.saveAndFlush(treatment);
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId).toUri());
        return headers;
    }
}