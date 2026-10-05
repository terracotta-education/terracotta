package edu.iu.terracotta.service.app.async.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import edu.iu.terracotta.service.app.distribute.ExperimentCopyCandidateService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class ExperimentCopyRecreationAsyncServiceImplTest {

    @Mock private EntityManagerFactory entityManagerFactory;
    @Mock private EntityManager entityManager;
    @Mock private ExperimentCopyCandidateService experimentCopyCandidateService;

    private ExperimentCopyRecreationAsyncServiceImpl experimentCopyRecreationAsyncService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.isOpen()).thenReturn(true);

        experimentCopyRecreationAsyncService = new ExperimentCopyRecreationAsyncServiceImpl(entityManagerFactory, experimentCopyCandidateService);
    }

    @Test
    public void testRecreateRunsWithABoundEntityManagerAndReleasesItAfterwards() {
        AtomicBoolean boundDuringRecreation = new AtomicBoolean(false);
        doAnswer(invocation -> {
            boundDuringRecreation.set(TransactionSynchronizationManager.hasResource(entityManagerFactory));
            return null;
        }).when(experimentCopyCandidateService).recreateForContext(7L, null);

        experimentCopyRecreationAsyncService.recreate(7L);

        assertTrue(boundDuringRecreation.get());
        assertFalse(TransactionSynchronizationManager.hasResource(entityManagerFactory));
        verify(entityManager).close();
    }

    @Test
    public void testRecreateAsActingUserPassesThemThrough() {
        experimentCopyRecreationAsyncService.recreate(7L, "retrying-user");

        verify(experimentCopyCandidateService).recreateForContext(7L, "retrying-user");
        verify(entityManager).close();
    }

    @Test
    public void testRecreateFailureIsCaughtAndStillReleasesTheEntityManager() {
        doThrow(new RuntimeException("fail")).when(experimentCopyCandidateService).recreateForContext(7L, null);

        assertDoesNotThrow(() -> experimentCopyRecreationAsyncService.recreate(7L));

        assertFalse(TransactionSynchronizationManager.hasResource(entityManagerFactory));
        verify(entityManager).close();
    }

}
