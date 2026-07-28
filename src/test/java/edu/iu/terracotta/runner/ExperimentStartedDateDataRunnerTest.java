package edu.iu.terracotta.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;

public class ExperimentStartedDateDataRunnerTest extends BaseTest {

    private ExperimentStartedDateDataRunner experimentStartedDateDataRunner;
    private ApplicationReadyEvent event;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        experimentStartedDateDataRunner = new ExperimentStartedDateDataRunner(assignmentRepository, experimentRepository);
        event = mock(ApplicationReadyEvent.class);
    }

    @Test
    public void testOnApplicationEventDisabled() {
        ReflectionTestUtils.setField(experimentStartedDateDataRunner, "enabled", false);

        experimentStartedDateDataRunner.onApplicationEvent(event);

        verifyNoInteractions(assignmentRepository, experimentRepository);
    }

    @Test
    public void testOnApplicationEventNoStartedAssignmentClearsExperimentStartedDate() {
        Experiment experiment = Experiment.builder()
            .experimentId(1L)
            .started(Timestamp.valueOf(LocalDateTime.now()))
            .build();
        Exposure exposure = Exposure.builder()
            .exposureId(1L)
            .experiment(experiment)
            .build();
        Assignment notStartedAssignment = Assignment.builder()
            .assignmentId(1L)
            .exposure(exposure)
            .build();

        ReflectionTestUtils.setField(experimentStartedDateDataRunner, "enabled", true);
        when(assignmentRepository.findAll()).thenReturn(List.of(notStartedAssignment));
        when(experimentRepository.findAll()).thenReturn(List.of(experiment));

        experimentStartedDateDataRunner.onApplicationEvent(event);

        // work happens on a spawned background thread, so a bare verify() can run before it completes; timeout() polls until it lands.
        verify(experimentRepository, timeout(2000)).save(experiment);
        assertNull(experiment.getStarted());
    }

    @Test
    public void testOnApplicationEventSetsExperimentStartedToEarliestStartedAssignment() {
        Experiment experiment = Experiment.builder()
            .experimentId(2L)
            .build();
        Exposure exposure = Exposure.builder()
            .exposureId(2L)
            .experiment(experiment)
            .build();
        Timestamp earlier = Timestamp.valueOf(LocalDateTime.now().minusDays(1));
        Timestamp later = Timestamp.valueOf(LocalDateTime.now());
        Assignment laterAssignment = Assignment.builder()
            .assignmentId(2L)
            .exposure(exposure)
            .started(later)
            .build();
        Assignment earlierAssignment = Assignment.builder()
            .assignmentId(3L)
            .exposure(exposure)
            .started(earlier)
            .build();

        ReflectionTestUtils.setField(experimentStartedDateDataRunner, "enabled", true);
        // deliberately returned out of chronological order to prove the comparator (not list order) picks the earliest
        when(assignmentRepository.findAll()).thenReturn(List.of(laterAssignment, earlierAssignment));
        when(experimentRepository.findAll()).thenReturn(List.of(experiment));

        experimentStartedDateDataRunner.onApplicationEvent(event);

        verify(experimentRepository, timeout(2000)).save(experiment);
        assertEquals(earlier, experiment.getStarted());
    }

    @Test
    public void testOnApplicationEventNoChangeWhenNeitherConditionApplies() {
        Experiment experiment = Experiment.builder()
            .experimentId(4L)
            .build();
        Exposure exposure = Exposure.builder()
            .exposureId(4L)
            .experiment(experiment)
            .build();
        Assignment notStartedAssignment = Assignment.builder()
            .assignmentId(4L)
            .exposure(exposure)
            .build();

        ReflectionTestUtils.setField(experimentStartedDateDataRunner, "enabled", true);
        when(assignmentRepository.findAll()).thenReturn(List.of(notStartedAssignment));
        when(experimentRepository.findAll()).thenReturn(List.of(experiment));

        experimentStartedDateDataRunner.onApplicationEvent(event);

        // no positive event to synchronize on here, so wait out a bounded window before asserting the negative
        verify(experimentRepository, after(500).never()).save(any());
    }

}
