package edu.iu.terracotta.connectors.generic.service.lms;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;

/**
 * Runs an LMS API token re-read and refresh in its own short transaction.
 *
 * Saving a refreshed token locks its row until the transaction commits. Inside a caller's long
 * transaction (the @Async LMS assignment sync makes many LMS calls in one) that lock was held for
 * the whole job, and any other request refreshing the same user's token waited until MySQL's lock
 * timeout. Committing here also lets the next caller through the per-user refresh lock read the
 * new token instead of refreshing it again.
 */
public final class LmsTokenRefreshTransaction {

    private LmsTokenRefreshTransaction() { }

    @FunctionalInterface
    public interface TokenWork<T> {
        T run() throws LmsOAuthException;
    }

    /**
     * A refresh failure is carried out of the transaction rather than thrown inside it, so it
     * doesn't roll back anything the refresh did write before failing.
     */
    public static <T> T run(PlatformTransactionManager transactionManager, TokenWork<T> work) throws LmsOAuthException {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        AtomicReference<LmsOAuthException> failure = new AtomicReference<>();

        T result = requiresNew.execute(status -> {
            try {
                return work.run();
            } catch (LmsOAuthException e) {
                failure.set(e);

                return null;
            }
        });

        if (failure.get() != null) {
            throw failure.get();
        }

        return result;
    }

}
