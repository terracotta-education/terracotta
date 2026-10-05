package edu.iu.terracotta.runner.distribute.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.service.app.async.ExperimentCopyRecreationAsyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyCandidateService;

public class ExperimentCopyRecoverySchedulerServiceImplTest {

    @Mock private ExperimentCopyCandidateService experimentCopyCandidateService;
    @Mock private ExperimentCopyRecreationAsyncService experimentCopyRecreationAsyncService;

    private ExperimentCopyRecoverySchedulerServiceImpl experimentCopyRecoverySchedulerService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        experimentCopyRecoverySchedulerService = new ExperimentCopyRecoverySchedulerServiceImpl(experimentCopyCandidateService, experimentCopyRecreationAsyncService);
        ReflectionTestUtils.setField(experimentCopyRecoverySchedulerService, "stalledMinutes", 15);
        ReflectionTestUtils.setField(experimentCopyRecoverySchedulerService, "importStalledMinutes", 60);
        ReflectionTestUtils.setField(experimentCopyRecoverySchedulerService, "maxAttempts", 3);
    }

    @Test
    void testRecoverRestartsRecreationInEachStalledCourse() {
        when(experimentCopyCandidateService.resetStalledForRecovery(Duration.ofMinutes(15), Duration.ofMinutes(60), 3)).thenReturn(Set.of(7L, 8L));

        assertEquals(2, experimentCopyRecoverySchedulerService.recover());

        verify(experimentCopyRecreationAsyncService).recreate(7L);
        verify(experimentCopyRecreationAsyncService).recreate(8L);
    }

    @Test
    void testRecoverNothingStalled() {
        when(experimentCopyCandidateService.resetStalledForRecovery(Duration.ofMinutes(15), Duration.ofMinutes(60), 3)).thenReturn(Set.of());

        assertEquals(0, experimentCopyRecoverySchedulerService.recover());

        verify(experimentCopyRecreationAsyncService, never()).recreate(anyLong());
    }

}
