package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssignmentFileArchiveDto;
import edu.iu.terracotta.service.app.AssignmentFileArchiveService;

public class ExperimentFileArchiveControllerTest extends BaseTest {

    private static final UUID EXPERIMENT_UUID = UUID.randomUUID();
    private static final long EXPERIMENT_ID = 7L;

    @Mock private AssignmentFileArchiveService assignmentFileArchiveService;

    private ExperimentFileArchiveController experimentFileArchiveController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // constructed manually for the same reason as AssignmentFileArchiveControllerTest:
        // @InjectMocks can wire the inherited canvasApiJwtService mock as the ApiJwtService
        experimentFileArchiveController = new ExperimentFileArchiveController(apiJwtService, experimentService, assignmentFileArchiveService);

        when(apiJwtService.extractValues(any(), eq(false))).thenReturn(securedInfo);
        when(experimentService.getExperimentIdByUuid(EXPERIMENT_UUID)).thenReturn(EXPERIMENT_ID);
    }

    @Test
    void testOutdatedReturnsTheExperimentsOutdatedArchives() throws Exception {
        List<AssignmentFileArchiveDto> outdated = List.of(AssignmentFileArchiveDto.builder().assignmentId(UUID.randomUUID()).build());
        when(assignmentFileArchiveService.findOutdated(EXPERIMENT_ID)).thenReturn(outdated);

        ResponseEntity<List<AssignmentFileArchiveDto>> response = experimentFileArchiveController.outdated(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(outdated, response.getBody());
        verify(apiJwtService).experimentAllowed(securedInfo, EXPERIMENT_ID);
    }

    @Test
    void testOutdatedPropagatesExperimentNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(securedInfo, EXPERIMENT_ID);

        assertThrows(ExperimentNotMatchingException.class, () -> experimentFileArchiveController.outdated(EXPERIMENT_UUID, httpServletRequest));
        verify(assignmentFileArchiveService, never()).findOutdated(anyLong());
    }

}
