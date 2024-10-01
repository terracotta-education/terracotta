package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMismatchException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.ConditionRepository;
import edu.iu.terracotta.repository.LtiUserRepository;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentTreatmentService;
import edu.iu.terracotta.service.app.TreatmentService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import edu.iu.terracotta.model.app.Condition;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class TreatmentServiceImpl implements TreatmentService {

    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ConditionRepository conditionRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private APIJWTService apijwtService;
    @Autowired private AssessmentService assessmentService;
    @Autowired private AssignmentTreatmentService assignmentTreatmentService;

    @PersistenceContext private EntityManager entityManager;

    @Override
    public List<TreatmentDto> getTreatments(Long conditionId, boolean submissions, SecuredInfo securedInfo)
            throws AssessmentNotMatchingException, NumberFormatException, CanvasApiException {
        List<Treatment> treatments = treatmentRepository.findByCondition_ConditionId(conditionId);

        if (CollectionUtils.isEmpty(treatments)) {
            return Collections.emptyList();
        }

        LtiUserEntity instructorUser = null;

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        }

        List<TreatmentDto> treatmentDtoList = new ArrayList<>();

        for (Treatment treatment : treatments) {
            // Only add assignment DTO attributes when an instructor user
            if (instructorUser != null) {
                assignmentTreatmentService.setAssignmentDtoAttrs(treatment.getAssignment(), securedInfo.getCanvasCourseId(), instructorUser);
            }

            treatmentDtoList.add(assignmentTreatmentService.toTreatmentDto(treatment, submissions, true));
        }

        return treatmentDtoList;
    }

    @Override
    public Treatment getTreatment(Long id) {
        return treatmentRepository.findByTreatmentId(id);
    }

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

        try {
            treatment = fromDto(treatmentDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException(String.format(TextConstants.UNABLE_TO_CREATE_TREATMENT, ex.getMessage()), ex);
        }

        limitToOne(treatment.getAssignment().getAssignmentId(), conditionId);
        Treatment treatmentSaved = save(treatment);

        return assignmentTreatmentService.toTreatmentDto(treatmentSaved, false, true);
    }

    @Override
    public TreatmentDto putTreatment(TreatmentDto treatmentDto, long treatmentId, SecuredInfo securedInfo, boolean questions)
            throws DataServiceException, IdMissingException, AssessmentNotMatchingException, IdMismatchException,
            TreatmentNotMatchingException, TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, CanvasApiException, AssignmentNotEditedException, IdInPostException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException {
        if (treatmentDto.getTreatmentId() == null) {
            throw new IdMissingException(TextConstants.ID_MISSING);
        }

        if (!treatmentDto.getTreatmentId().equals(treatmentId)) {
            throw new IdMismatchException(TextConstants.ID_MISMATCH_PUT);
        }

        if (treatmentDto.getAssignmentId() == null) {
            throw new DataServiceException(TextConstants.NO_ASSIGNMENT_IN_TREATMENTDTO);
        }

        Treatment treatment = getTreatment(treatmentDto.getTreatmentId());

        if (treatment == null) {
            throw new TreatmentNotMatchingException(TextConstants.TREATMENT_NOT_MATCHING);
        }

        Optional<Condition> condition = conditionRepository.findById(treatmentDto.getConditionId());

        if (condition.isEmpty()) {
            throw new DataServiceException(TextConstants.NO_CONDITION_FOR_TREATMENT);
        }

        treatment.setCondition(condition.get());

        try {
            Assessment assessment = assessmentService.updateAssessment(treatmentDto.getAssessmentDto().getAssessmentId(), treatmentDto.getAssessmentDto(), questions);
            treatment.setAssessment(assessment);
        } catch (AssessmentNotMatchingException e) {
            throw new DataServiceException(String.format(TextConstants.UNABLE_TO_UPDATE_TREATMENT, e.getMessage()), e);
        }

        return assignmentTreatmentService.toTreatmentDto(save(treatment), false, true);
    }

    @Override
    public Treatment fromDto(TreatmentDto treatmentDto) throws DataServiceException{
        Treatment treatment = new Treatment();
        treatment.setTreatmentId(treatmentDto.getTreatmentId());
        Optional<Assignment> assignment = assignmentRepository.findById(treatmentDto.getAssignmentId());

        if (assignment.isEmpty()) {
            throw new DataServiceException(TextConstants.NO_ASSIGNMENT_IN_TREATMENTDTO);
        }

        treatment.setAssignment(assignment.get());
        Optional<Condition> condition = conditionRepository.findById(treatmentDto.getConditionId());

        if (condition.isEmpty()) {
            throw new DataServiceException(TextConstants.NO_CONDITION_FOR_TREATMENT);
        }

        treatment.setCondition(condition.get());

        return treatment;
    }

    private Treatment save(Treatment treatment) {
        return treatmentRepository.save(treatment);
    }

    @Override
    public void deleteById(Long id) {
        treatmentRepository.deleteByTreatmentId(id);
    }

    @Override
    public void limitToOne(long assignmentId, long conditionId) throws ExceedingLimitException {
        if (treatmentRepository.existsByAssignment_AssignmentIdAndCondition_ConditionId(assignmentId, conditionId)) {
            throw new ExceedingLimitException("Error 141: A treatment for the condition " + conditionId + " and assignment " + assignmentId + " already exists.");
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}")
                .buildAndExpand(experimentId, conditionId, treatmentId).toUri());

        return headers;
    }

}
