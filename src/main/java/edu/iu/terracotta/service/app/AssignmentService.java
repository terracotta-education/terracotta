package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.dto.AssignmentDto;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.AssignmentMoveException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface AssignmentService {

    List<Assignment> findAllByExposureId(long exposureId, boolean includeDeleted);

    /**
     * Get all assignments for the given exposure set id.
     *
     * @param exposureId
     * @param lmsCourseId
     * @param submissions
     * @param includeDeleted
     * @param instructorUserId optional user id of instructor. This would be
     *                         null if authenticating user is a student for
     *                         example.
     * @return
     * @throws AssessmentNotMatchingException
     * @throws ApiException
     * @throws TerracottaConnectorException
     * @throws NumberFormatException
     */
    List<AssignmentDto> getAssignments(Long exposureId, boolean submissions, boolean includeDeleted, SecuredInfo securedInfo) throws AssessmentNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException;

    Assignment getAssignment(Long id);
    AssignmentDto postAssignment(AssignmentDto assignmentDto, long experimentId, long exposureId, SecuredInfo securedInfo)
            throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
            AssignmentNotCreatedException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, NumberFormatException, ApiException, TerracottaConnectorException;
    AssignmentDto duplicateAssignment(long assignmentId, SecuredInfo securedInfo)
            throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                    AssignmentNotCreatedException, RevealResponsesSettingValidationException,
                    MultipleAttemptsSettingsValidationException, NumberFormatException, ExceedingLimitException,
                    TreatmentNotMatchingException, QuestionNotMatchingException, ApiException, TerracottaConnectorException;
    Assignment fromDto(AssignmentDto assignmentDto) throws DataServiceException;
    Assignment save(Assignment assignment);
    Optional<Assignment> findById(Long id);
    AssignmentDto putAssignment(Long id, AssignmentDto assignmentDto, SecuredInfo securedInfo) throws TitleValidationException,
            AssignmentNotEditedException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException, ApiException, TerracottaConnectorException;
    Assignment updateAssignment(Long id, AssignmentDto assignmentDto, SecuredInfo securedInfo) throws TitleValidationException,
            AssignmentNotEditedException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException, ApiException, TerracottaConnectorException;
    List<AssignmentDto> updateAssignments(List<AssignmentDto> assignmentDtos, SecuredInfo securedInfo) throws TitleValidationException,
            AssignmentNotEditedException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException, ApiException, TerracottaConnectorException;
    Assignment saveAndFlush(Assignment assignmentToChange);
    void deleteById(Long id, SecuredInfo securedInfo) throws EmptyResultDataAccessException, AssignmentNotEditedException, ApiException, TerracottaConnectorException;
    boolean assignmentBelongsToExperimentAndExposure(Long experimentId, Long exposureId, Long assignmentId);
    boolean assignmentBelongsToExperiment(Long experimentId, Long assignmentId);
    void sendAssignmentGradeToLms(Assignment assignment) throws ConnectionException, DataServiceException, IOException, ApiException, TerracottaConnectorException;
    ResponseEntity<Object> launchAssignment(Long experimentId, SecuredInfo securedInfo)
            throws AssessmentNotMatchingException, ParticipantNotUpdatedException, AssignmentDatesException, DataServiceException,
                    IOException, GroupNotMatchingException, ParticipantNotMatchingException, ConnectionException, AssignmentAttemptException, AssignmentNotMatchingException, ExperimentNotMatchingException, ApiException, TerracottaConnectorException, IntegrationTokenNotFoundException;
    void checkAndRestoreAllAssignmentsInLms() throws DataServiceException, ConnectionException, IOException, ApiException, NumberFormatException, TerracottaConnectorException;
    void checkAndRestoreAssignmentsInLms(Long platformDeploymentKeyId) throws DataServiceException, ConnectionException, IOException, ApiException, NumberFormatException, TerracottaConnectorException;
    List<LmsAssignment> checkAndRestoreAssignmentsInLmsByContext(SecuredInfo securedInfo) throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException;
    boolean checkLmsAssignmentExists(Assignment assignment, LtiUserEntity instructorUser) throws ApiException, NumberFormatException, TerracottaConnectorException;
    Assignment restoreAssignmentInLms(Assignment assignment) throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException;
    void validateTitle(String title) throws TitleValidationException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long exposureId, long assignmentId);
    void createAssignmentInLms(LtiUserEntity instructorUser, Assignment assignment, long experimentId, String lmsCourseId) throws AssignmentNotCreatedException, TerracottaConnectorException;
    void editAssignmentNameInLms(Assignment assignment, String lmsCourseId, String newName, LtiUserEntity instructorUser) throws AssignmentNotEditedException, ApiException, TerracottaConnectorException;
    void deleteAssignmentInLms(Assignment assignment, String lmsCourseId, LtiUserEntity instructorUser) throws AssignmentNotEditedException, ApiException, TerracottaConnectorException;
    void deleteAllFromExperiment(Long id, SecuredInfo securedInfo) throws TerracottaConnectorException;
    AssignmentDto moveAssignment(long assignmentId, AssignmentDto assignmentDto, long experimentId, long exposureId, SecuredInfo securedInfo)
            throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                    AssignmentNotCreatedException, RevealResponsesSettingValidationException, AssignmentNotMatchingException,
                    MultipleAttemptsSettingsValidationException, NumberFormatException, ExceedingLimitException,
                    TreatmentNotMatchingException, ExposureNotMatchingException, AssignmentMoveException, AssignmentNotEditedException, QuestionNotMatchingException, ApiException;
    List<LmsAssignment> getAllAssignmentsForLmsCourse(SecuredInfo securedInfo) throws ApiException, TerracottaConnectorException;
    Optional<LmsAssignment> getLmsAssignmentById(String lmsAssignmentId, SecuredInfo securedInfo) throws ApiException, TerracottaConnectorException;
    boolean isSingleVersion(long assignmentId);
    boolean isSingleVersion(Assignment assignment);

}
