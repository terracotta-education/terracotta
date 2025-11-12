package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.model.dto.TreatmentDto;
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

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

public interface TreatmentService {

    /**
     * Get all treatments for the given condition.
     *
     * @param conditionId
     * @param lmsCourseId
     * @param submissions
     * @param instructorUserId optional user id of instructor. This would be null if authenticating user is a student for example.
     * @return
     * @throws AssessmentNotMatchingException
     * @throws NumberFormatException
     * @throws ApiException
     * @throws TerracottaConnectorException
     */
    List<TreatmentDto> getTreatments(Long conditionId, boolean submissions, SecuredInfo securedInfo) throws AssessmentNotMatchingException, NumberFormatException, ApiException, TerracottaConnectorException;
    Treatment getTreatment(Long id);
    TreatmentDto postTreatment(TreatmentDto treatmentDto, long conditionId, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, TreatmentNotMatchingException;
    TreatmentDto putTreatment(TreatmentDto treatmentDto, long treatmentId, SecuredInfo securedInfo, boolean questions)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, IdMissingException, IdMismatchException, TreatmentNotMatchingException,
        TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssignmentNotEditedException,
        NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException, ApiException;
    Treatment fromDto(TreatmentDto treatmentDto) throws DataServiceException, TreatmentNotMatchingException, AssessmentNotMatchingException;
    void deleteById(Long id) throws EmptyResultDataAccessException;
    void limitToOne(long assignmentId, long conditionId) throws ExceedingLimitException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId);

}
