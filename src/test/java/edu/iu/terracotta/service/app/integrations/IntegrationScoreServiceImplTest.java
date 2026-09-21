package edu.iu.terracotta.service.app.integrations;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.entity.integrations.IntegrationTokenLog;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.integrations.impl.IntegrationScoreServiceImpl;

public class IntegrationScoreServiceImplTest extends BaseTest {

    @InjectMocks private IntegrationScoreServiceImpl integrationScoreService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        clearInvocations(
            integrationTokenService,
            questionSubmissionRepository,
            integrationTokenLogRepository
        );
        setup();

        when(integrationClientRepository.findByPreviewToken(anyString())).thenReturn(Optional.empty());
        when(integrationTokenLogRepository.findByCode(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void testScore() throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        integrationScoreService.score("token", "1", Optional.empty());

        verify(integrationTokenService).redeemToken(anyString());
        verify(questionSubmissionRepository).save(any(QuestionSubmission.class));
        verify(integrationTokenLogRepository).save(any(IntegrationTokenLog.class));
    }

    @Test
    void testScoreNoExistingQuestionSubmissions()
        throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        when(submission.getQuestionSubmissions()).thenReturn(Collections.emptyList());
        when(questionRepository.findByAssessment_AssessmentIdAndQuestionId(anyLong(), anyLong())).thenReturn(Optional.of(question));
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));

        integrationScoreService.score("token", "1", Optional.empty());

        verify(integrationTokenService).redeemToken(anyString());
        verify(questionSubmissionRepository).save(any(QuestionSubmission.class));
        verify(integrationTokenLogRepository).save(any(IntegrationTokenLog.class));
    }

    // these four token-lifecycle exceptions all come from redeemToken() itself, before this
    // request ever touches a submission - so whatever submission the token points to (if any)
    // belongs to a different, earlier request, most commonly the one that actually won a race
    // against a duplicate click on an external integration site's submit button (which
    // Terracotta can't debounce - it doesn't control that page). Deleting it would destroy that
    // other request's legitimate, already-scored submission - see IntegrationTokenServiceImplTest
    // for the row-lock fix that makes "who won the race" well-defined in the first place.
    @Test
    public void testScoreIntegrationTokenInvalidException()
        throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException, IntegrationTokenAlreadyRedeemedException, IntegrationTokenExpiredException {
        when(submissionRepository.findByIntegrationToken_Token(anyString())).thenReturn(Optional.of(submission));
        when(integrationTokenService.redeemToken(anyString())).thenThrow(new IntegrationTokenInvalidException("error"));

        assertThrows(IntegrationTokenInvalidException.class, () -> { integrationScoreService.score("token", "1", Optional.empty()); });
        verify(integrationTokenLogRepository).save(any(IntegrationTokenLog.class));
        verify(submissionService, never()).deleteById(any());
    }

    @Test
    public void testScoreNullIntegrationTokenInvalidException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(submissionRepository.findByIntegrationToken_Token(anyString())).thenReturn(Optional.of(submission));

        assertThrows(IntegrationTokenInvalidException.class, () -> { integrationScoreService.score(null, "1", Optional.empty()); });
        verify(integrationTokenLogRepository).save(any(IntegrationTokenLog.class));
        verify(submissionService, never()).deleteById(any());
    }

    @Test
    public void testScoreIntegrationTokenNotFoundException()
        throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException, IntegrationTokenAlreadyRedeemedException, IntegrationTokenExpiredException {
        when(submissionRepository.findByIntegrationToken_Token(anyString())).thenReturn(Optional.of(submission));
        when(integrationTokenService.redeemToken(anyString())).thenThrow(new IntegrationTokenNotFoundException("error"));

        assertThrows(IntegrationTokenNotFoundException.class, () -> { integrationScoreService.score("token", "1", Optional.empty()); });

        verify(integrationTokenLogRepository).save(any(IntegrationTokenLog.class));
        verify(submissionService, never()).deleteById(any());
    }

    @Test
    public void testScoreIntegrationTokenAlreadyRedeemedException()
        throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException, IntegrationTokenAlreadyRedeemedException, IntegrationTokenExpiredException {
        when(submissionRepository.findByIntegrationToken_Token(anyString())).thenReturn(Optional.of(submission));
        when(integrationTokenService.redeemToken(anyString())).thenThrow(new IntegrationTokenAlreadyRedeemedException("error"));

        assertThrows(IntegrationTokenAlreadyRedeemedException.class, () -> { integrationScoreService.score("token", "1", Optional.empty()); });

        verify(integrationTokenLogRepository).save(any(IntegrationTokenLog.class));
        verify(submissionService, never()).deleteById(any());
    }

    @Test
    public void testScoreIntegrationTokenExpiredException()
        throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException, IntegrationTokenAlreadyRedeemedException, IntegrationTokenExpiredException {
        when(submissionRepository.findByIntegrationToken_Token(anyString())).thenReturn(Optional.of(submission));
        when(integrationTokenService.redeemToken(anyString())).thenThrow(new IntegrationTokenExpiredException("error"));

        assertThrows(IntegrationTokenExpiredException.class, () -> { integrationScoreService.score("token", "1", Optional.empty()); });

        verify(integrationTokenLogRepository).save(any(IntegrationTokenLog.class));
        verify(submissionService, never()).deleteById(any());
    }

    // DataServiceException doesn't get the same treatment as the four exceptions above, even
    // though this particular test triggers it from the mocked redeemToken() call: score()'s own
    // blank-token check (already covered by testScoreNullIntegrationTokenInvalidException)
    // guarantees a non-blank token reaches the real redeemToken() first, so in real traffic this
    // exception only ever comes from mid-processing - see testScoreDataServiceExceptionNoQuestionExists,
    // where deleteById() firing is the behavior actually being pinned
    @Test
    public void testScoreDataServiceException()
        throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException, IntegrationTokenAlreadyRedeemedException, IntegrationTokenExpiredException {
        when(integrationTokenService.redeemToken(anyString())).thenThrow(new DataServiceException("error"));

        assertThrows(DataServiceException.class, () -> { integrationScoreService.score("token", "1", Optional.empty()); });

        verify(integrationTokenLogRepository).save(any(IntegrationTokenLog.class));
    }

    @Test
    public void testScoreInvalidScore() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        assertThrows(RuntimeException.class, () -> { integrationScoreService.score("token", "INVALID_SCORE", Optional.empty()); });

        verify(integrationTokenLogRepository).save(any(IntegrationTokenLog.class));
    }

    @Test
    void testScoreBlankPreview()
        throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        when(integrationClientRepository.findByPreviewToken(anyString())).thenReturn(Optional.of(integrationClient));

        integrationScoreService.score("token", null, Optional.of(INTEGRATION_CLIENT_NAME));

        verify(integrationTokenService, never()).redeemToken(anyString());
        verify(questionSubmissionRepository, never()).save(any(QuestionSubmission.class));
        verify(integrationTokenLogRepository, never()).save(any(IntegrationTokenLog.class));
    }

    @Test
    void testScoreBlank() throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        integrationScoreService.score("token", null, Optional.empty());

        verify(integrationTokenService).redeemToken(anyString());
        verify(questionSubmissionRepository).save(any(QuestionSubmission.class));
        verify(integrationTokenLogRepository).save(any(IntegrationTokenLog.class));
    }

    // a genuine mid-processing failure (redeemToken() already succeeded here) - the broken
    // submission this request was scoring is this request's own, so deleting it is correct,
    // unlike the four token-lifecycle exceptions above where it would belong to someone else
    @Test
    public void testScoreDataServiceExceptionNoQuestionExists() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(submission.getQuestionSubmissions()).thenReturn(Collections.emptyList());
        when(questionRepository.findByAssessment_AssessmentIdAndQuestionId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(submissionRepository.findByIntegrationToken_Token(anyString())).thenReturn(Optional.of(submission));

        assertThrows(DataServiceException.class, () -> { integrationScoreService.score("token", "1", Optional.empty()); });

        verify(integrationTokenLogRepository).save(any(IntegrationTokenLog.class));
        verify(submissionService).deleteById(submission.getSubmissionId());
    }

    @Test
    public void testGetPreviewTokenExists() {
        when(integrationClientRepository.findByPreviewToken(anyString())).thenReturn(Optional.of(integrationClient));

        Optional<String> ret = integrationScoreService.getPreviewTokenClient(INTEGRATION_TOKEN);

        assertTrue(ret.isPresent());
    }

    @Test
    public void testGetPreviewTokenNotExists() {
        Optional<String> ret = integrationScoreService.getPreviewTokenClient(INTEGRATION_TOKEN);

        assertTrue(ret.isEmpty());
    }

    @Test
    public void testGetPreviewTokenNull() {
        Optional<String> ret = integrationScoreService.getPreviewTokenClient(null);

        assertTrue(ret.isEmpty());
    }

}
