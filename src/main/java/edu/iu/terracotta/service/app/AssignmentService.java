package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.AssignmentMoveException;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

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
     * @param canvasCourseId
     * @param submissions
     * @param includeDeleted
     * @param instructorUserId optional user id of instructor. This would be
     *                         null if authenticating user is a student for
     *                         example.
     * @return
     * @throws AssessmentNotMatchingException
     * @throws CanvasApiException
     */
    List<AssignmentDto> getAssignments(Long exposureId, String canvasCourseId, boolean submissions,
                    boolean includeDeleted, String instructorUserId)
                    throws AssessmentNotMatchingException, CanvasApiException;

    Assignment getAssignment(Long id);

    AssignmentDto postAssignment(AssignmentDto assignmentDto, long experimentId, String canvasCourseId, long exposureId,
                    String instructorUserId)
            throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
            AssignmentNotCreatedException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException;

    AssignmentDto duplicateAssignment(long assignmentId, String canvasCourseId, String instructorUserId)
            throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                    AssignmentNotCreatedException, RevealResponsesSettingValidationException,
                    MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException, ExceedingLimitException,
                    TreatmentNotMatchingException, QuestionNotMatchingException;

    AssignmentDto toDto(Assignment assignment, boolean submissions, boolean addTreatmentDto) throws AssessmentNotMatchingException;

    Assignment fromDto(AssignmentDto assignmentDto) throws DataServiceException;

    Assignment save(Assignment assignment);

    Optional<Assignment> findById(Long id);

    AssignmentDto putAssignment(Long id, AssignmentDto assignmentDto, String canvasCourseId, String instructorUserId) throws TitleValidationException,
            CanvasApiException, AssignmentNotEditedException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException;

    Assignment updateAssignment(Long id, AssignmentDto assignmentDto, String canvasCourseId, String instructorUserId) throws TitleValidationException,
            CanvasApiException, AssignmentNotEditedException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException;

    List<AssignmentDto> updateAssignments(List<AssignmentDto> assignmentDtos, String canvasCourseId,
                    String instructorUserId) throws TitleValidationException,
            CanvasApiException, AssignmentNotEditedException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException;

    Assignment saveAndFlush(Assignment assignmentToChange);

    void deleteById(Long id, String canvasCourseId, String instructorUserId)
                    throws EmptyResultDataAccessException, CanvasApiException, AssignmentNotEditedException;

    boolean assignmentBelongsToExperimentAndExposure(Long experimentId, Long exposureId, Long assignmentId);

    boolean assignmentBelongsToExperiment(Long experimentId, Long assignmentId);

    String lineItemId(Assignment assignment) throws ConnectionException;

    void sendAssignmentGradeToCanvas(Assignment assignment) throws ConnectionException, DataServiceException, CanvasApiException, IOException;

    ResponseEntity<Object> launchAssignment(Long experimentId, SecuredInfo securedInfo)
            throws AssessmentNotMatchingException, ParticipantNotUpdatedException, AssignmentDatesException, DataServiceException, CanvasApiException,
                    IOException, GroupNotMatchingException, ParticipantNotMatchingException, ConnectionException, AssignmentAttemptException, AssignmentNotMatchingException, ExperimentNotMatchingException;

    void checkAndRestoreAllAssignmentsInCanvas() throws CanvasApiException, DataServiceException, ConnectionException, IOException;

    void checkAndRestoreAssignmentsInCanvas(Long platformDeploymentLeyId) throws CanvasApiException, DataServiceException, ConnectionException, IOException;

    void checkAndRestoreAssignmentsInCanvasByContext(Long contextId, String instructorUserId)
                    throws CanvasApiException, DataServiceException, ConnectionException, IOException;

    boolean checkCanvasAssignmentExists(Assignment assignment, String instructorUserId) throws CanvasApiException;

    Assignment restoreAssignmentInCanvas(Assignment assignment) throws CanvasApiException, DataServiceException, ConnectionException, IOException;

    void validateTitle(String title) throws TitleValidationException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long exposureId, long assignmentId);

    void createAssignmentInCanvas(LtiUserEntity instructorUser, Assignment assignment, long experimentId,
                    String canvasCourseId) throws AssignmentNotCreatedException;

    void editAssignmentNameInCanvas(Assignment assignment, String canvasCourseId, String newName,
                    String instructorUserId) throws AssignmentNotEditedException, CanvasApiException;

    void deleteAssignmentInCanvas(Assignment assignment, String canvasCourseId, String instructorUserId)
                    throws AssignmentNotEditedException, CanvasApiException;

    void deleteAllFromExperiment(Long id, SecuredInfo securedInfo);

    void setAssignmentDtoAttrs(Assignment assignment, String canvasCourseId, String instructorUserId)
                    throws NumberFormatException, CanvasApiException;

    AssignmentDto moveAssignment(long assignmentId, AssignmentDto assignmentDto, long experimentId, long exposureId,
                    String canvasCourseId, String instructorUserId)
            throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                    AssignmentNotCreatedException, RevealResponsesSettingValidationException,
                    MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException, ExceedingLimitException,
                    TreatmentNotMatchingException, ExposureNotMatchingException, AssignmentMoveException, AssignmentNotEditedException, QuestionNotMatchingException;

}
