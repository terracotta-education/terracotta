package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.TreatmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;
import edu.iu.terracotta.model.app.Condition;

import java.util.List;
import java.util.Optional;

@Component
public class TreatmentServiceImpl implements TreatmentService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    AssessmentService assessmentService;

    @Override
    public List<Treatment> findAllByConditionId(Long conditionId) {
        return allRepositories.treatmentRepository.findByCondition_ConditionId(conditionId);
    }

    @Override
    public TreatmentDto toDto(Treatment treatment) {
        TreatmentDto treatmentDto = new TreatmentDto();
        treatmentDto.setTreatmentId(treatment.getTreatmentId());
        if(treatment.getAssessment() != null) {
            treatmentDto.setAssessmentDto(assessmentService.toDto(treatment.getAssessment(), false,false));
        }
        treatmentDto.setConditionId(treatment.getCondition().getConditionId());
        treatmentDto.setAssignmentId(treatment.getAssignment().getAssignmentId());

        return treatmentDto;
    }

    @Override
    public Treatment fromDto(TreatmentDto treatmentDto) throws DataServiceException{
        Treatment treatment = new Treatment();
        treatment.setTreatmentId(treatmentDto.getTreatmentId());
        Optional<Assignment> assignment= allRepositories.assignmentRepository.findById(treatmentDto.getAssignmentId());
        if (assignment.isPresent()) {
            treatment.setAssignment(assignment.get());
        } else {
            throw new DataServiceException("The assignment for the treatment does not exist");
        }
        Optional<Condition> condition = allRepositories.conditionRepository.findById(treatmentDto.getConditionId());
        if(condition.isPresent()) {
            treatment.setCondition(condition.get());
        }else {
            throw new DataServiceException("The condition for the treatment does not exist");
        }
        return treatment;
    }

    @Override
    public Treatment save(Treatment treatment) { return allRepositories.treatmentRepository.save(treatment); }

    @Override
    public Optional<Treatment> findById(Long id) { return allRepositories.treatmentRepository.findById(id); }

    @Override
    public void saveAndFlush(Treatment treatmentToChange) { allRepositories.treatmentRepository.saveAndFlush(treatmentToChange); }

    @Override
    public void deleteById(Long id) { allRepositories.treatmentRepository.deleteById(id); }

    @Override
    public boolean treatmentBelongsToExperimentAndCondition(Long experimentId, Long conditionId, Long treatmentId) {
        return allRepositories.treatmentRepository.existsByCondition_Experiment_ExperimentIdAndCondition_ConditionIdAndTreatmentId(
                experimentId, conditionId, treatmentId);
    }
}
