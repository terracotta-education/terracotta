package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageAgsService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageMembershipService;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.LmsUserBatchStatusDto;
import edu.iu.terracotta.dao.model.dto.ParticipantDto;
import edu.iu.terracotta.dao.model.dto.UserDto;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConsentDocumentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.GroupRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.ParticipantAlreadyStartedException;
import edu.iu.terracotta.service.app.GroupParticipantService;
import edu.iu.terracotta.service.app.ParticipantRosterWriteService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.async.LmsUserBatchAsyncService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.ParticipantConsentUtils;
import edu.iu.terracotta.utils.TextConstants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.LooseCoupling", "PMD.UselessParentheses", "PMD.GuardLogStatement", "PMD.PreserveStackTrace", "squid:S112", "squid:S1066"})
public class ParticipantServiceImpl implements ParticipantService {

    private final AssignmentRepository assignmentRepository;
    private final ConsentDocumentRepository consentDocumentRepository;
    private final ExperimentRepository experimentRepository;
    private final GroupRepository groupRepository;
    private final LmsUserBatchRepository lmsUserBatchRepository;
    private final LtiUserRepository ltiUserRepository;
    private final ParticipantRepository participantRepository;
    private final SubmissionRepository submissionRepository;
    private final TreatmentRepository treatmentRepository;
    private final AdvantageAgsService advantageAgsService;
    private final AdvantageMembershipService advantageMembershipService;
    private final ApiJwtService apiJwtService;
    private final ApiClient apiClient;
    private final GroupParticipantService groupParticipantService;
    private final LmsUserBatchAsyncService lmsUserBatchAsyncService;
    private final LtiDataService ltiDataService;
    private final LtiContextRepository ltiContextRepository;
    private final LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
    private final ParticipantRosterWriteService participantRosterWriteService;

    @PersistenceContext private EntityManager entityManager;

    @Value("${app.participant.batch.size:500}")
    private int batchSize;

    @Value("${app.participant.refresh.throttle.hours:168}")
    private long refreshThrottleHours;

    @Override
    public List<Participant> findAllByExperimentId(long experimentId) {
        return participantRepository.findByExperiment_ExperimentId(experimentId);
    }

