package edu.iu.terracotta.service.app.async.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchEmailProjection;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetUsersInCourseOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentState;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentType;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUserBatchWriteService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUtils;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.projection.LmsParticipantSummary;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.async.LmsUserBatchAsyncService;
import edu.iu.terracotta.service.app.async.ParticipantAsyncService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ParticipantAsyncServiceImpl implements ParticipantAsyncService {

    private final LmsUserBatchWriteService lmsUserBatchWriteService;
    private final LmsUserBatchRepository lmsUserBatchRepository;
    private final LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
    private final LtiContextRepository ltiContextRepository;
    private final LtiUserRepository ltiUserRepository;
    private final ParticipantRepository participantRepository;
    private final ApiClient apiClient;
    private final FeatureService featureService;
    private final LmsUserBatchAsyncService lmsUserBatchAsyncService;
    private final LmsUtils lmsUtils;
    private final ParticipantService participantService;

    @PersistenceContext private EntityManager entityManager;

    @Value("${app.participant.batch.size:500}")
    private int batchSize;

    // updateParticipantData runs on every Home.vue load (via ExperimentServiceImpl.getExperiments'
    // hardcoded syncWithLms=true) with no throttle of its own, unlike the rest of the participant
    // roster sync paths - debounce against the most recently attempted sync for the context so
    // back-to-back launches don't each kick off their own full LMS course-membership fetch
    @Value("${app.participant.messaging.sync.debounce.seconds:300}")
    private long messagingSyncDebounceSeconds;

    @Async
    @Override
    @Transactional(rollbackFor = { ApiException.class })
    public void updateParticipantData(SecuredInfo securedInfo) throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        LtiContextEntity ltiContextEntity = ltiContextRepository.findById(securedInfo.getContextId())
            .orElse(null);

        if (ltiContextEntity == null) {
            log.error("LTI Context not found for ID: [{}]", securedInfo.getContextId());
            return;
        }

        if (!featureService.isFeatureEnabled(FeatureType.MESSAGING, ltiContextEntity.getToolDeployment().getPlatformDeployment().getKeyId())) {
            // only update participants if messaging feature is enabled
            return;
        }

        Long missingLmsUserIds = participantRepository.existsLmsParticipantSummaryToUpdateByContextId(securedInfo.getContextId());

        if (missingLmsUserIds == null || missingLmsUserIds == 0) {
            // nothing is actually missing an LMS user ID for this context - skip the full LMS
            // course-membership fetch (and the LmsUserBatchProcessing row it would otherwise
            // create) entirely
            return;
        }

        if (isRecentSyncAttempt(securedInfo.getContextId())) {
            // a sync for this context was already attempted moments ago (e.g. this same
            // instructor reloading the tool) - this call runs on every launch with no throttle
            // of its own, so debounce it here instead of hitting the LMS again immediately
            return;
        }

        LtiUserEntity ltiUserEntity = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        if (ltiUserEntity == null) {
            log.error("LTI User not found for ID: [{}] and platform ID: [{}]", securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
            return;
        }

        UUID batchId = UUID.randomUUID();

        apiClient.listUsersForCourse(
            LmsGetUsersInCourseOptions.builder()
                .batchId(batchId)
                .contextId(ltiContextEntity.getContextId())
                .batchSize(batchSize)
                .enrollmentState(Arrays.asList(EnrollmentState.ACTIVE, EnrollmentState.INVITED))
                .enrollmentType(Arrays.asList(EnrollmentType.STUDENT))
                .lmsCourseId(lmsUtils.parseCourseId(ltiContextEntity.getToolDeployment().getPlatformDeployment(), ltiContextEntity.getContext_memberships_url()))
                .build(),
            ltiUserEntity
        );

        int page = 0;
        List<LmsParticipantSummary> lmsParticipantSummariesToUpdate = participantRepository.findLmsParticipantSummaryToUpdateByContextId(securedInfo.getContextId(), batchSize, (long) page * batchSize);

        if (CollectionUtils.isEmpty(lmsParticipantSummariesToUpdate)) {
            String message = String.format("No participants to update found for LTI Context with ID: [%s]", securedInfo.getContextId());
            log.info(message);
            lmsUserBatchAsyncService.processed(batchId, message);
            return;
        }

        while (CollectionUtils.isNotEmpty(lmsParticipantSummariesToUpdate)) {
            List<Participant> participants = participantRepository.findAllById(
                lmsParticipantSummariesToUpdate.stream()
                    .map(LmsParticipantSummary::getId)
                    .toList()
            );

            List<LmsUserBatchEmailProjection> batchEmails = lmsUserBatchRepository.findBatchProjectionsByBatchIdAndEmailIn(
                batchId,
                participants.stream()
                    .map(p -> p.getLtiUserEntity().getEmail())
                    .toList(),
                PageRequest.of(0, batchSize)
            );

            // Update participants without LTI user IDs based on email matching
            lmsParticipantSummariesToUpdate.stream()
                .forEach(
                    lmsParticipantSummary -> {
                        Optional<Participant> participant = participants.stream()
                            .filter(p -> p.getId().longValue() == lmsParticipantSummary.getId())
                            .findFirst();

                        if (participant.isEmpty()) {
                            log.warn("Participant not found for ID: [{}]", lmsParticipantSummary.getId());
                            return;
                        }

                        String matchedLmsUserId = batchEmails.stream()
                            .filter(batchEmail -> Strings.CI.equals(batchEmail.getEmail(), participant.get().getLtiUserEntity().getEmail()))
                            .findFirst()
                            .map(LmsUserBatchEmailProjection::getLmsUserId)
                            .orElse(null);

                        if (matchedLmsUserId == null) {
                            log.warn("No LMS user ID match found in Canvas response for participant ID: [{}] with email: [{}]", lmsParticipantSummary.getId(), participant.get().getLtiUserEntity().getEmail());
                        }

                        participant.get().getLtiUserEntity().setLmsUserId(matchedLmsUserId);
                        ltiUserRepository.save(participant.get().getLtiUserEntity());
                    }
                );

            // flush and clear each batch
            entityManager.flush();
            entityManager.clear();

            // retrieve next set of participants to update
            page++;
            lmsParticipantSummariesToUpdate = participantRepository.findLmsParticipantSummaryToUpdateByContextId(securedInfo.getContextId(), batchSize, (long) page * batchSize);
        }

        // send the event and delete temporary batch data
        lmsUserBatchAsyncService.success(batchId);
    }

    private boolean isRecentSyncAttempt(long contextId) {
        return lmsUserBatchProcessingRepository.findFirstByContextIdOrderByCreatedAtDesc(contextId)
            .map(LmsUserBatchProcessing::getCreatedAt)
            .map(createdAt -> createdAt.toInstant().isAfter(Instant.now().minus(Duration.ofSeconds(messagingSyncDebounceSeconds))))
            .orElse(false);
    }

    @Async
    @Override
    public void prepareParticipationAsync(long experimentId, SecuredInfo securedInfo, UUID batchId) {
        try {
            participantService.prepareParticipation(experimentId, securedInfo, batchId);
            // updateStatus uses a direct UPDATE that bypasses the entity's optimistic-lock check,
            // so it can't fail here even if refreshParticipants's own completion event (see
            // LmsUserBatchAsyncServiceImpl.handleBatchEvent) races this same batchId - see
            // LmsUserBatchWriteServiceImpl.updateStatus
            lmsUserBatchWriteService.updateStatus(batchId, LmsUserBatchStatus.COMPLETED, null);
        } catch (ParticipantNotUpdatedException | ExperimentNotMatchingException | TerracottaConnectorException | RuntimeException e) {
            log.error("Failed to prepare participation for experiment ID: [{}]", experimentId, e);
            lmsUserBatchWriteService.updateStatus(batchId, LmsUserBatchStatus.FAILED, e.getMessage());
        }
    }

}
