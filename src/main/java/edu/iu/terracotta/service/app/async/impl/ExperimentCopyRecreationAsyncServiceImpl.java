package edu.iu.terracotta.service.app.async.impl;

import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import edu.iu.terracotta.service.app.async.ExperimentCopyRecreationAsyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyCandidateService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement")
public class ExperimentCopyRecreationAsyncServiceImpl implements ExperimentCopyRecreationAsyncService {

    private final EntityManagerFactory entityManagerFactory;
    private final ExperimentCopyCandidateService experimentCopyCandidateService;

    @Async
    @Override
    public void recreate(long destinationContextId) {
        recreateInSession(destinationContextId, null);
    }

    @Async
    @Override
    public void recreate(long destinationContextId, String actingUserKey) {
        recreateInSession(destinationContextId, actingUserKey);
    }

    private void recreateInSession(long destinationContextId, String actingUserKey) {
        // recreation reuses the same export/import code an instructor's own request runs, which
        // relies on that request's open-in-view session to lazy-load an Experiment's children
        // outside of any transaction. This background thread has no such session, so bind one
        // for the duration, the same way OpenEntityManagerInViewInterceptor does for a request.
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManager));

        try {
            experimentCopyCandidateService.recreateForContext(destinationContextId, actingUserKey);
        } catch (Exception e) {
            log.error("Error recreating copied experiments for destination context ID: [{}]", destinationContextId, e);
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
            EntityManagerFactoryUtils.closeEntityManager(entityManager);
        }
    }

}
