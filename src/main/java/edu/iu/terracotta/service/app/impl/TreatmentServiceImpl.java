package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.TreatmentService;
import edu.iu.terracotta.utils.TextConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import edu.iu.terracotta.model.app.Condition;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
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
    public List<TreatmentDto> getTreatments(Long conditionId, boolean submissions) throws AssessmentNotMatchingException{
        List<Treatment> treatments = findAllByConditionId(conditionId);
        List<TreatmentDto> treatmentDtoList = new ArrayList<>();
        for(Treatment treatment : treatments){
            treatmentDtoList.add(toDto(treatment, submissions));
        }
        return treatmentDtoList;
    }

    @Override
    public Treatment getTreatment(Long id) { return allRepositories.treatmentRepository.findByTreatmentId(id); }

    @Override
    public TreatmentDto postTreatment(TreatmentDto treatmentDto, long conditionId) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException {
        if(treatmentDto.getTreatmentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }
        treatmentDto.setConditionId(conditionId);
        if (treatmentDto.getAssignmentId()==null){
            throw new DataServiceException("Error 129: Unable to create Treatment: The assignmentId is mandatory");
        }
        Treatment treatment;
        try{
            treatment = fromDto(treatmentDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create Treatment: " + ex.getMessage());
        }

        limitToOne(treatment.getAssignment().getAssignmentId(), conditionId);
        Treatment treatmentSaved = save(treatment);
        return toDto(treatmentSaved, false);
    }

    @Override
    public TreatmentDto toDto(Treatment treatment, boolean submissions) throws AssessmentNotMatchingException {
        TreatmentDto treatmentDto = new TreatmentDto();
        treatmentDto.setTreatmentId(treatment.getTreatmentId());
        if(treatment.getAssessment() != null) {
            treatmentDto.setAssessmentDto(assessmentService.toDto(treatment.getAssessment(), false,false, submissions, false));
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
    public void deleteById(Long id) { allRepositories.treatmentRepository.deleteByTreatmentId(id); }

    @Override
    public boolean treatmentBelongsToExperimentAndCondition(Long experimentId, Long conditionId, Long treatmentId) {
        return allRepositories.treatmentRepository.existsByCondition_Experiment_ExperimentIdAndCondition_ConditionIdAndTreatmentId(
                experimentId, conditionId, treatmentId);
    }

    @Override
    public void limitToOne(long assignmentId, long conditionId) throws ExceedingLimitException {
        if(allRepositories.treatmentRepository.existsByAssignment_AssignmentIdAndCondition_ConditionId(assignmentId, conditionId)){
            throw new ExceedingLimitException("Error 141: A treatment for the condition " + conditionId + " and assignment " + assignmentId + " already exists.");
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId).toUri());
        return headers;
    }
}