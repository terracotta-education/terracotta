package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMismatchException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.TreatmentService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import edu.iu.terracotta.model.app.Condition;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class TreatmentServiceImpl implements TreatmentService {

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private AssignmentService assignmentService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Treatment> findAllByConditionId(Long conditionId) {
        return allRepositories.treatmentRepository.findByCondition_ConditionId(conditionId);
    }

    @Override
    public List<TreatmentDto> getTreatments(Long conditionId, String canvasCourseId, long platformDeploymentId,
            boolean submissions, String instructorUserId)
            throws AssessmentNotMatchingException, NumberFormatException, CanvasApiException {
        List<Treatment> treatments = findAllByConditionId(conditionId);

        if (CollectionUtils.isEmpty(treatments)) {
            return Collections.emptyList();
        }

        List<TreatmentDto> treatmentDtoList = new ArrayList<>();

        for(Treatment treatment : treatments){
            // Only add assignment DTO attributes when an instructor user
            if (instructorUserId != null) {
                assignmentService.setAssignmentDtoAttrs(treatment.getAssignment(), canvasCourseId, platformDeploymentId,
                        instructorUserId);
            }
            treatmentDtoList.add(toDto(treatment, submissions, true));
        }

        return treatmentDtoList;
    }

    @Override
    public Treatment getTreatment(Long id) {return allRepositories.treatmentRepository.findByTreatmentId(id); }

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
        return toDto(treatmentSaved, false, true);
    }

    @Override
    public TreatmentDto putTreatment(TreatmentDto treatmentDto, long treatmentId)
            throws DataServiceException, IdMissingException, AssessmentNotMatchingException, IdMismatchException {
        if(treatmentDto.getTreatmentId() == null) {
            throw new IdMissingException(TextConstants.ID_MISSING);
        }

        if (!treatmentDto.getTreatmentId().equals(treatmentId)) {
            throw new IdMismatchException(TextConstants.ID_MISMATCH_PUT);
        }

        if (treatmentDto.getAssignmentId() == null){
            throw new DataServiceException(TextConstants.NO_ASSIGNMENT_IN_TREATMENTDTO);
        }

        Treatment treatment;

        try{
            treatment = fromDto(treatmentDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException(String.format(TextConstants.UNABLE_TO_CREATE_TREATMENT, ex.getMessage()));
        }

        Treatment treatmentSaved = save(treatment);

        return toDto(treatmentSaved, false, true);
    }

    @Override
    public TreatmentDto toDto(Treatment treatment, boolean submissions, boolean addAssignmentDto) throws AssessmentNotMatchingException {
        TreatmentDto treatmentDto = new TreatmentDto();

        treatmentDto.setTreatmentId(treatment.getTreatmentId());

        if (addAssignmentDto) {
            treatmentDto.setAssignmentDto(assignmentService.toDto(treatment.getAssignment(), false, false));
        }

        if(treatment.getAssessment() != null) {
            treatmentDto.setAssessmentDto(assessmentService.toDto(treatment.getAssessment(), false,false, submissions, false));
        }

        treatmentDto.setConditionId(treatment.getCondition().getConditionId());
        // keeping assignmentId at the root, as removal will break the UI in many places...
        treatmentDto.setAssignmentId(treatment.getAssignment().getAssignmentId());

        return treatmentDto;
    }

    @Override
    public Treatment fromDto(TreatmentDto treatmentDto) throws DataServiceException{
        Treatment treatment = new Treatment();
        treatment.setTreatmentId(treatmentDto.getTreatmentId());
        Optional<Assignment> assignment = allRepositories.assignmentRepository.findById(treatmentDto.getAssignmentId());

        if (!assignment.isPresent()) {
            throw new DataServiceException(TextConstants.NO_ASSIGNMENT_IN_TREATMENTDTO);
        }

        treatment.setAssignment(assignment.get());
        Optional<Condition> condition = allRepositories.conditionRepository.findById(treatmentDto.getConditionId());

        if(!condition.isPresent()) {
            throw new DataServiceException(TextConstants.NO_CONDITION_FOR_TREATMENT);
        }

        treatment.setCondition(condition.get());

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

    @Override
    public TreatmentDto duplicateTreatment(long treatmentId, String canvasCourseId, long platformDeploymentId,
            String instructorUserId)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            CanvasApiException, TreatmentNotMatchingException {
        return duplicateTreatment(treatmentId, null, canvasCourseId, platformDeploymentId, instructorUserId);
    }

    @Override
    public TreatmentDto duplicateTreatment(long treatmentId, Assignment assignment, String canvasCourseId,
            long platformDeploymentId, String instructorUserId)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            CanvasApiException, TreatmentNotMatchingException {
        Treatment from = getTreatment(treatmentId);

        if (from == null) {
            throw new DataServiceException("The treatment with the given ID does not exist");
        }

        entityManager.detach(from);

        // reset ID
        from.setTreatmentId(null);

        // set new assignment; if exists
        if (assignment != null) {
            from.setAssignment(assignment);
        }

        Treatment newTreatment = save(from);
        assignmentService.setAssignmentDtoAttrs(newTreatment.getAssignment(), canvasCourseId, platformDeploymentId,
                instructorUserId);
        TreatmentDto treatmentDto = toDto(newTreatment, false, true);

        // duplicate assessment
        Assessment existingAssessment = from.getAssessment();

        if (existingAssessment != null) {
            AssessmentDto newAssessment = assessmentService.duplicateAssessment(existingAssessment.getAssessmentId(), newTreatment.getTreatmentId());
            treatmentDto.setAssessmentDto(newAssessment);
        }

        return treatmentDto;
    }

}
