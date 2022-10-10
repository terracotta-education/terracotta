package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMismatchException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
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
    public List<TreatmentDto> getTreatments(Long conditionId, String canvasCourseId, long platformDeploymentId, boolean submissions)
            throws AssessmentNotMatchingException, NumberFormatException, CanvasApiException {
        List<Treatment> treatments = findAllByConditionId(conditionId);

        if (CollectionUtils.isEmpty(treatments)) {
            return Collections.emptyList();
        }

        List<TreatmentDto> treatmentDtoList = new ArrayList<>();

        for(Treatment treatment : treatments){
            assignmentService.setAssignmentDtoAttrs(treatment.getAssignment(), canvasCourseId, platformDeploymentId);
            treatmentDtoList.add(toDto(treatment, submissions, true));
        }

        return treatmentDtoList;
    }

    @Override
    public Treatment getTreatment(Long id) {return allRepositories.treatmentRepository.findByTreatmentId(id); }

    @Override
    public TreatmentDto postTreatment(TreatmentDto treatmentDto, long conditionId) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, TreatmentNotMatchingException {
        if (treatmentDto.getTreatmentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        treatmentDto.setConditionId(conditionId);

        if (treatmentDto.getAssignmentId() == null) {
            throw new DataServiceException("Error 129: Unable to create Treatment: The assignmentId is mandatory");
        }

        Treatment treatment = null;

        try{
            treatment = fromDto(treatmentDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException(String.format(TextConstants.UNABLE_TO_CREATE_TREATMENT, ex.getMessage()));
        }

        limitToOne(treatment.getAssignment().getAssignmentId(), conditionId);
        Treatment treatmentSaved = save(treatment);

        return toDto(treatmentSaved, false, true);
    }

    @Override
    public TreatmentDto putTreatment(TreatmentDto treatmentDto, long treatmentId, SecuredInfo securedInfo, boolean questions)
            throws DataServiceException, IdMissingException, AssessmentNotMatchingException, IdMismatchException,
            TreatmentNotMatchingException, TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, CanvasApiException, AssignmentNotEditedException, IdInPostException, NegativePointsException, QuestionNotMatchingException {
        if(treatmentDto.getTreatmentId() == null) {
            throw new IdMissingException(TextConstants.ID_MISSING);
        }

        if (!treatmentDto.getTreatmentId().equals(treatmentId)) {
            throw new IdMismatchException(TextConstants.ID_MISMATCH_PUT);
        }

        if (treatmentDto.getAssignmentId() == null){
            throw new DataServiceException(TextConstants.NO_ASSIGNMENT_IN_TREATMENTDTO);
        }

        Treatment treatment = getTreatment(treatmentDto.getTreatmentId());

        if (treatment == null) {
            throw new TreatmentNotMatchingException(TextConstants.TREATMENT_NOT_MATCHING);
        }

        Optional<Condition> condition = allRepositories.conditionRepository.findById(treatmentDto.getConditionId());

        if(!condition.isPresent()) {
            throw new DataServiceException(TextConstants.NO_CONDITION_FOR_TREATMENT);
        }

        treatment.setCondition(condition.get());

        try {
            assessmentService.updateAssessment(treatmentDto.getAssessmentDto().getAssessmentId(), treatmentDto.getAssessmentDto(), questions);

            if (treatmentDto.getAssignmentDto() != null) {
                assignmentService.updateAssignment(treatmentDto.getAssignmentId(), treatmentDto.getAssignmentDto(), securedInfo.getCanvasCourseId());
            }
        } catch (AssessmentNotMatchingException | AssignmentNotMatchingException e) {
            throw new DataServiceException(String.format(TextConstants.UNABLE_TO_UPDATE_TREATMENT, e.getMessage()));
        }

        return toDto(save(treatment), false, true);
    }

    @Override
    public TreatmentDto toDto(Treatment treatment, boolean submissions, boolean addAssignmentDto) throws AssessmentNotMatchingException {
        TreatmentDto treatmentDto = new TreatmentDto();

        treatmentDto.setTreatmentId(treatment.getTreatmentId());

        if (addAssignmentDto) {
            treatmentDto.setAssignmentDto(assignmentService.toDto(treatment.getAssignment(), false, false));
        }

        if(treatment.getAssessment() != null) {
            treatmentDto.setAssessmentDto(assessmentService.toDto(treatment.getAssessment(), true,false, submissions, false));
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
    public Treatment saveAndFlush(Treatment treatmentToChange) { return allRepositories.treatmentRepository.saveAndFlush(treatmentToChange); }

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
    public TreatmentDto duplicateTreatment(long treatmentId, String canvasCourseId, long platformDeploymentId)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException {
        return duplicateTreatment(treatmentId, null, canvasCourseId, platformDeploymentId);
    }

    @Override
    public TreatmentDto duplicateTreatment(long treatmentId, Assignment assignment, String canvasCourseId, long platformDeploymentId)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException {
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

        // unset the assessment
        from.setAssessment(null);

        Treatment newTreatment = save(from);
        assignmentService.setAssignmentDtoAttrs(newTreatment.getAssignment(), canvasCourseId, platformDeploymentId);
        TreatmentDto treatmentDto = toDto(newTreatment, false, true);

        // duplicate assessment
        List<Assessment> existingAssessments = assessmentService.findAllByTreatmentId(treatmentId);

        if (CollectionUtils.isNotEmpty(existingAssessments)) {
            Assessment newAssessment = assessmentService.duplicateAssessment(existingAssessments.get(0).getAssessmentId(), newTreatment, assignment);
            newTreatment.setAssessment(newAssessment);
            saveAndFlush(newTreatment);
            treatmentDto.setAssessmentDto(assessmentService.toDto(newAssessment, true, true, true, false));
        }

        return treatmentDto;
    }

}