package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.service.app.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AssessmentServiceImpl implements AssessmentService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    QuestionService questionService;

    @Autowired
    SubmissionService submissionService;

    @Override
    public List<Assessment> findAllByTreatmentId(Long treatmentId){
        return allRepositories.assessmentRepository.findByTreatment_TreatmentId(treatmentId);
    }

    @Override
    public AssessmentDto toDto(Assessment assessment, boolean questions, boolean submissions) {

        AssessmentDto assessmentDto = new AssessmentDto();
        assessmentDto.setAssessmentId(assessment.getAssessmentId());
        assessmentDto.setHtml(assessment.getHtml());
        assessmentDto.setTitle(assessment.getTitle());
        assessmentDto.setAutoSubmit(assessment.getAutoSubmit());
        assessmentDto.setNumOfSubmissions(assessment.getNumOfSubmissions());
        List<QuestionDto> questionDtoList = new ArrayList<>();
        if(questions){
            List<Question> questionList = allRepositories.questionRepository.findByAssessment_AssessmentId(assessment.getAssessmentId());
            for(Question question : questionList) {
                questionDtoList.add(questionService.toDto(question, false));
            }
        }
        assessmentDto.setQuestions(questionDtoList);
        List<SubmissionDto> submissionDtoList = new ArrayList<>();
        if(submissions){
            List<Submission> submissionList =  allRepositories.submissionRepository.findByAssessment_AssessmentId(assessment.getAssessmentId());
            for(Submission submission : submissionList){
                submissionDtoList.add(submissionService.toDto(submission, false,false));
            }
        }
        assessmentDto.setSubmissions(submissionDtoList);
        assessmentDto.setTreatmentId(assessment.getTreatment().getTreatmentId());

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
        if(treatment.isPresent()) {
            assessment.setTreatment(treatment.get());
        } else {
            throw new DataServiceException("The treatment for the assessment does not exist");
        }
        return assessment;
    }

    @Override
    public Assessment save(Assessment assessment) { return allRepositories.assessmentRepository.save(assessment); }

    @Override
    public Optional<Assessment> findById(Long id) { return allRepositories.assessmentRepository.findById(id); }

    @Override
    public void saveAndFlush(Assessment assessmentToChange) { allRepositories.assessmentRepository.saveAndFlush(assessmentToChange); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.assessmentRepository.deleteByAssessmentId(id); }

    @Override
    public boolean assessmentBelongsToExperimentAndConditionAndTreatment(Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) {
        return allRepositories.assessmentRepository
                .existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(
                        experimentId, conditionId, treatmentId, assessmentId);
    }

    @Override
    public Float calculateMaxScore(Assessment assessment){
        Float score = Float.parseFloat("0");
        for (Question question:assessment.getQuestions()){
            score = score + question.getPoints();
        }
        return score;

    }
}
