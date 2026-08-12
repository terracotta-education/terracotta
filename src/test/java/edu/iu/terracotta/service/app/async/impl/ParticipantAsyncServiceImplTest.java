package edu.iu.terracotta.service.app.async.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchEmailProjection;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetUsersInCourseOptions;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUserBatchWriteService;
import edu.iu.terracotta.dao.entity.projection.LmsParticipantSummary;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.async.LmsUserBatchAsyncService;

public class ParticipantAsyncServiceImplTest extends BaseTest {

    @Mock private LmsUserBatchWriteService lmsUserBatchWriteService;
    @Mock private LmsUserBatchRepository lmsUserBatchRepository;
    @Mock private LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
    @Mock private LmsUserBatchAsyncService lmsUserBatchAsyncService;
    @Mock private FeatureService featureService;

    private ParticipantAsyncServiceImpl participantAsyncService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        participantAsyncService = new ParticipantAsyncServiceImpl(
            lmsUserBatchWriteService,
            lmsUserBatchRepository,
            lmsUserBatchProcessingRepository,
            ltiContextRepository,
            ltiUserRepository,
            participantRepository,
            apiClient,
            featureService,
            lmsUserBatchAsyncService,
            lmsUtils,
            participantService
        );
        ReflectionTestUtils.setField(participantAsyncService, "entityManager", entityManager);
        ReflectionTestUtils.setField(participantAsyncService, "batchSize", 500);
        ReflectionTestUtils.setField(participantAsyncService, "messagingSyncDebounceSeconds", 300L);

        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, 1L)).thenReturn(true);
        when(participant.getId()).thenReturn(1L);
        // most tests exercise the actual sync happening - only the dedicated
        // "nothing missing"/"recent attempt" tests below stub this away from the default
        when(participantRepository.existsLmsParticipantSummaryToUpdateByContextId(anyLong())).thenReturn(true);
    }

    private LmsParticipantSummary summary(long id, String email) {
        LmsParticipantSummary summary = mock(LmsParticipantSummary.class);
        when(summary.getId()).thenReturn(id);
        when(summary.getEmail()).thenReturn(email);

        return summary;
    }

    @Test
    public void testUpdateParticipantDataContextNotFound() throws Exception {
        when(ltiContextRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> participantAsyncService.updateParticipantData(securedInfo));

        verify(featureService, never()).isFeatureEnabled(any(), anyLong());
        verify(apiClient, never()).listUsersForCourse(any(), any());
    }

    @Test
    public void testUpdateParticipantDataMessagingDisabled() throws Exception {
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, 1L)).thenReturn(false);

        assertDoesNotThrow(() -> participantAsyncService.updateParticipantData(securedInfo));

        verify(ltiUserRepository, never()).findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong());
        verify(apiClient, never()).listUsersForCourse(any(), any());
    }

    @Test
    public void testUpdateParticipantDataLtiUserNotFound() throws Exception {
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(null);

        assertDoesNotThrow(() -> participantAsyncService.updateParticipantData(securedInfo));

        verify(apiClient, never()).listUsersForCourse(any(), any());
    }

    // this runs on every Home.vue load via getExperiments' hardcoded syncWithLms=true, with no
    // throttle of its own - so it must skip the expensive LMS course-membership fetch (and the
    // LmsUserBatchProcessing row it would otherwise create) whenever nothing actually needs it.
    @Test
    public void testUpdateParticipantDataSkipsWhenNoLmsUserIdsMissing() throws Exception {
        when(participantRepository.existsLmsParticipantSummaryToUpdateByContextId(anyLong())).thenReturn(false);

        assertDoesNotThrow(() -> participantAsyncService.updateParticipantData(securedInfo));

        verify(ltiUserRepository, never()).findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong());
        verify(apiClient, never()).listUsersForCourse(any(), any());
    }

    @Test
    public void testUpdateParticipantDataSkipsWhenRecentSyncAttemptExistsForContext() throws Exception {
        LmsUserBatchProcessing recentAttempt = new LmsUserBatchProcessing();
        recentAttempt.setCreatedAt(Timestamp.from(Instant.now()));
        when(lmsUserBatchProcessingRepository.findFirstByContextIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(recentAttempt));

        assertDoesNotThrow(() -> participantAsyncService.updateParticipantData(securedInfo));

        verify(ltiUserRepository, never()).findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong());
        verify(apiClient, never()).listUsersForCourse(any(), any());
    }

    @Test
    public void testUpdateParticipantDataRunsWhenPriorSyncAttemptOutsideDebounceWindow() throws Exception {
        LmsUserBatchProcessing staleAttempt = new LmsUserBatchProcessing();
        staleAttempt.setCreatedAt(Timestamp.from(Instant.now().minusSeconds(301)));
        when(lmsUserBatchProcessingRepository.findFirstByContextIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(staleAttempt));
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);
        when(participantRepository.findLmsParticipantSummaryToUpdateByContextId(anyLong(), any(Pageable.class))).thenReturn(List.of());

        participantAsyncService.updateParticipantData(securedInfo);

        verify(apiClient).listUsersForCourse(any(), eq(ltiUserEntity));
    }

    @Test
    public void testUpdateParticipantDataNoParticipantsToUpdate() throws Exception {
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);
        when(participantRepository.findLmsParticipantSummaryToUpdateByContextId(anyLong(), any(Pageable.class))).thenReturn(List.of());

        participantAsyncService.updateParticipantData(securedInfo);

        ArgumentCaptor<LmsGetUsersInCourseOptions> optionsCaptor = ArgumentCaptor.forClass(LmsGetUsersInCourseOptions.class);
        verify(apiClient).listUsersForCourse(optionsCaptor.capture(), eq(ltiUserEntity));
        assertEquals(Long.valueOf(securedInfo.getContextId()), optionsCaptor.getValue().getContextId());
        verify(lmsUserBatchAsyncService).processed(any(UUID.class), anyString());
        verify(lmsUserBatchAsyncService, never()).success(any(UUID.class));
    }

    @Test
    public void testUpdateParticipantDataSuccessUpdatesLmsUserIdByEmail() throws Exception {
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);
        LmsParticipantSummary firstPageSummary = summary(1L, EMAIL);
        when(participantRepository.findLmsParticipantSummaryToUpdateByContextId(anyLong(), any(Pageable.class)))
            .thenReturn(List.of(firstPageSummary))
            .thenReturn(List.of());
        when(participantRepository.findAllById(any())).thenReturn(List.of(participant));

        LmsUserBatchEmailProjection batchEmail = mock(LmsUserBatchEmailProjection.class);
        when(batchEmail.getEmail()).thenReturn(EMAIL);
        when(batchEmail.getLmsUserId()).thenReturn("lms-user-1");
        when(lmsUserBatchRepository.findBatchProjectionsByBatchIdAndEmailIn(any(UUID.class), any(), any())).thenReturn(List.of(batchEmail));

        participantAsyncService.updateParticipantData(securedInfo);

        verify(ltiUserEntity).setLmsUserId("lms-user-1");
        verify(ltiUserRepository).save(ltiUserEntity);
        verify(entityManager).flush();
        verify(entityManager).clear();
        verify(lmsUserBatchAsyncService).success(any(UUID.class));
        verify(lmsUserBatchAsyncService, never()).processed(any(UUID.class), anyString());
    }

    @Test
    public void testUpdateParticipantDataNoMatchingEmailSetsLmsUserIdNull() throws Exception {
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);
        LmsParticipantSummary firstPageSummary = summary(1L, EMAIL);
        when(participantRepository.findLmsParticipantSummaryToUpdateByContextId(anyLong(), any(Pageable.class)))
            .thenReturn(List.of(firstPageSummary))
            .thenReturn(List.of());
        when(participantRepository.findAllById(any())).thenReturn(List.of(participant));
        when(lmsUserBatchRepository.findBatchProjectionsByBatchIdAndEmailIn(any(UUID.class), any(), any())).thenReturn(List.of());

        participantAsyncService.updateParticipantData(securedInfo);

        verify(ltiUserEntity).setLmsUserId(null);
        verify(ltiUserRepository).save(ltiUserEntity);
    }

    @Test
    public void testUpdateParticipantDataSummaryWithNoMatchingParticipantIsSkipped() throws Exception {
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);
        LmsParticipantSummary unmatchedSummary = summary(99L, EMAIL);
        when(participantRepository.findLmsParticipantSummaryToUpdateByContextId(anyLong(), any(Pageable.class)))
            .thenReturn(List.of(unmatchedSummary))
            .thenReturn(List.of());
        when(participantRepository.findAllById(any())).thenReturn(List.of());
        when(lmsUserBatchRepository.findBatchProjectionsByBatchIdAndEmailIn(any(UUID.class), any(), any())).thenReturn(List.of());

        assertDoesNotThrow(() -> participantAsyncService.updateParticipantData(securedInfo));

        verify(ltiUserRepository, never()).save(any());
        verify(lmsUserBatchAsyncService).success(any(UUID.class));
    }

    @Test
    public void testUpdateParticipantDataMultiplePagesProcessed() throws Exception {
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);
        LmsParticipantSummary firstPageSummary = summary(1L, EMAIL);
        LmsParticipantSummary secondPageSummary = summary(1L, EMAIL);
        when(participantRepository.findLmsParticipantSummaryToUpdateByContextId(anyLong(), any(Pageable.class)))
            .thenReturn(List.of(firstPageSummary))
            .thenReturn(List.of(secondPageSummary))
            .thenReturn(List.of());
        when(participantRepository.findAllById(any())).thenReturn(List.of(participant));
        when(lmsUserBatchRepository.findBatchProjectionsByBatchIdAndEmailIn(any(UUID.class), any(), any())).thenReturn(List.of());

        participantAsyncService.updateParticipantData(securedInfo);

        verify(participantRepository, times(3)).findLmsParticipantSummaryToUpdateByContextId(anyLong(), any(Pageable.class));
        verify(entityManager, times(2)).flush();
        verify(entityManager, times(2)).clear();
        verify(lmsUserBatchAsyncService).success(any(UUID.class));
    }

    @Test
    public void testPrepareParticipationAsyncMarksCompletedOnSuccess() throws Exception {
        UUID batchId = UUID.randomUUID();

        participantAsyncService.prepareParticipationAsync(1L, securedInfo, batchId);

        verify(participantService).prepareParticipation(1L, securedInfo, batchId);
        verify(lmsUserBatchWriteService).updateStatus(batchId, LmsUserBatchStatus.COMPLETED, null);
    }

    @Test
    public void testPrepareParticipationAsyncMarksFailedOnException() throws Exception {
        UUID batchId = UUID.randomUUID();
        doThrow(new ParticipantNotUpdatedException("boom")).when(participantService).prepareParticipation(1L, securedInfo, batchId);

        assertDoesNotThrow(() -> participantAsyncService.prepareParticipationAsync(1L, securedInfo, batchId));

        verify(lmsUserBatchWriteService).updateStatus(batchId, LmsUserBatchStatus.FAILED, "boom");
    }

}
