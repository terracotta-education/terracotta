package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssignmentFileArchiveDto;
import edu.iu.terracotta.exceptions.AssignmentFileArchiveNotFoundException;
import edu.iu.terracotta.service.app.AssignmentFileArchiveService;

public class AssignmentFileArchiveControllerTest extends BaseTest {

    private static final long EXPERIMENT_ID = 1L;
    private static final long EXPOSURE_ID = 2L;
    private static final long ASSIGNMENT_ID = 3L;
    private static final UUID FILE_ID = UUID.randomUUID();

    private AssignmentFileArchiveController assignmentFileArchiveController;

    // Not present in the BaseServiceTest/BaseRepositoryTest/BaseModelTest hierarchy, so it
    // is declared locally rather than reused from an inherited field.
    @Mock private AssignmentFileArchiveService assignmentFileArchiveService;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // apiJwtService below also collides with CanvasApiJwtServiceImpl in BaseServiceTest (see the
        // @InjectMocks pitfall note there), so this class is constructed manually instead of relying
        // on @InjectMocks, which non-deterministically wired the wrong mock and left apiJwtService
        // calls silently unstubbed.
        assignmentFileArchiveController = new AssignmentFileArchiveController(apiJwtService, assignmentFileArchiveService);

        when(apiJwtService.extractValues(any(), eq(false))).thenReturn(securedInfo);
        when(apiJwtService.exposureAllowed(eq(securedInfo), anyLong(), anyLong())).thenReturn(exposure);
        when(apiJwtService.assignmentAllowed(eq(securedInfo), anyLong(), anyLong(), anyLong())).thenReturn(assignment);
    }

    @Test
    void testFilesSuccess() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        AssignmentFileArchiveDto dto = AssignmentFileArchiveDto.builder().assignmentId(ASSIGNMENT_ID).build();
        when(assignmentFileArchiveService.process(assignment, securedInfo)).thenReturn(dto);

        ResponseEntity<AssignmentFileArchiveDto> response = assignmentFileArchiveController.files(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, httpServletRequest);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testFilesUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<AssignmentFileArchiveDto> response = assignmentFileArchiveController.files(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testFilesPropagatesExperimentNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(securedInfo, EXPERIMENT_ID);

        assertThrows(ExperimentNotMatchingException.class, () -> assignmentFileArchiveController.files(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, httpServletRequest));
    }

    @Test
    void testFilesPropagatesExposureNotMatching() throws Exception {
        doThrow(new ExposureNotMatchingException("not matching")).when(apiJwtService).exposureAllowed(securedInfo, EXPERIMENT_ID, EXPOSURE_ID);

        assertThrows(ExposureNotMatchingException.class, () -> assignmentFileArchiveController.files(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, httpServletRequest));
    }

    @Test
    void testPollSuccess() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        AssignmentFileArchiveDto dto = AssignmentFileArchiveDto.builder().assignmentId(ASSIGNMENT_ID).build();
        when(assignmentFileArchiveService.poll(assignment, securedInfo, false)).thenReturn(dto);

        ResponseEntity<AssignmentFileArchiveDto> response = assignmentFileArchiveController.poll(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, false, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testPollUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<AssignmentFileArchiveDto> response = assignmentFileArchiveController.poll(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testPollNotFoundReturnsBadRequest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(assignmentFileArchiveService.poll(assignment, securedInfo, true)).thenThrow(new AssignmentFileArchiveNotFoundException("not found"));

        ResponseEntity<AssignmentFileArchiveDto> response = assignmentFileArchiveController.poll(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, true, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testPollPropagatesAssignmentNotMatching() throws Exception {
        doThrow(new AssignmentNotMatchingException("not matching")).when(apiJwtService).assignmentAllowed(securedInfo, EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID);

        assertThrows(AssignmentNotMatchingException.class, () -> assignmentFileArchiveController.poll(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, false, httpServletRequest));
    }

    @Test
    void testRetrieveSuccess() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        File realFile = File.createTempFile("archive", ".zip");
        realFile.deleteOnExit();
        AssignmentFileArchiveDto dto = AssignmentFileArchiveDto.builder()
            .id(FILE_ID)
            .fileName("archive.zip")
            .mimeType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .file(realFile)
            .build();
        when(assignmentFileArchiveService.retrieve(FILE_ID, assignment, securedInfo)).thenReturn(dto);

        ResponseEntity<Resource> response = assignmentFileArchiveController.retrieve(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, FILE_ID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=archive.zip", response.getHeaders().getFirst("Content-Disposition"));
        assertEquals(realFile.length(), response.getHeaders().getContentLength());
        assertTrue(response.getBody() instanceof InputStreamResource);
    }

    @Test
    void testRetrieveOutdatedReturnsConflict() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        AssignmentFileArchiveDto dto = AssignmentFileArchiveDto.builder().id(FILE_ID).file(null).build();
        when(assignmentFileArchiveService.retrieve(FILE_ID, assignment, securedInfo)).thenReturn(dto);

        ResponseEntity<Resource> response = assignmentFileArchiveController.retrieve(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, FILE_ID, httpServletRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testRetrieveUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Resource> response = assignmentFileArchiveController.retrieve(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, FILE_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testErrorAcknowledgeSuccess() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Void> response = assignmentFileArchiveController.errorAcknowledge(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, FILE_ID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testErrorAcknowledgeUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = assignmentFileArchiveController.errorAcknowledge(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, FILE_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testErrorAcknowledgeNotFoundReturnsBadRequest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new AssignmentFileArchiveNotFoundException("not found")).when(assignmentFileArchiveService).errorAcknowledge(FILE_ID, assignment);

        ResponseEntity<Void> response = assignmentFileArchiveController.errorAcknowledge(EXPERIMENT_ID, EXPOSURE_ID, ASSIGNMENT_ID, FILE_ID, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

}
