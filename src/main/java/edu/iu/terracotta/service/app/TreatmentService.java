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
import edu.iu.terracotta.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

public interface TreatmentService {

    /**
     * Get all treatments for the given condition.
     *
     * @param conditionId
     * @param canvasCourseId
     * @param submissions
     * @param instructorUserId optional user id of instructor. This would be
     *                         null if authenticating user is a student for
     *                         example.
     * @return
     * @throws AssessmentNotMatchingException
     * @throws NumberFormatException
     * @throws CanvasApiException
     */
    List<TreatmentDto> getTreatments(Long conditionId, boolean submissions, SecuredInfo securedInfo) throws AssessmentNotMatchingException, NumberFormatException, CanvasApiException;
    Treatment getTreatment(Long id);
    TreatmentDto postTreatment(TreatmentDto treatmentDto, long conditionId) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, TreatmentNotMatchingException;
    TreatmentDto putTreatment(TreatmentDto treatmentDto, long treatmentId, SecuredInfo securedInfo, boolean questions) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, IdMissingException, IdMismatchException, TreatmentNotMatchingException, TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, CanvasApiException, AssignmentNotEditedException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException;
    Treatment fromDto(TreatmentDto treatmentDto) throws DataServiceException, TreatmentNotMatchingException, AssessmentNotMatchingException;
    void deleteById(Long id) throws EmptyResultDataAccessException;
    void limitToOne(long assignmentId, long conditionId) throws ExceedingLimitException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId);

}
