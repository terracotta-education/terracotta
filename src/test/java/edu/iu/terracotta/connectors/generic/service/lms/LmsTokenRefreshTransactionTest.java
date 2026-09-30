package edu.iu.terracotta.connectors.generic.service.lms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;

public class LmsTokenRefreshTransactionTest {

    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

    @Test
    public void testRunsTheWorkInANewTransactionAndCommits() throws LmsOAuthException {
        assertEquals("token", LmsTokenRefreshTransaction.run(transactionManager, () -> "token"));

        verify(transactionManager).getTransaction(argThat(definition -> definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(transactionManager).commit(any());
    }

    // a failed refresh may already have written something that must stick (e.g. deleting a token
    // the LMS rejected), so the failure is rethrown after the commit instead of rolling back
    @Test
    public void testAFailureIsCommittedAndThenRethrown() {
        LmsOAuthException failure = new LmsOAuthException("invalid_grant");

        LmsOAuthException thrown = assertThrows(LmsOAuthException.class, () -> LmsTokenRefreshTransaction.run(transactionManager, () -> {
            throw failure;
        }));

        assertSame(failure, thrown);
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
    }

}