    // READ_COMMITTED (rather than MySQL's default REPEATABLE READ): this call chain may lead
    // into refreshParticipants, which writes lms_user_batch via LmsUserBatchWriteService in a
    // REQUIRES_NEW sub-transaction so a huge sync doesn't hold one giant lock for its whole
    // duration. Under REPEATABLE READ, THIS transaction's consistent-read snapshot is fixed as of
    // its first read (here, the very first repository call below) - so a REQUIRES_NEW
    // sub-transaction that commits AFTER that point is invisible to this transaction's later
    // reads, even though the rows are genuinely there (findByBatchId would return nothing for a
    // batchId that demonstrably matches in the DB). READ_COMMITTED gives each statement in this
    // transaction a fresh view of whatever's committed at that moment instead of a frozen one.
    // Self-invocation within this class means only the actual entry point's isolation setting
    // takes effect for a whole call chain, so this same isolation is set on every public method
    // here that can be an entry point into refreshParticipants (see also refreshParticipants,
    // refreshParticipantsIfStale, ensureParticipantExists, prepareParticipation below).
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<ParticipantDto> getParticipants(long experimentId, String userId, boolean student, SecuredInfo securedInfo, boolean refresh) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);
        // retrieve published assignment IDs from LMS
        List<Long> publishedExperimentAssignmentIds = calculatedPublishedAssignmentIds(experimentId, securedInfo.getLmsCourseId(), experiment.getCreatedBy());

        if (!student) {
            if (refresh) {
                // throttled - the manual-participation selection page (the only caller that
                // passes refresh=true) was forcing a full synchronous LMS roster sync on every
                // instructor page load, blocking the request for however long that took (several
                // minutes for a large course) with no throttle at all
                refreshParticipantsIfStale(experimentId);
            }

            int page = 0;
            PageRequest pageRequest = PageRequest.of(page, batchSize);
            List<Participant> participants = participantRepository.findByExperiment_ExperimentId(experimentId, pageRequest);

            List<ParticipantDto> participantDtos = new ArrayList<>();

            while (CollectionUtils.isNotEmpty(participants)) {
                participantDtos.addAll(
                    participants.stream()
                        .filter(participant -> !participant.isTestStudent())
                        .map(participant -> toDto(participant, publishedExperimentAssignmentIds, securedInfo))
                        .toList()
                );

                entityManager.flush();
                entityManager.clear();

                pageRequest = PageRequest.of(++page, batchSize);
                participants = participantRepository.findByExperiment_ExperimentId(experimentId, pageRequest);
            }

            return participantDtos;
        }

        try {
            return Collections.singletonList(
                toDto(
                    participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId),
                    publishedExperimentAssignmentIds,
                    securedInfo
                )
            );
        } catch (NullPointerException ex) {
            // NPE == no participant for this experiment with that userId; return an empty list
            return Collections.emptyList();
        }
    }

    @Override
    public Participant getParticipant(long id, long experimentId, String userId, boolean student) throws InvalidUserException, ParticipantNotMatchingException {
        if (!student) {
            return participantRepository.findById(id)
                .orElseThrow(() -> new ParticipantNotMatchingException(TextConstants.PARTICIPANT_NOT_MATCHING));
        }

        Participant participant = participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);

        if (!participant.getParticipantId().equals(id)) {
            throw new InvalidUserException("Error 146: Students are not authorized to view other participants.");
        }

        return participant;
    }

    @Override
    public ParticipantDto postParticipant(ParticipantDto participantDto, long experimentId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException {
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);
        List<Long> publishedExperimentAssignmentIds = calculatedPublishedAssignmentIds(experimentId, securedInfo.getLmsCourseId(), experiment.getCreatedBy());

        if (participantDto.getParticipantId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        Participant participant;
        participantDto.setExperimentId(experimentId);

        try {
            participant = fromDto(participantDto);
        } catch (DataServiceException e) {
            throw new DataServiceException("Error 105: Unable to create the participant:" + e.getMessage(), e);
        }

        return toDto(participantRepository.save(participant), publishedExperimentAssignmentIds, securedInfo);
    }

    @Override
    public ParticipantDto toDto(Participant participant, SecuredInfo securedInfo) {
        List<Long> publishedExperimentAssignmentIds = calculatedPublishedAssignmentIds(participant.getExperiment().getExperimentId(), securedInfo.getLmsCourseId(), participant.getExperiment().getCreatedBy());

        return toDto(participant, publishedExperimentAssignmentIds, securedInfo);
    }

    @Override
    public ParticipantDto toDto(Participant participant, List<Long> publishedExperimentAssignmentIds, SecuredInfo securedInfo) {
        ParticipantDto participantDto = ParticipantDto.builder()
            .id(participant.getUuid())
            .consent(participant.getConsent())
            .createdAt(participant.getCreatedAt())
            .dateGiven(participant.getDateGiven())
            .dateRevoked(participant.getDateRevoked())
            .dropped(participant.getDropped())
            .experimentId(participant.getExperiment().getExperimentId())
            .source(participant.getSource().name())
            .started(hasParticipantSubmitted(participant, publishedExperimentAssignmentIds))
            .updatedAt(participant.getUpdatedAt())
            .user(userToDTO(participant.getLtiUserEntity()))
            .build();

        participantDto.setParticipantId(participant.getParticipantId());

        if (participant.getGroup() != null) {
            participantDto.setGroupId(participant.getGroup().getGroupId());
        }

        return participantDto;
    }

    private UserDto userToDTO(LtiUserEntity user) {
        UserDto userDto = new UserDto();
        userDto.setUserId(user.getUserId());
        userDto.setUserKey(user.getUserKey());
        userDto.setDisplayName(user.getDisplayName());

        return userDto;
    }

    @Override
    public Participant fromDto(ParticipantDto participantDto) throws DataServiceException {
        Optional<Experiment> experiment = experimentRepository.findById(participantDto.getExperimentId());

        if (experiment.isEmpty()) {
            throw new DataServiceException("The experiment for the participant does not exist");
        }

        Participant participant = Participant.builder()
            .consent(participantDto.getConsent())
            .dateGiven(participantDto.getDateGiven())
            .dateRevoked(participantDto.getDateRevoked())
            .dropped(participantDto.getDropped())
            .experiment(experiment.get())
            .source(ParticipationTypes.valueOf(participantDto.getSource()))
            .build();

        try {
            LtiUserEntity userEntity = ltiUserRepository.findById(participantDto.getUser().getUserId())
                .orElseThrow(() -> new DataServiceException("The user for the participant does not exist"));

            participant.setLtiUserEntity(userEntity);
        } catch (Exception e) {
            throw new DataServiceException("The user for the participant is not valid", e);
        }

        if (participantDto.getGroupId() != null && groupRepository.existsByExperiment_ExperimentIdAndGroupId(experiment.get().getExperimentId(), participantDto.getGroupId())) {
            participant.setGroup(groupRepository.getReferenceById(participantDto.getGroupId()));
        }

        return participant;
    }

    @Override
    public void saveAndFlush(Participant participantToChange) {
        participantRepository.saveAndFlush(participantToChange);
    }

    // see the READ_COMMITTED comment on getParticipants above - only relevant if this method is
    // ever an actual entry point (all current callers reach it via self-invocation from within
    // this same class, where this annotation is bypassed and the real entry point's isolation
    // governs instead)
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void refreshParticipants(long experimentId) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        Instant startTime = Instant.now();

        // We don't want to delete participants if they drop the course, so keep the all participants. But we will need to mark them as
        // dropped if they are not in the course roster.
        UUID batchId = UUID.randomUUID();

        try {
            Optional<Experiment> experiment = experimentRepository.findById(experimentId);

            if (experiment.isEmpty()) {
                throw new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING);
            }

            LtiToken ltiToken = advantageMembershipService.getToken(experiment.get().getPlatformDeployment());
            advantageMembershipService.callMembershipService(ltiToken, experiment.get().getLtiContextEntity(), batchId);

            int page = 0;
            PageRequest pageRequest = PageRequest.of(page, batchSize);
            List<LmsUserBatch> batchUsers = lmsUserBatchRepository.findByBatchId(batchId, pageRequest);

            // each page is synced (matched, created, saved) in its own transaction, independent
            // of this method's own - a huge course roster can take many pages/minutes to fully
            // sync, and holding every page's participant/lti-user writes uncommitted for that
            // whole duration is what previously risked "Lock wait timeout exceeded" against other
            // concurrent writers of those same tables (mirroring the equivalent lms_user_batch fix
            // in CanvasAdvantageMembershipServiceImpl)
            while (CollectionUtils.isNotEmpty(batchUsers)) {
                participantRosterWriteService.syncParticipantsPage(experiment.get(), batchUsers);

                pageRequest = PageRequest.of(++page, batchSize);
                batchUsers = lmsUserBatchRepository.findByBatchId(batchId, pageRequest);
            }

            lmsUserBatchAsyncService.success(batchId);
        } catch (ConnectionException | NoSuchElementException e) {
            lmsUserBatchAsyncService.fail(batchId, e.getMessage());
            throw new ParticipantNotUpdatedException(e.getMessage());
        }

        log.debug("Refreshing participants for experiment ID: [{}] took [{}]", experimentId, Duration.between(startTime, Instant.now()));
    }

    /**
     * Like refreshParticipants, but skips the LMS membership service sync entirely if this
     * experiment's LTI context (course) was already refreshed within the last
     * app.participant.refresh.throttle.hours. Tracked per LTI context, not per experiment,
     * since the LMS roster is a property of the course, and multiple experiments can share one.
     * Intended for callers that need the roster reasonably fresh (to reconcile external LMS
     * submitters against participants) but run repeatedly enough that syncing on every call is
     * wasteful - e.g. viewing a gradebook outcome, or repeated exports.
     *
     * @param experimentId
     */
    // see the READ_COMMITTED comment on getParticipants above - this is a real entry point
    // (OutcomeServiceImpl/ExportServiceImpl call it directly, and prepareParticipation below
    // reaches it via self-invocation)
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void refreshParticipantsIfStale(long experimentId) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);

        if (experiment == null) {
            throw new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING);
        }

        if (isParticipantSyncFresh(experiment.getLtiContextEntity())) {
            return;
        }

        // take a real DB row lock (rather than an in-process-only one) so concurrent callers for
        // the same course (a double-click, a slow request the user retried, or - since this is a
        // DB lock - a concurrent request handled by a different app instance entirely) don't each
        // fire a full roster sync at once. Concurrent bulk inserts into lms_user_batch from
        // separate transactions can otherwise lock each other out entirely ("Lock wait timeout
        // exceeded" was observed in production). Held for this whole method, so nothing else can
        // change last_participant_sync out from under it - a failed refreshParticipants below
        // simply never reaches the write that marks it synced, leaving it correctly untouched.
        LtiContextEntity ltiContextEntity = ltiContextRepository.findByContextIdForUpdate(experiment.getLtiContextEntity().getContextId());

        if (isParticipantSyncFresh(ltiContextEntity)) {
            return;
        }

        refreshParticipants(experimentId);

        ltiContextEntity.setLastParticipantSync(Instant.now());
        ltiContextRepository.save(ltiContextEntity);
    }

    private boolean isParticipantSyncFresh(LtiContextEntity ltiContextEntity) {
        Instant lastSync = ltiContextEntity.getLastParticipantSync();

        return lastSync != null && lastSync.isAfter(Instant.now().minus(Duration.ofHours(refreshThrottleHours)));
    }

    /**
     * If experiment hasn't started and participation type has changed, reset the
     * participant's consent.
     *
     * @param experiment
     * @param participant
     */
    public void resetParticipantConsentIfExperimentNotStarted(Experiment experiment, Participant participant) {
        ParticipantConsentUtils.resetConsentIfExperimentNotStarted(experiment, participant);
        participantRepository.save(participant);
    }

    /**
     * Creates a new participant for an LTI user who has already launched the tool (and so
     * already has an LtiUserEntity created by the LTI launch flow), without requiring a full
     * LMS membership service roster refresh. Returns null if the launch's LtiUserEntity can't
     * be resolved, so the caller can fall back to a full refresh.
     *
     * @param experiment
     * @param securedInfo
     * @return
     */
    private Participant createParticipantFromLaunch(Experiment experiment, SecuredInfo securedInfo) {
        LtiUserEntity ltiUserEntity = ltiDataService.findByUserKeyAndPlatformDeployment(securedInfo.getUserId(), experiment.getPlatformDeployment());

        if (ltiUserEntity == null) {
            return null;
        }

        return buildAndSaveParticipant(ltiUserEntity, experiment);
    }

    private Participant buildAndSaveParticipant(LtiUserEntity ltiUserEntity, Experiment experiment) {
        LtiMembershipEntity ltiMembershipEntity = ltiDataService.findByUserAndContext(ltiUserEntity, experiment.getLtiContextEntity());

        if (ltiMembershipEntity == null) {
            ltiMembershipEntity = new LtiMembershipEntity(experiment.getLtiContextEntity(), ltiUserEntity, LtiStrings.ROLE_STUDENT);
            ltiMembershipEntity = ltiDataService.saveLtiMembershipEntity(ltiMembershipEntity);
        }

        Participant newParticipant = Participant.builder()
            .experiment(experiment)
            .dropped(false)
            .ltiUserEntity(ltiUserEntity)
            .ltiMembershipEntity(ltiMembershipEntity)
            .source(experiment.getParticipationType())
            .build();

        switch (experiment.getParticipationType()) {
            case MANUAL:
                newParticipant.setConsent(null);
                break;
            case CONSENT:
                newParticipant.setConsent(false);
                break;
            case AUTO:
                newParticipant.setConsent(true);
                newParticipant.setDateGiven(Timestamp.from(Instant.now()));
                break;
            default:
        }

        return participantRepository.save(newParticipant);
    }

    // see the READ_COMMITTED comment on getParticipants above - a real entry point
    // (StepsController calls it directly), and its fallback path reaches refreshParticipants via
    // self-invocation
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void ensureParticipantExists(long experimentId, SecuredInfo securedInfo) throws ExperimentNotMatchingException, ParticipantNotUpdatedException, TerracottaConnectorException {
        Participant participant = participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securedInfo.getUserId());

        if (participant != null) {
            return;
        }

        Experiment experiment = experimentRepository.findByExperimentId(experimentId);

        if (experiment == null) {
            throw new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING);
        }

        // brand-new participant: create directly from the current LTI launch instead of
        // syncing the entire course roster just to add one record
        if (createParticipantFromLaunch(experiment, securedInfo) != null) {
            return;
        }

        // launch's LtiUserEntity couldn't be resolved (shouldn't normally happen): fall back
        // to a full roster refresh
        refreshParticipants(experimentId);
    }

    // see the READ_COMMITTED comment on getParticipants above - a real entry point
    // (ParticipantAsyncServiceImpl calls it directly), reaching refreshParticipantsIfStale
    // (and from there refreshParticipants) via self-invocation
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void prepareParticipation(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        // throttled, so picking/re-picking a participation type doesn't always force a full LMS
        // roster sync; still proactively populates the roster (rather than waiting for each
        // student's own lazy-create-on-launch) as long as the last sync isn't recent
        refreshParticipantsIfStale(experimentId);

        // reset consent for participants who already exist - covers both the case where the
        // above was skipped as not-yet-stale, and is a harmless no-op otherwise, since a
        // just-synced participant's source already matches the current participation type
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);

        if (experiment == null) {
            throw new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING);
        }

        int page = 0;
        PageRequest pageRequest = PageRequest.of(page, batchSize);
        List<Participant> participants = participantRepository.findByExperiment_ExperimentId(experimentId, pageRequest);

        while (CollectionUtils.isNotEmpty(participants)) {
            participants.forEach(participant -> resetParticipantConsentIfExperimentNotStarted(experiment, participant));

            entityManager.flush();
            entityManager.clear();

            pageRequest = PageRequest.of(++page, batchSize);
            participants = participantRepository.findByExperiment_ExperimentId(experimentId, pageRequest);
        }
    }

    /**
     * If the LTI context's participant roster isn't due for a sync, prepareParticipation only
     * does a fast, local consent reset anyway (see refreshParticipantsIfStale) - so run it
     * synchronously and report COMPLETED immediately, instead of making the caller wait out a
     * full poll cycle for work that's already done by the time it would first check.
     *
     * Otherwise, creates a tracking record for an async prepareParticipation run and returns its
     * ID immediately, instead of the caller blocking on the LMS roster sync (which can take
     * several minutes for a large course, and would otherwise hold the HTTP request/DB
     * transaction open that whole time). The caller is responsible for actually invoking the
     * async work (see ParticipantAsyncService.prepareParticipationAsync) with the returned
     * batch ID.
     *
     * @param experimentId
     * @param securedInfo
     * @return
     */
    @Override
    @Transactional
    public LmsUserBatchStatusDto startPrepareParticipation(long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);

        if (!isParticipantSyncStale(experiment)) {
            prepareParticipation(experimentId, securedInfo);

            return LmsUserBatchStatusDto.builder()
                .batchId(UUID.randomUUID())
                .status(LmsUserBatchStatus.COMPLETED)
                .build();
        }

        UUID batchId = UUID.randomUUID();

        lmsUserBatchProcessingRepository.save(
            LmsUserBatchProcessing.builder()
                .batchId(batchId)
                .contextId(experiment == null ? null : experiment.getLtiContextEntity().getContextId())
                .status(LmsUserBatchStatus.IN_PROGRESS)
                .build()
        );

        return LmsUserBatchStatusDto.builder()
            .batchId(batchId)
            .status(LmsUserBatchStatus.IN_PROGRESS)
            .build();
    }

    private boolean isParticipantSyncStale(Experiment experiment) {
        if (experiment == null) {
            return true;
        }

        return !isParticipantSyncFresh(experiment.getLtiContextEntity());
    }

    @Override
    public Optional<LmsUserBatchStatusDto> getPrepareParticipationStatus(UUID batchId) {
        return lmsUserBatchProcessingRepository.findByBatchId(batchId)
            .map(
                lmsUserBatchProcessing -> LmsUserBatchStatusDto.builder()
                    .batchId(lmsUserBatchProcessing.getBatchId())
                    .status(lmsUserBatchProcessing.getStatus())
                    .message(lmsUserBatchProcessing.getMessage())
                    .build()
            );
    }

    @Override
    @Transactional
    public List<Participant> changeParticipant(Map<Participant, ParticipantDto> map, Long experimentId, SecuredInfo securedInfo) {
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);
        List<Long> publishedExperimentAssignmentIds = calculatedPublishedAssignmentIds(experimentId, securedInfo.getLmsCourseId(), experiment.getCreatedBy());
        List<Participant> participants = new ArrayList<>();

        for (Map.Entry<Participant, ParticipantDto> entry : map.entrySet()) {
            Participant participantToChange = entry.getKey();
            ParticipantDto participantDto = entry.getValue();

            // If they had consent, and now they don't have, we change the dateRevoked to now.
            // In any other case, we leave the date as it is. Ignoring any value in the PUT
            if (participantToChange.getConsent() != null
                    && (BooleanUtils.isTrue(participantToChange.getConsent()) || (BooleanUtils.isFalse(participantToChange.getConsent()) && participantToChange.getDateRevoked() == null))
                    && BooleanUtils.isNotTrue(participantDto.getConsent())) {
                participantToChange.setDateGiven(null);
                participantToChange.setDateRevoked(Timestamp.valueOf(LocalDateTime.now()));
                participantToChange.setSource(ParticipationTypes.REVOKED);
            }

            // update non-consented to consented; reset revoked date; set source to experiment type
            if (BooleanUtils.isNotTrue(participantToChange.getConsent()) && BooleanUtils.isTrue(participantDto.getConsent())) {
                participantToChange.setDateGiven(Timestamp.valueOf(LocalDateTime.now()));
                participantToChange.setDateRevoked(null);
                participantToChange.setSource(experiment.getParticipationType());
            }

            participantToChange.setConsent(participantDto.getConsent());

            // NOTE: we do this... but this will be updated in the next GET participants with the real data and dropped will be overwritten.
            if (participantDto.getDropped() != null) {
                participantToChange.setDropped(participantDto.getDropped());
            }

            // We don't allow changing the group (manually) once the experiment has started.
            if (!hasParticipantSubmitted(participantToChange, publishedExperimentAssignmentIds)) {
                if (participantDto.getGroupId() != null
                        && groupRepository.existsByExperiment_ExperimentIdAndGroupId(experiment.getExperimentId(), participantDto.getGroupId())) {
                    participantToChange.setGroup(groupRepository.findByGroupId(participantDto.getGroupId()));
                } else {
                    participantToChange.setGroup(null);
                }
            }

            if (participantToChange.getSource() == null) {
                participantToChange.setSource(experiment.getParticipationType());
            }

            participants.add(participantToChange);
        }

        return participantRepository.saveAll(participants);
    }

    @Override
    @Transactional
    public Participant changeConsent(ParticipantDto participantDto, SecuredInfo securedInfo, Long experimentId) throws ParticipantAlreadyStartedException, ExperimentNotMatchingException, ParticipantNotMatchingException {
        Participant participant = participantRepository.findById(participantDto.getParticipantId())
            .orElseThrow(() -> new ParticipantNotMatchingException(TextConstants.PARTICIPANT_NOT_MATCHING));

        if (!Strings.CS.equals(participant.getLtiUserEntity().getUserKey(), securedInfo.getUserId())
                || securedInfo.getConsent() == null
                || BooleanUtils.isFalse(securedInfo.getConsent())) {
            return participant;
        }

        if (BooleanUtils.isTrue(participant.getConsent()) && BooleanUtils.isFalse(participantDto.getConsent())) {
            // user is changing consent from true to false, set source = REVOKED
            participant.setSource(ParticipationTypes.REVOKED);
        }

        // Don't allow changing consent to true if participant has submitted a response and previously not consented
        if (hasParticipantSubmitted(participant, calculatedPublishedAssignmentIds(experimentId, securedInfo.getLmsCourseId(), participant.getExperiment().getCreatedBy()))
                && BooleanUtils.isFalse(participant.getConsent())
                && BooleanUtils.isTrue(participantDto.getConsent())) {
            throw new ParticipantAlreadyStartedException("Participant has already started experiment, consent cannot be changed to given");
        }

        // We only edit the consent here.
        participantDto.setDropped(participant.getDropped());

        if (participant.getGroup() == null) {
            participantDto.setGroupId(null);
        } else {
            participantDto.setGroupId(participant.getGroup().getGroupId());
        }

        List<Participant> changedParticipants = changeParticipant(Collections.singletonMap(participant, participantDto), experimentId, securedInfo);

        // update experiment as started
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);

        if (experiment == null) {
            throw new ExperimentNotMatchingException(String.format("No experiment with ID: '%s' found.", experimentId));
        }

        if (!participant.isTestStudent() && !experiment.isStarted()) {
            experiment.setStarted(Timestamp.valueOf(LocalDateTime.now()));
        }

        return changedParticipants.get(0);
    }

    @Override
    public Participant findParticipant(long experimentId, String userId) {
        int page = 0;
        PageRequest pageRequest = PageRequest.of(page, batchSize);
        List<Participant> participants = participantRepository.findByExperiment_ExperimentId(experimentId, pageRequest);
        Participant found = null;

        while (CollectionUtils.isNotEmpty(participants) && found == null) {
            found = participants.stream()
                .filter(participant -> Strings.CS.equals(participant.getLtiUserEntity().getUserKey(), userId))
                .findFirst()
                .orElse(null);

            entityManager.flush();
            entityManager.clear();
            pageRequest = PageRequest.of(++page, batchSize);
            participants = participantRepository.findByExperiment_ExperimentId(experimentId, pageRequest);
        }

        return found;
    }

    /**
     * Has the participant submitted a response to an assignment?
     *
     * true if:
     *
     * 1. has at least created a submission (viewed) a multi-version assignment
     *
     * false if none of the above and:
     *
     * 1. has only accessed and/or submitted to a single-version assignment
     *
     * @param participant
     * @param securedInfo
     * @return
     */
    private boolean hasParticipantSubmitted(Participant participant, List<Long> publishedExperimentAssignmentIds) {
        // find only published assignment submissions
        List<Submission> publishedSubmissions = submissionRepository.findByParticipant_Id(participant.getParticipantId()).stream()
            .filter(submission -> publishedExperimentAssignmentIds.contains(submission.getAssessment().getTreatment().getAssignment().getAssignmentId()))
            .toList();

        return
            // participant has at least viewed a multi-version assignment; consider it submitted
            CollectionUtils.isNotEmpty(
                publishedSubmissions.stream()
                    .filter(publishedSubmission -> treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(publishedSubmission.getAssessment().getTreatment().getAssignment().getAssignmentId()).size() > 1)
                    .toList()
            );
    }

    @Override
    public List<Long> calculatedPublishedAssignmentIds(long experimentId, String lmsCourseId, LtiUserEntity createdBy) {
        // find only published assignments
        return assignmentRepository.findByExposure_Experiment_ExperimentId(experimentId).stream()
            .filter(
                assignment -> {
                    try {
                        return apiClient.listAssignment(createdBy, lmsCourseId, assignment.getLmsAssignmentId()).get().isPublished();
                    } catch (Exception e) {
                        return false;
                    }
                }
            )
            .map(Assignment::getAssignmentId)
            .toList();
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long participantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/participant/{participantId}")
                .buildAndExpand(experimentId, participantId).toUri());

        return headers;
    }

    private void setConsentToAll(Boolean consent, Long experimentId) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        // apply to participants who already exist, instead of syncing the entire course
        // roster; a brand-new participant already gets the correct initial consent for the
        // current participation type at creation time (see buildAndSaveParticipant)
        int page = 0;
        PageRequest pageRequest = PageRequest.of(page, batchSize);
        List<Participant> participants = participantRepository.findByExperiment_ExperimentId(experimentId, pageRequest);

        while (CollectionUtils.isNotEmpty(participants)) {
            for (Participant participant : participants) {
                participant.setConsent(consent);
                participant.setDateGiven(consent == null ? null : Timestamp.from(Instant.now()));
            }

            participantRepository.saveAll(participants);

            entityManager.flush();
            entityManager.clear();

            pageRequest = PageRequest.of(++page, batchSize);
            participants = participantRepository.findByExperiment_ExperimentId(experimentId, pageRequest);
        }
    }

    @Override
    @Transactional
    public void setAllToNull(Long experimentId) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        setConsentToAll(null, experimentId);
    }

    @Override
    @Transactional
    public void setAllToTrue(Long experimentId) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        setConsentToAll(true, experimentId);
    }

    @Override
    @Transactional
    public void setAllToFalse(Long experimentId) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        setConsentToAll(false, experimentId);
    }

    @Override
    public void postConsentSubmission(Participant participant, SecuredInfo securedInfo) throws ConnectionException, DataServiceException, TerracottaConnectorException {
        // need the assignment and the iss configuration
        PlatformDeployment platformDeployment = participant.getExperiment().getPlatformDeployment();
        Experiment experiment = participant.getExperiment();
        LtiToken ltiTokenScore = advantageAgsService.getToken(LtiAgsScope.SCORES, platformDeployment);
        LtiToken ltiTokenResults = advantageAgsService.getToken(LtiAgsScope.RESULTS, platformDeployment);
        // find the right id to pass based on the assignment
        LtiToken ltiToken = advantageAgsService.getToken(LtiAgsScope.LINEITEMS, experiment.getPlatformDeployment());
        // find the right id to pass based on the assignment
        LineItems lineItems = advantageAgsService.getLineItems(ltiToken, experiment.getLtiContextEntity());

        Optional<LineItem> lineItem = lineItems.getLineItemList().stream()
            .filter(li -> Strings.CS.equals(li.getResourceLinkId(), participant.getExperiment().getConsentDocument().getResourceLinkId()))
            .findFirst();

        if (lineItem.isEmpty()) {
            // if we couldn't find lineitem, try to get the resource link id anew
            // (This is needed because the resourceLinkId for a consent assignment
            // wasn't accurately assigned previously. Eventually this can be removed.)
            lineItem = fixConsentAssignmentResourceLinkId(securedInfo, experiment, lineItems, lineItem);
        }

        if (lineItem.isEmpty()) {
            throw new DataServiceException("Error 136: The assignment is not linked to any LMS assignment");
        }

        Score score = new Score();
        score.setUserId(participant.getLtiUserEntity().getUserKey());
        // Score the consent submission as 100% and let the platform scale the grade to the max number of points.
        score.setScoreGiven(1F);
        score.setScoreMaximum(1F);
        score.setActivityProgress("Completed");
        score.setGradingProgress("FullyGraded");

        Date date = new Date();
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String strDate = dt.format(date);
        score.setTimestamp(strDate);
        advantageAgsService.postScore(ltiTokenScore, ltiTokenResults, experiment.getLtiContextEntity(), lineItem.get().getId(), score);
    }

    /**
     * This is needed because the resourceLinkId for a consent assignment wasn't
     * accurately assigned previous to the fix in TCOTA-430. Eventually this can be
     * removed.
     *
     * @param securedInfo
     * @param platformDeployment
     * @param experiment
     * @param lineItems
     * @param lineItem
     * @return
     * @throws DataServiceException
          * @throws TerracottaConnectorException
          */
         private Optional<LineItem> fixConsentAssignmentResourceLinkId(SecuredInfo securedInfo, Experiment experiment, LineItems lineItems, Optional<LineItem> lineItem)
                 throws DataServiceException, TerracottaConnectorException {

        try {
            log.warn(
                    "Could not find line item for experiment {} consent assignment. Going to use "
                            + "LMS API to try to figure out the right resourceLinkId. This is only "
                            + "for an older issue with setting the resourceLinkId correctly so this "
                            + "should NEVER happen with new experiments.",
                    experiment.getExperimentId());
            LtiUserEntity instructorUser = experiment.getCreatedBy();
            Optional<? extends LmsAssignment> consentAssignment = apiClient.listAssignment(instructorUser, securedInfo.getLmsCourseId(), experiment.getConsentDocument().getLmsAssignmentId());

            if (consentAssignment.isPresent()) {
                String jwtTokenAssignment = consentAssignment.get().getSecureParams();
                String resourceLinkId = apiJwtService.unsecureToken(jwtTokenAssignment, experiment.getPlatformDeployment()).get("lti_assignment_id").toString();
                lineItem = lineItems.getLineItemList().stream()
                    .filter(li -> li.getResourceLinkId()
                    .equals(resourceLinkId))
                    .findFirst();

                // If we now have a lineitem, save it with the consent document
                if (lineItem.isPresent()) {
                    log.info("Updating the resourceLinkId to {} for the consent assignment of experiment {}", resourceLinkId, experiment.getExperimentId());
                    experiment.getConsentDocument().setResourceLinkId(resourceLinkId);
                    consentDocumentRepository.save(experiment.getConsentDocument());
                }
            }
        } catch (ApiException e) {
            throw new DataServiceException("Error 136: The assignment is not linked to any LMS assignment");
        }

        return lineItem;
    }

    @Override
    @Transactional
    public Participant handleExperimentParticipant(Experiment experiment, SecuredInfo securedInfo)
            throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException,
                    ExperimentNotMatchingException, TerracottaConnectorException {
        Participant participant = participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experiment.getExperimentId(), securedInfo.getUserId());

        if (participant == null) {
            // brand-new participant: create directly from the current LTI launch instead of
            // syncing the entire course roster just to add one record
            participant = createParticipantFromLaunch(experiment, securedInfo);

            if (participant == null) {
                // launch's LtiUserEntity couldn't be resolved (shouldn't normally happen):
                // fall back to a full roster refresh
                refreshParticipants(experiment.getExperimentId());
                participant = findParticipant(experiment.getExperimentId(), securedInfo.getUserId());

                if (participant == null) {
                    throw new ParticipantNotMatchingException(TextConstants.PARTICIPANT_NOT_MATCHING);
                }

                // get managed entity
                participant = participantRepository.findById(participant.getId()).get();
            }
        } else if ((BooleanUtils.isTrue(participant.getConsent()) && participant.getGroup() == null)
                || BooleanUtils.isTrue(participant.getDropped())) {
            // an active LTI launch already proves the participant is currently enrolled, so
            // repair their state directly instead of syncing the entire course roster
            resetParticipantConsentIfExperimentNotStarted(experiment, participant);
        }

        if (BooleanUtils.isTrue(participant.getDropped())) {
            participant.setDropped(false);
        }

        // 1. Check if the student has the consent signed. If not, set it as no participant
        handleInitialConsent(experiment, participant, securedInfo);

        // 2. Check if the student is in a group (and if not assign it to the right one if consent is true)
        if (BooleanUtils.isTrue(participant.getConsent()) && participant.getGroup() == null) {
            participant.setGroup(groupParticipantService.nextGroup(experiment));
        }

        return participantRepository.save(participant);
    }

    /**
     * Sets participant consent if any of the following are true:
     *
     * 1. Particpation type is "auto"
     * 2. Experiment is not a single condition
     * 3. Participant has not submitted to an assignment (per conditions in hasParticipantSubmitted method)
     *
     * @param experiment
     * @param participant
     */
    private void handleInitialConsent(Experiment experiment, Participant participant, SecuredInfo securedInfo) {
        List<Long> publishedExperimentAssignmentIds = calculatedPublishedAssignmentIds(experiment.getExperimentId(), securedInfo.getLmsCourseId(), experiment.getCreatedBy());
        if (participant.getConsent() == null || (!participant.getConsent() && participant.getDateRevoked() == null)) {
            if (ParticipationTypes.AUTO.equals(experiment.getParticipationType())) {
                participant.setConsent(true);
                participant.setDateGiven(Timestamp.from(Instant.now()));

                return;
            }

            if (!hasParticipantSubmitted(participant, publishedExperimentAssignmentIds)) {
                // participant has no submissions
                return;
            }

            participant.setConsent(false);
            participant.setDateRevoked(Timestamp.from(Instant.now()));
        }
    }

}
