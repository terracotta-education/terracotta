package edu.iu.terracotta.runner.apitokencleaner.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiTokenEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.runner.apitokencleaner.model.ApiTokenCleanerScheduleMessage;
import edu.iu.terracotta.runner.apitokencleaner.model.ApiTokenCleanerScheduleResult;

public class ApiTokenCleanerSchedulerServiceImplTest extends BaseTest {

    private static final int EXPIRATION_TTL_DAYS = 30;

    private ApiTokenCleanerSchedulerServiceImpl apiTokenCleanerSchedulerService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        apiTokenCleanerSchedulerService = new ApiTokenCleanerSchedulerServiceImpl(apiTokenRepository);
    }

    @Test
    public void testCleanupNoTokensFound() {
        when(apiTokenRepository.findAllByLmsConnector(LmsConnector.BRIGHTSPACE)).thenReturn(List.of());

        Optional<ApiTokenCleanerScheduleResult> result = apiTokenCleanerSchedulerService.cleanup(EXPIRATION_TTL_DAYS);

        assertTrue(result.isEmpty());
        verify(apiTokenRepository, never()).delete(any(ApiTokenEntity.class));
    }

    @Test
    public void testCleanupNoExpiredTokens() {
        ApiTokenEntity notExpiredToken = buildApiToken(1L, Timestamp.valueOf(LocalDateTime.now().plusDays(5)));

        when(apiTokenRepository.findAllByLmsConnector(LmsConnector.BRIGHTSPACE)).thenReturn(List.of(notExpiredToken));

        Optional<ApiTokenCleanerScheduleResult> result = apiTokenCleanerSchedulerService.cleanup(EXPIRATION_TTL_DAYS);

        assertTrue(result.isEmpty());
        verify(apiTokenRepository, never()).delete(any(ApiTokenEntity.class));
    }

    @Test
    public void testCleanupDeletesExpiredTokenSuccessfully() {
        ApiTokenEntity expiredToken = buildApiToken(1L, Timestamp.valueOf(LocalDateTime.now().minusDays(EXPIRATION_TTL_DAYS + 10)));

        when(apiTokenRepository.findAllByLmsConnector(LmsConnector.BRIGHTSPACE)).thenReturn(List.of(expiredToken));

        Optional<ApiTokenCleanerScheduleResult> result = apiTokenCleanerSchedulerService.cleanup(EXPIRATION_TTL_DAYS);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().size());

        ApiTokenCleanerScheduleMessage message = result.get().getProcessed().get(0);

        assertEquals(expiredToken.getAccessToken(), message.getAccessToken());
        assertEquals(expiredToken.getRefreshToken(), message.getRefreshToken());
        assertEquals(expiredToken.getExpiresAt(), message.getExpiresAt());
        assertEquals(expiredToken.getTokenId(), message.getId());
        assertEquals(expiredToken.getLmsConnector(), message.getLmsConnector());
        assertEquals(expiredToken.getLmsUserName(), message.getLmsUserName());
        assertEquals(expiredToken.getUser().getUserId(), message.getUserId());
        assertNotNull(message.getDeletedAt());
        assertNull(message.getErrors());

        verify(apiTokenRepository).delete(expiredToken);
    }

    @Test
    public void testCleanupCapturesErrorWhenDeleteThrows() {
        ApiTokenEntity expiredToken = buildApiToken(1L, Timestamp.valueOf(LocalDateTime.now().minusDays(EXPIRATION_TTL_DAYS + 10)));
        String errorMessage = "delete failed";

        when(apiTokenRepository.findAllByLmsConnector(LmsConnector.BRIGHTSPACE)).thenReturn(List.of(expiredToken));
        doThrow(new RuntimeException(errorMessage)).when(apiTokenRepository).delete(expiredToken);

        Optional<ApiTokenCleanerScheduleResult> result = apiTokenCleanerSchedulerService.cleanup(EXPIRATION_TTL_DAYS);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().size());

        ApiTokenCleanerScheduleMessage message = result.get().getProcessed().get(0);

        assertEquals(List.of(errorMessage), message.getErrors());

        verify(apiTokenRepository).delete(expiredToken);
    }

    @Test
    public void testCleanupProcessesMultipleTokensIndependently() {
        ApiTokenEntity succeeds = buildApiToken(1L, Timestamp.valueOf(LocalDateTime.now().minusDays(EXPIRATION_TTL_DAYS + 10)));
        ApiTokenEntity fails = buildApiToken(2L, Timestamp.valueOf(LocalDateTime.now().minusDays(EXPIRATION_TTL_DAYS + 20)));
        String errorMessage = "delete failed";

        when(apiTokenRepository.findAllByLmsConnector(LmsConnector.BRIGHTSPACE)).thenReturn(List.of(succeeds, fails));
        doThrow(new RuntimeException(errorMessage)).when(apiTokenRepository).delete(fails);

        Optional<ApiTokenCleanerScheduleResult> result = apiTokenCleanerSchedulerService.cleanup(EXPIRATION_TTL_DAYS);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getProcessed().size());

        assertNull(result.get().getProcessed().get(0).getErrors());
        assertEquals(List.of(errorMessage), result.get().getProcessed().get(1).getErrors());

        verify(apiTokenRepository).delete(succeeds);
        verify(apiTokenRepository).delete(fails);
    }

    private ApiTokenEntity buildApiToken(long tokenId, Timestamp expiresAt) {
        LtiUserEntity user = LtiUserEntity.builder()
            .userId(tokenId)
            .build();

        return ApiTokenEntity.builder()
            .tokenId(tokenId)
            .accessToken("access-token-" + tokenId)
            .refreshToken("refresh-token-" + tokenId)
            .expiresAt(expiresAt)
            .lmsUserName("lms-user-" + tokenId)
            .lmsConnector(LmsConnector.BRIGHTSPACE)
            .user(user)
            .build();
    }

}
