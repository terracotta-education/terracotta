package edu.iu.terracotta.service.app.integrations;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.integrations.IntegrationToken;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.integrations.impl.IntegrationTokenServiceImpl;
import jakarta.persistence.LockModeType;

public class IntegrationTokenServiceImplTest extends BaseTest {

    @InjectMocks private IntegrationTokenServiceImpl integrationTokenService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        clearInvocations(
            integrationTokenRepository,
            integrationToken,
            submission
        );
        setup();

        // entityManager isn't a constructor-injected (final) field, and Mockito's field-injection
        // fallback for @InjectMocks doesn't reliably reach it - wire it explicitly
        ReflectionTestUtils.setField(integrationTokenService, "entityManager", entityManager);

        when(question.getQuestionType()).thenReturn(QuestionTypes.INTEGRATION);
        when(submission.isIntegration()).thenReturn(true);
        when(submission.getIntegrationToken()).thenReturn(integrationToken);
        when(integrationToken.isAlreadyRedeemed()).thenReturn(false);
        when(integrationToken.isExpired(anyInt())).thenReturn(false);
    }

    @Test
    void testCreate() throws IntegrationTokenNotFoundException {
        when(questionRepository.findByAssessment_AssessmentIdAndQuestionId(anyLong(), anyLong())).thenReturn(Optional.of(question));
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));

        integrationTokenService.create(submission, securedInfo);

        verify(integrationTokenRepository).saveAndFlush(any(IntegrationToken.class));
        verify(submission).setIntegrationToken(any(IntegrationToken.class));
    }

    @Test
    void testCreatePreview() throws IntegrationTokenNotFoundException {
        when(questionRepository.findByAssessment_AssessmentIdAndQuestionId(anyLong(), anyLong())).thenReturn(Optional.of(question));
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));

        integrationTokenService.create(submission, securedInfo);

        verify(integrationTokenRepository).saveAndFlush(any(IntegrationToken.class));
        verify(submission).setIntegrationToken(any(IntegrationToken.class));
    }

    @Test
    void testCreateNotIntegration() throws IntegrationTokenNotFoundException {
        when(submission.isIntegration()).thenReturn(false);

        integrationTokenService.create(submission, securedInfo);

        verify(integrationTokenRepository, never()).saveAndFlush(any(IntegrationToken.class));
        verify(submission, never()).setIntegrationToken(any(IntegrationToken.class));
        verify(integrationTokenRepository, never()).deleteById(anyLong());
    }

    @Test
    void testCreateNoIntegrationQuestions() throws IntegrationTokenNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);

        integrationTokenService.create(submission, securedInfo);

        verify(integrationTokenRepository, never()).saveAndFlush(any(IntegrationToken.class));
        verify(submission, never()).setIntegrationToken(any(IntegrationToken.class));
        verify(integrationTokenRepository, never()).deleteById(anyLong());
    }

    @Test
    void testCreateBuildsNewTokenWhenNoneExists() throws IntegrationTokenNotFoundException {
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));
        when(submission.getIntegrationToken()).thenReturn(null);

        integrationTokenService.create(submission, securedInfo);

        verify(integrationTokenRepository).saveAndFlush(any(IntegrationToken.class));
        verify(submission).setIntegrationToken(any(IntegrationToken.class));
    }

    // an existing token means another launch of this same submission may be racing to stamp the
    // same row - it has to be re-read under a row lock first, so the write always targets the
    // row's current version instead of failing its version check as an optimistic-locking error
    @Test
    void testCreateLocksExistingTokenRowBeforeStamping() throws IntegrationTokenNotFoundException {
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));

        integrationTokenService.create(submission, securedInfo);

        verify(entityManager).refresh(integrationToken, LockModeType.PESSIMISTIC_WRITE);
        verify(integrationTokenRepository).saveAndFlush(integrationToken);
        verify(submission).setIntegrationToken(integrationToken);
    }

    @Test
    void testCreateDoesNotLockWhenBuildingANewToken() throws IntegrationTokenNotFoundException {
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));
        when(submission.getIntegrationToken()).thenReturn(null);

        integrationTokenService.create(submission, securedInfo);

        verify(entityManager, never()).refresh(any(), any(LockModeType.class));
    }

    // a failure of the write itself has to propagate: catching it inside the caller's
    // transaction can't recover anything (that transaction is already rollback-only by then),
    // it would only hide the real cause behind a later, opaque commit failure
    @Test
    void testCreateSaveFailurePropagates() {
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));
        when(integrationTokenRepository.saveAndFlush(any(IntegrationToken.class)))
            .thenThrow(new ObjectOptimisticLockingFailureException(IntegrationToken.class, 1L));

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> integrationTokenService.create(submission, securedInfo));
        verify(integrationTokenRepository, never()).findBySubmission_SubmissionId(anyLong());
    }

    @Test
    void testFindByToken() throws IntegrationTokenNotFoundException {
        IntegrationToken ret = integrationTokenService.findByToken("token");

        assertNotNull(ret);
    }

    @Test
    public void testFindByTokenBlankTokenIntegrationTokenNotFoundException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        assertThrows(IntegrationTokenNotFoundException.class, () -> { integrationTokenService.findByToken(""); });
    }

    @Test
    public void testFindByTokenIntegrationTokenNotFoundException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(integrationTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThrows(IntegrationTokenNotFoundException.class, () -> { integrationTokenService.findByToken("token"); });
    }

    @Test
    void testRedeemToken() throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenAlreadyRedeemedException, IntegrationTokenExpiredException {
        IntegrationToken ret = integrationTokenService.redeemToken("token");

        assertNotNull(ret);
        verify(integrationToken).setRedeemedAt(any(Timestamp.class));
        verify(integrationTokenRepository).saveAndFlush(any(IntegrationToken.class));
    }

    @Test
    public void testRedeemTokenDataServiceException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(integrationTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThrows(DataServiceException.class, () -> { integrationTokenService.redeemToken(""); });
    }

    // a double-click (or a slow response retried) on an external integration site's submit
    // button - which Terracotta has no control over and can't debounce - sends two concurrent
    // redemption attempts for the same token. The row lock is what makes this safe: it's
    // acquired before the already-redeemed check runs, so a second concurrent call blocks until
    // the first's transaction commits, instead of both reading "not yet redeemed" and both
    // proceeding to score the same submission.
    @Test
    public void testRedeemTokenLocksRowBeforeCheckingRedeemedState() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException, IntegrationTokenAlreadyRedeemedException, IntegrationTokenExpiredException {
        integrationTokenService.redeemToken("token");

        verify(entityManager).refresh(integrationToken, LockModeType.PESSIMISTIC_WRITE);
    }

    // not re-invalidated: setRedeemedAt() again here would overwrite the original redemption's
    // timestamp with this duplicate attempt's own, corrupting the value the exception message
    // itself reports (and the value any earlier, successful request's submission was scored
    // under)
    @Test
    public void testredeemTokenIntegrationTokenAlreadyRedeemedException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(integrationToken.isAlreadyRedeemed()).thenReturn(true);

        assertThrows(IntegrationTokenAlreadyRedeemedException.class, () -> { integrationTokenService.redeemToken("token"); });
        verify(entityManager).refresh(integrationToken, LockModeType.PESSIMISTIC_WRITE);
        verify(integrationToken, never()).setRedeemedAt(any(Timestamp.class));
        verify(integrationTokenRepository, never()).saveAndFlush(any(IntegrationToken.class));
    }

    @Test
    public void testRedeemTokenIntegrationTokenExpiredException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(integrationToken.isExpired(anyLong())).thenReturn(true);

        assertThrows(IntegrationTokenExpiredException.class, () -> { integrationTokenService.redeemToken("token"); });
        verify(integrationToken).setRedeemedAt(any(Timestamp.class));
        verify(integrationTokenRepository).saveAndFlush(any(IntegrationToken.class));
    }

}
