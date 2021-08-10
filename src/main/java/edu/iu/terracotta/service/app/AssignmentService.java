package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Group;
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

    List<Assignment> findAllByExposureId(long exposureId);

    List<AssignmentDto> getAssignments(Long exposureId, boolean submissions) throws AssessmentNotMatchingException;

    Assignment getAssignment(Long id);

    AssignmentDto postAssignment(AssignmentDto assignmentDto, long experimentId, String CavnasCourseId, long exposureId) throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException, AssignmentNotCreatedException;

    AssignmentDto toDto(Assignment assignment, boolean submissions) throws AssessmentNotMatchingException;

    Assignment fromDto(AssignmentDto assignmentDto) throws DataServiceException;

    Assignment save(Assignment assignment);

    Optional<Assignment> findById(Long id);

    void updateAssignment(Long id, AssignmentDto assignmentDto, String canvasCourseId ) throws TitleValidationException, CanvasApiException, AssignmentNotEditedException;

    void saveAndFlush(Assignment assignmentToChange);

    void deleteById(Long id, String canvasCourseId) throws EmptyResultDataAccessException, CanvasApiException, AssignmentNotEditedException;

    boolean assignmentBelongsToExperimentAndExposure(Long experimentId, Long exposureId, Long assignmentId);

    boolean assignmentBelongsToExperiment(Long experimentId, Long assignmentId);

    String lineItemId(Assignment assignment) throws ConnectionException;

    void sendAssignmentGradeToCanvas(Assignment assignment) throws ConnectionException, DataServiceException, CanvasApiException, IOException;

    Assessment getAssessmentByGroupId(Long experimentId, String canvasAssignmentId, Long groupId) throws AssessmentNotMatchingException;

    Assessment getAssessmentByConditionId(Long experimentId, String canvasAssignmentId, Long conditionId) throws AssessmentNotMatchingException;

    Group getUniqueGroupByConditionId(Long experimentId, String canvasAssignmentId, Long conditionId) throws GroupNotMatchingException;

    ResponseEntity<Object> launchAssignment(Long experimentId, SecuredInfo securedInfo) throws AssessmentNotMatchingException, ParticipantNotUpdatedException, AssignmentDatesException, DataServiceException, CanvasApiException, IOException, GroupNotMatchingException, ParticipantNotMatchingException;

    void checkAndRestoreAllAssignmentsInCanvas() throws CanvasApiException, DataServiceException, ConnectionException, IOException;

    void checkAndRestoreAssignmentsInCanvas(Long platformDeploymentLeyId) throws CanvasApiException, DataServiceException, ConnectionException, IOException;

    void checkAndRestoreAssignmentsInCanvasByContext(Long contextId) throws CanvasApiException, DataServiceException, ConnectionException, IOException;

    boolean checkCanvasAssignmentExists(Assignment assignment) throws CanvasApiException;

    Assignment restoreAssignmentInCanvas(Assignment assignment) throws CanvasApiException, DataServiceException, ConnectionException, IOException;

    void validateTitle(String title) throws TitleValidationException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long exposureId, long assignmentId);

    void createAssignmentInCanvas(Assignment assignment, long experimentId, String canvasCourseId) throws AssignmentNotCreatedException;

    void editAssignmentNameInCanvas(Assignment assignment, String canvasCourseId, String newName) throws AssignmentNotEditedException, CanvasApiException;

    void deleteAssignmentInCanvas(Assignment assignment, String canvasCourseId) throws AssignmentNotEditedException, CanvasApiException;

    void deleteAllFromExperiment(Long id, SecuredInfo securedInfo);
}