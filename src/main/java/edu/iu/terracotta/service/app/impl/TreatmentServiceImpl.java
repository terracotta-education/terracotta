package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.model.dto.TreatmentDto;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMismatchException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentTreatmentService;
import edu.iu.terracotta.service.app.TreatmentService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
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
    @Autowired private ApiJwtService apiJwtService;
    @Autowired private AssessmentService assessmentService;
    @Autowired private AssignmentTreatmentService assignmentTreatmentService;

    @PersistenceContext private EntityManager entityManager;

    @Override
    public List<TreatmentDto> getTreatments(Long conditionId, boolean submissions, SecuredInfo securedInfo) throws AssessmentNotMatchingException, NumberFormatException, ApiException, TerracottaConnectorException {
        List<Treatment> treatments = treatmentRepository.findByCondition_ConditionIdOrderByCondition_ConditionIdAsc(conditionId);

        if (CollectionUtils.isEmpty(treatments)) {
            return Collections.emptyList();
        }

        LtiUserEntity instructorUser = null;

        if (apiJwtService.isInstructorOrHigher(securedInfo)) {
            instructorUser = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        }

        List<TreatmentDto> treatmentDtoList = new ArrayList<>();

        for (Treatment treatment : treatments) {
            // Only add assignment DTO attributes when an instructor user
            if (instructorUser != null) {
                assignmentTreatmentService.setAssignmentDtoAttrs(treatment.getAssignment(), securedInfo.getLmsCourseId(), instructorUser);
            }

            treatmentDtoList.add(assignmentTreatmentService.toTreatmentDto(treatment, submissions, true, securedInfo));
        }

        return treatmentDtoList;
    }

    @Override
    public Treatment getTreatment(Long id) {
        return treatmentRepository.findByTreatmentId(id);
    }

    @Override
    public TreatmentDto postTreatment(TreatmentDto treatmentDto, long conditionId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, TreatmentNotMatchingException {
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

        return assignmentTreatmentService.toTreatmentDto(treatmentSaved, false, true, securedInfo);
    }

    @Override
    public TreatmentDto putTreatment(TreatmentDto treatmentDto, long treatmentId, SecuredInfo securedInfo, boolean questions)
            throws DataServiceException, IdMissingException, AssessmentNotMatchingException, IdMismatchException,
            TreatmentNotMatchingException, TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
            ApiException, AssignmentNotEditedException, IdInPostException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException,
            IntegrationClientNotFoundException, IntegrationNotFoundException {
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

        return assignmentTreatmentService.toTreatmentDto(save(treatment), false, true, securedInfo);
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
