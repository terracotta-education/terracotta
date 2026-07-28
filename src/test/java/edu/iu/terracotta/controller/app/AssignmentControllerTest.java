package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssignmentDto;
import edu.iu.terracotta.exceptions.AssignmentMoveException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.utils.TextConstants;
import jakarta.servlet.http.HttpServletRequest;

public class AssignmentControllerTest extends BaseTest {

    private AssignmentController assignmentController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // manual construction: ApiJwtService is also implemented by canvasApiJwtService in
        // BaseServiceTest, so @InjectMocks constructor-injection (type-only matching) could wire
        // the wrong candidate.
        assignmentController = new AssignmentController(assignmentService, assignmentTreatmentService, apiJwtService);

        when(apiJwtService.extractValues(any(HttpServletRequest.class), eq(false))).thenReturn(securedInfo);
    }

    @Test
    void allAssignmentsByExposureHappyPathTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(assignmentService.getAssignments(anyLong(), anyBoolean(), anyBoolean(), any(SecuredInfo.class))).thenReturn(List.of(assignmentDto));

        ResponseEntity<List<AssignmentDto>> ret = assignmentController.allAssignmentsByExposure(1, 1, false, false, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(1, ret.getBody().size());
    }

    @Test
    void allAssignmentsByExposureEmptyTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(assignmentService.getAssignments(anyLong(), anyBoolean(), anyBoolean(), any(SecuredInfo.class))).thenReturn(List.of());

        ResponseEntity<List<AssignmentDto>> ret = assignmentController.allAssignmentsByExposure(1, 1, false, false, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, ret.getStatusCode());
    }

    @Test
    void allAssignmentsByExposureUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<List<AssignmentDto>> ret = assignmentController.allAssignmentsByExposure(1, 1, false, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void allAssignmentsByExposurePropagatesAssessmentNotMatchingTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        doThrow(new AssessmentNotMatchingException("no match")).when(assignmentService).getAssignments(anyLong(), anyBoolean(), anyBoolean(), any(SecuredInfo.class));

        assertThrows(AssessmentNotMatchingException.class, () -> assignmentController.allAssignmentsByExposure(1, 1, false, false, httpServletRequest));
    }

    @Test
    void getAssignmentHappyPathTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(assignmentService.getAssignment(anyLong())).thenReturn(assignment);

        ResponseEntity<AssignmentDto> ret = assignmentController.getAssignment(1, 1, 1, false, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(assignmentDto, ret.getBody());
    }

    @Test
    void getAssignmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<AssignmentDto> ret = assignmentController.getAssignment(1, 1, 1, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void getAssignmentPropagatesAssignmentNotMatchingTest() throws Exception {
        doThrow(new AssignmentNotMatchingException("no match")).when(apiJwtService).assignmentAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong());

        assertThrows(AssignmentNotMatchingException.class, () -> assignmentController.getAssignment(1, 1, 1, false, httpServletRequest));
    }

    @Test
    void postAssignmentHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        AssignmentDto returnedDto = AssignmentDto.builder().assignmentId(10L).build();
        when(assignmentService.postAssignment(any(AssignmentDto.class), anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(returnedDto);
        when(assignmentService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), eq(10L))).thenReturn(new HttpHeaders());

        ResponseEntity<AssignmentDto> ret = assignmentController.postAssignment(1, 1, AssignmentDto.builder().build(), mock(UriComponentsBuilder.class), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(returnedDto, ret.getBody());
    }

    @Test
    void postAssignmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<AssignmentDto> ret = assignmentController.postAssignment(1, 1, AssignmentDto.builder().build(), mock(UriComponentsBuilder.class), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void postAssignmentPropagatesTitleValidationTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        doThrow(new TitleValidationException("bad title")).when(assignmentService).postAssignment(any(AssignmentDto.class), anyLong(), anyLong(), any(SecuredInfo.class));

        AssignmentDto dto = AssignmentDto.builder().build();
        UriComponentsBuilder ucBuilder = mock(UriComponentsBuilder.class);

        assertThrows(TitleValidationException.class, () -> assignmentController.postAssignment(1, 1, dto, ucBuilder, httpServletRequest));
    }

    @Test
    void updateAssignmentHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        AssignmentDto updatedDto = AssignmentDto.builder().assignmentId(1L).build();
        when(assignmentService.putAssignment(anyLong(), any(AssignmentDto.class), any(SecuredInfo.class))).thenReturn(updatedDto);

        ResponseEntity<AssignmentDto> ret = assignmentController.updateAssignment(1, 1, 1, AssignmentDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(updatedDto, ret.getBody());
    }

    @Test
    void updateAssignmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<AssignmentDto> ret = assignmentController.updateAssignment(1, 1, 1, AssignmentDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void updateAssignmentPropagatesAssignmentNotEditedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        doThrow(new AssignmentNotEditedException("cannot edit")).when(assignmentService).putAssignment(anyLong(), any(AssignmentDto.class), any(SecuredInfo.class));

        AssignmentDto dto = AssignmentDto.builder().build();

        assertThrows(AssignmentNotEditedException.class, () -> assignmentController.updateAssignment(1, 1, 1, dto, httpServletRequest));
    }

    @Test
    void updateAssignmentsHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        List<AssignmentDto> input = List.of(AssignmentDto.builder().assignmentId(1L).build());
        when(assignmentService.updateAssignments(any(), any(SecuredInfo.class))).thenReturn(input);

        ResponseEntity<List<AssignmentDto>> ret = assignmentController.updateAssignments(1, 1, input, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(input, ret.getBody());
    }

    @Test
    void updateAssignmentsUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<List<AssignmentDto>> ret = assignmentController.updateAssignments(1, 1, List.of(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void updateAssignmentsPropagatesAssignmentNotMatchingTest() throws Exception {
        doThrow(new AssignmentNotMatchingException("no match")).when(apiJwtService).assignmentAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong());

        List<AssignmentDto> input = List.of(AssignmentDto.builder().assignmentId(1L).build());

        assertThrows(AssignmentNotMatchingException.class, () -> assignmentController.updateAssignments(1, 1, input, httpServletRequest));
    }

    @Test
    void deleteAssignmentHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);

        ResponseEntity<Void> ret = assignmentController.deleteAssignment(1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void deleteAssignmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<Void> ret = assignmentController.deleteAssignment(1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void deleteAssignmentNotFoundTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        doThrow(new EmptyResultDataAccessException(1)).when(assignmentService).deleteById(anyLong(), any(SecuredInfo.class));

        ResponseEntity<Void> ret = assignmentController.deleteAssignment(1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, ret.getStatusCode());
    }

    @Test
    void deleteAssignmentPropagatesExperimentLockedTest() throws Exception {
        doThrow(new ExperimentLockedException("locked")).when(apiJwtService).experimentLocked(anyLong(), eq(true));

        assertThrows(ExperimentLockedException.class, () -> assignmentController.deleteAssignment(1, 1, 1, httpServletRequest));
    }

    @Test
    void duplicateAssignmentHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        AssignmentDto returnedDto = AssignmentDto.builder().assignmentId(20L).build();
        when(assignmentService.duplicateAssignment(anyLong(), any(SecuredInfo.class))).thenReturn(returnedDto);
        when(assignmentService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), eq(20L))).thenReturn(new HttpHeaders());

        ResponseEntity<AssignmentDto> ret = assignmentController.duplicateAssignment(1, 1, 1, mock(UriComponentsBuilder.class), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(returnedDto, ret.getBody());
    }

    @Test
    void duplicateAssignmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<AssignmentDto> ret = assignmentController.duplicateAssignment(1, 1, 1, mock(UriComponentsBuilder.class), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void duplicateAssignmentPropagatesAssignmentNotCreatedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        doThrow(new AssignmentNotCreatedException("cannot create")).when(assignmentService).duplicateAssignment(anyLong(), any(SecuredInfo.class));

        UriComponentsBuilder ucBuilder = mock(UriComponentsBuilder.class);

        assertThrows(AssignmentNotCreatedException.class, () -> assignmentController.duplicateAssignment(1, 1, 1, ucBuilder, httpServletRequest));
    }

    @Test
    void moveAssignmentHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        AssignmentDto returnedDto = AssignmentDto.builder().assignmentId(30L).build();
        when(assignmentService.moveAssignment(anyLong(), any(AssignmentDto.class), anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(returnedDto);
        when(assignmentService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), eq(30L))).thenReturn(new HttpHeaders());

        ResponseEntity<AssignmentDto> ret = assignmentController.moveAssignment(1, 1, 1, AssignmentDto.builder().build(), mock(UriComponentsBuilder.class), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(returnedDto, ret.getBody());
    }

    @Test
    void moveAssignmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<AssignmentDto> ret = assignmentController.moveAssignment(1, 1, 1, AssignmentDto.builder().build(), mock(UriComponentsBuilder.class), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void moveAssignmentPropagatesAssignmentMoveExceptionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        doThrow(new AssignmentMoveException("cannot move")).when(assignmentService).moveAssignment(anyLong(), any(AssignmentDto.class), anyLong(), anyLong(), any(SecuredInfo.class));

        AssignmentDto dto = AssignmentDto.builder().build();
        UriComponentsBuilder ucBuilder = mock(UriComponentsBuilder.class);

        assertThrows(AssignmentMoveException.class, () -> assignmentController.moveAssignment(1, 1, 1, dto, ucBuilder, httpServletRequest));
    }

}
