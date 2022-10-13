package edu.iu.terracotta.service.app;

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
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

public interface TreatmentService {

    List<Treatment> findAllByConditionId(Long conditionId);

    List<TreatmentDto> getTreatments(Long conditionId, String canvasCourseId, long platformDeploymentId, boolean submissions) throws AssessmentNotMatchingException, NumberFormatException, CanvasApiException;

    Treatment getTreatment(Long id);

    TreatmentDto postTreatment(TreatmentDto treatmentDto, long conditionId) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, TreatmentNotMatchingException;

    TreatmentDto putTreatment(TreatmentDto treatmentDto, long treatmentId, SecuredInfo securedInfo, boolean questions) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, IdMissingException, IdMismatchException, TreatmentNotMatchingException, TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, CanvasApiException, AssignmentNotEditedException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException;

    TreatmentDto duplicateTreatment(long treatmentId, Assignment assignment, String canvasCourseId, long platformDeploymentId) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException, CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException;

    TreatmentDto duplicateTreatment(long treatmentId, String canvasCourseId, long platformDeploymentId) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException, CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException;

    TreatmentDto toDto(Treatment treatment, boolean submissions, boolean addAssignmentDto) throws AssessmentNotMatchingException;

    Treatment fromDto(TreatmentDto treatmentDto) throws DataServiceException, TreatmentNotMatchingException, AssessmentNotMatchingException;

    Treatment save(Treatment treatment);

    Optional<Treatment> findById(Long id);

    Treatment saveAndFlush(Treatment treatmentToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean treatmentBelongsToExperimentAndCondition(Long experimentId, Long conditionId, Long treatmentId);

    void limitToOne(long assignmentId, long conditionId) throws ExceedingLimitException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId);
}
