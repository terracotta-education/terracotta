package edu.iu.terracotta.runner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Assignment;

public class AssignmentDataRunnerTest extends BaseTest {

    private AssignmentDataRunner assignmentDataRunner;
    private ApplicationReadyEvent event;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        assignmentDataRunner = new AssignmentDataRunner(assignmentRepository, submissionRepository);
        event = mock(ApplicationReadyEvent.class);
    }

    @Test
    public void testOnApplicationEventDisabled() {
        ReflectionTestUtils.setField(assignmentDataRunner, "enabled", false);
        ReflectionTestUtils.setField(assignmentDataRunner, "batchSize", 100);

        assignmentDataRunner.onApplicationEvent(event);

        verifyNoInteractions(assignmentRepository, submissionRepository);
    }

    @Test
    public void testOnApplicationEventEnabled() {
        Assignment startedAssignment = Assignment.builder()
            .assignmentId(1L)
            .started(Timestamp.valueOf(LocalDateTime.now()))
            .build();
        Assignment notStartedNoSubmissions = Assignment.builder()
            .assignmentId(2L)
            .build();
        Assignment notStartedWithSubmissions = Assignment.builder()
            .assignmentId(3L)
            .build();

        ReflectionTestUtils.setField(assignmentDataRunner, "enabled", true);
        ReflectionTestUtils.setField(assignmentDataRunner, "batchSize", 100);

        when(assignmentRepository.findAll(PageRequest.of(0, 100)))
            .thenReturn(new PageImpl<>(List.of(startedAssignment, notStartedNoSubmissions, notStartedWithSubmissions)));
        when(assignmentRepository.findAll(PageRequest.of(1, 100)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(2L)).thenReturn(0L);
        when(submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(3L)).thenReturn(5L);

        assignmentDataRunner.onApplicationEvent(event);

        // work happens on a spawned background thread, so a bare verify() can run before it completes; timeout() polls until it lands.
        verify(assignmentRepository, timeout(2000)).save(notStartedWithSubmissions);
        verify(assignmentRepository, timeout(2000)).findAll(PageRequest.of(1, 100));

        assertNotNull(notStartedWithSubmissions.getStarted());
        verify(assignmentRepository, never()).save(startedAssignment);
        verify(assignmentRepository, never()).save(notStartedNoSubmissions);
        // already-started assignments are filtered out before the submission-count check even runs
        verify(submissionRepository, never()).countByAssessment_Treatment_Assignment_AssignmentId(eq(1L));
    }

}
