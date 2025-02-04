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

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.app.integrations.IntegrationToken;
import edu.iu.terracotta.service.app.integrations.impl.IntegrationTokenServiceImpl;

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

        when(question.getQuestionType()).thenReturn(QuestionTypes.INTEGRATION);
        when(submission.isIntegration()).thenReturn(true);
        when(submission.getIntegrationTokens()).thenReturn(Collections.singletonList(integrationToken));
        when(integrationToken.isAlreadyRedeemed()).thenReturn(false);
        when(integrationToken.isExpired(anyInt())).thenReturn(false);
    }

    @Test
    void testCreate() {
        when(questionRepository.findByAssessment_AssessmentIdAndQuestionId(anyLong(), anyLong())).thenReturn(Optional.of(question));
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));

        integrationTokenService.create(submission, false, securedInfo);

        verify(integrationTokenRepository).save(any(IntegrationToken.class));
        verify(submission).addIntegrationToken(any(IntegrationToken.class));
        verify(integrationTokenRepository).deleteById(anyLong());
    }

    @Test
    void testCreatePreview() {
        when(questionRepository.findByAssessment_AssessmentIdAndQuestionId(anyLong(), anyLong())).thenReturn(Optional.of(question));
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));

        integrationTokenService.create(submission, true, securedInfo);

        verify(integrationTokenRepository).save(any(IntegrationToken.class));
        verify(submission).addIntegrationToken(any(IntegrationToken.class));
        verify(integrationTokenRepository).deleteById(anyLong());
    }

    @Test
    void testCreateNotIntegration() {
        when(submission.isIntegration()).thenReturn(false);

        integrationTokenService.create(submission, false, securedInfo);

        verify(integrationTokenRepository, never()).save(any(IntegrationToken.class));
        verify(submission, never()).addIntegrationToken(any(IntegrationToken.class));
        verify(integrationTokenRepository, never()).deleteById(anyLong());
    }

    @Test
    void testCreateNoIntegrationQuestions() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);

        integrationTokenService.create(submission, false, securedInfo);

        verify(integrationTokenRepository, never()).save(any(IntegrationToken.class));
        verify(submission, never()).addIntegrationToken(any(IntegrationToken.class));
        verify(integrationTokenRepository, never()).deleteById(anyLong());
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

    @Test
    public void testredeemTokenIntegrationTokenExpiredException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(integrationToken.isExpired(anyInt())).thenReturn(true);

        assertThrows(IntegrationTokenExpiredException.class, () -> { integrationTokenService.redeemToken("token"); });
        verify(integrationToken).setRedeemedAt(any(Timestamp.class));
        verify(integrationTokenRepository).saveAndFlush(any(IntegrationToken.class));
    }

    @Test
    public void testredeemTokenIntegrationTokenAlreadyRedeemedException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(integrationToken.isAlreadyRedeemed()).thenReturn(true);

        assertThrows(IntegrationTokenAlreadyRedeemedException.class, () -> { integrationTokenService.redeemToken("token"); });
        verify(integrationToken).setRedeemedAt(any(Timestamp.class));
        verify(integrationTokenRepository).saveAndFlush(any(IntegrationToken.class));
    }

}
