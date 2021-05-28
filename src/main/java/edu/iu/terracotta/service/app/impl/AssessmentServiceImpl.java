package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.QuestionService;
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

    @Override
    public List<Assessment> findAllByTreatmentId(Long treatmentId){
        return allRepositories.assessmentRepository.findByTreatment_TreatmentId(treatmentId);
    }

    @Override
    public AssessmentDto toDto(Assessment assessment) {

        AssessmentDto assessmentDto = new AssessmentDto();
        assessmentDto.setAssessmentId(assessment.getAssessmentId());
        assessmentDto.setHtml(assessment.getHtml());
        assessmentDto.setTitle(assessment.getTitle());
        List<QuestionDto> questions = new ArrayList<>();
        for(Question question : assessment.getQuestions()){
            questions.add(questionService.toDto(question));
        }
        assessmentDto.setQuestions(questions);
        assessmentDto.setTreatmentId(assessment.getTreatment().getTreatmentId());

        return assessmentDto;
    }


    @Override
    public Assessment fromDto(AssessmentDto assessmentDto) throws DataServiceException {

        Assessment assessment = new Assessment();
        assessment.setAssessmentId(assessmentDto.getAssessmentId());
        assessment.setHtml(assessmentDto.getHtml());
        assessment.setTitle(assessmentDto.getTitle());
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
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.assessmentRepository.deleteById(id); }

    @Override
    public boolean assessmentBelongsToExperimentAndConditionAndTreatment(Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) {
        return allRepositories.assessmentRepository
                .existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(
                        experimentId, conditionId, treatmentId, assessmentId);
    }
}
