package edu.iu.terracotta.service.app.distribute.impl;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.Roles;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiMembershipRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthServiceManager;
import edu.iu.terracotta.connectors.generic.service.lti.LtiNoticeService;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ObsoleteAssignment;
import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCandidate;
import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCreatedAssignment;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.model.dto.distribute.CopyStatusDto;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.dao.model.distribute.LmsRepointTargets;
import edu.iu.terracotta.dao.model.distribute.RepointPlan;
import edu.iu.terracotta.dao.model.dto.distribute.ImportDto;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyCandidateStatus;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyStatus;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ObsoleteAssignmentRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentCopyCandidateRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentCopyCreatedAssignmentRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentImportRepository;
import edu.iu.terracotta.exceptions.ExperimentCopyCandidateNotFoundException;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyCandidateService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyNotificationService;
import edu.iu.terracotta.service.app.distribute.ExperimentExportService;
import edu.iu.terracotta.service.app.distribute.ExperimentImportService;
import edu.iu.terracotta.utils.LmsAuthorizationUtils;
import edu.iu.terracotta.utils.LmsExternalToolUrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement")
public class ExperimentCopyCandidateServiceImpl implements ExperimentCopyCandidateService {

    // role is an ordinal scale (see Lti3Request.makeUserRoleNum): 0 = general/learner,
    // 1 = instructor, 2 = admin
    private static final int INSTRUCTOR_ROLE = 1;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1024;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    // the launch-triggered retry (DistributeController's /copy-status/retry, called
    // automatically by Home.vue the first time it sees ERROR) is attempted once before the
    // obsolete-assignment process is allowed to run and the failure alert is shown - see
    // hasUnfinishedForContext
    private static final int ATTEMPTS_BEFORE_OBSOLETE_CHECK_RUNS = 2;

    private static final List<ExperimentCopyCandidateStatus> UNFINISHED_STATUSES = List.of(
        ExperimentCopyCandidateStatus.PENDING,
        ExperimentCopyCandidateStatus.IMPORTING
    );

    private static final List<ExperimentCopyCandidateStatus> ERROR_STATUS = List.of(ExperimentCopyCandidateStatus.ERROR);

    // every status a recreation attempt can be in - i.e. not declined through the (now unused)
    // selection prompt
    private static final List<ExperimentCopyCandidateStatus> RECREATION_STATUSES = List.of(
        ExperimentCopyCandidateStatus.PENDING,
        ExperimentCopyCandidateStatus.IMPORTING,
        ExperimentCopyCandidateStatus.IMPORTED,
        ExperimentCopyCandidateStatus.ERROR
    );

    private static final List<ExperimentImportStatus> IMPORT_ERROR_STATUSES = List.of(
        ExperimentImportStatus.ERROR,
        ExperimentImportStatus.ERROR_ACKNOWLEDGED
    );

    private final ExperimentCopyCandidateRepository experimentCopyCandidateRepository;
    private final ExperimentRepository experimentRepository;
    private final AssignmentRepository assignmentRepository;
    private final ObsoleteAssignmentRepository obsoleteAssignmentRepository;
    private final LtiUserRepository ltiUserRepository;
    private final LtiContextRepository ltiContextRepository;
    private final LtiMembershipRepository ltiMembershipRepository;
    private final ExperimentImportRepository experimentImportRepository;
    private final ApiClient apiClient;
    private final LtiNoticeService ltiNoticeService;
    private final FeatureService featureService;
    private final AssignmentService assignmentService;
    private final AssignmentAsyncService assignmentAsyncService;
    private final ExperimentExportService experimentExportService;
    private final ExperimentImportService experimentImportService;
    private final ExperimentCopyNotificationService experimentCopyNotificationService;
    private final ExperimentCopyCreatedAssignmentRepository experimentCopyCreatedAssignmentRepository;
    private final LmsOAuthServiceManager lmsOAuthServiceManager;

    @Override
    public Optional<Long> stageFromNotice(Claims noticeClaims) {
        LtiContextEntity destination = ltiNoticeService.resolveOrCreateContext(noticeClaims).orElse(null);

        if (destination == null) {
            log.warn("Could not resolve or create a destination context for a course-copy notice. Issuer: [{}]", noticeClaims.getIssuer());
            return Optional.empty();
        }

        long platformDeploymentKeyId = destination.getToolDeployment().getPlatformDeployment().getKeyId();

        if (!featureService.isFeatureEnabled(FeatureType.PLATFORM_NOTIFICATIONS, platformDeploymentKeyId)) {
            log.info("Platform notifications feature is disabled for platform deployment ID: [{}] - not staging copy candidates for destination context ID: [{}]", platformDeploymentKeyId, destination.getContextId());
            return Optional.empty();
        }

        List<LtiContextEntity> origins = ltiNoticeService.resolveOriginContexts(noticeClaims);
        List<Experiment> sourceExperiments = origins.stream()
            .flatMap(origin -> experimentRepository.findAllByLtiContextEntity_ContextId(origin.getContextId()).stream())
            .toList();

        log.info(
            "Staging copy candidates for destination context ID: [{}] - resolved [{}] origin context(s) ({}), containing [{}] experiment(s) to consider",
            destination.getContextId(),
            origins.size(),
            origins.stream().map(origin -> String.valueOf(origin.getContextId())).collect(Collectors.joining(", ")),
            sourceExperiments.size()
        );

        long staged = sourceExperiments.stream()
            .filter(experiment -> !experimentCopyCandidateRepository.existsBySourceExperiment_ExperimentIdAndDestinationContext_ContextId(experiment.getExperimentId(), destination.getContextId()))
            .map(experiment ->
                experimentCopyCandidateRepository.save(
                    ExperimentCopyCandidate.builder()
                        .sourceExperiment(experiment)
                        .destinationContext(destination)
                        .status(ExperimentCopyCandidateStatus.PENDING)
                        .build()
                )
            )
            .count();

        log.info("Staged [{}] new copy candidate(s) for destination context ID: [{}]", staged, destination.getContextId());

        return Optional.of(destination.getContextId());
    }

    @Override
    public void recreateForContext(long destinationContextId, String actingUserKey) {
        LtiContextEntity destination = ltiContextRepository.findById(destinationContextId).orElse(null);

        if (destination == null) {
            log.warn("Destination context ID: [{}] not found - not recreating copied experiments", destinationContextId);
            return;
        }

        List<ExperimentCopyCandidate> pending = experimentCopyCandidateRepository
            .findAllByDestinationContext_ContextIdAndStatus(destinationContextId, ExperimentCopyCandidateStatus.PENDING);

        if (pending.isEmpty()) {
            log.info("No pending copy candidates to recreate for destination context ID: [{}]", destinationContextId);
            return;
        }

        log.info("Recreating [{}] copied experiment(s) in destination context ID: [{}]", pending.size(), destinationContextId);

        LtiUserEntity actingUser = actingUserKey != null
            ? ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(actingUserKey, destination.getToolDeployment().getPlatformDeployment().getKeyId())
            : null;
        boolean notifyOnLmsFailure = actingUser == null;

        // candidates copied from different courses can have different instructors, and each one
        // needs its own LMS session and assignment listing - but only once per instructor
        Map<Long, RecreationSession> sessions = new HashMap<>();

        for (ExperimentCopyCandidate candidate : pending) {
            recreateCandidate(candidate, sessions, destination, actingUser, notifyOnLmsFailure, destinationContextId);
        }
    }

    // one candidate's worth of recreateForContext's per-candidate work, pulled out so its several
    // early-exit cases are separate returns instead of continues cluttering the loop above
    private void recreateCandidate(ExperimentCopyCandidate candidate, Map<Long, RecreationSession> sessions, LtiContextEntity destination, LtiUserEntity actingUser, boolean notifyOnLmsFailure, long destinationContextId) {
        try {
            Optional<LtiUserEntity> instructor = actingUser != null ? Optional.of(actingUser) : resolveInstructor(candidate.getSourceExperiment());

            if (instructor.isEmpty()) {
                markError(candidate, "No instructor found in the source course to recreate this experiment as");
                return;
            }

            RecreationSession session = sessions.get(instructor.get().getUserId());

            if (session == null) {
                session = startSession(instructor.get(), destination);
                sessions.put(instructor.get().getUserId(), session);

                if (session.errorMessage() != null && notifyOnLmsFailure) {
                    // nothing can be created or re-pointed in the LMS as this instructor
                    experimentCopyNotificationService.notifyLmsFailure(instructor.get());
                }
            }

            if (session.errorMessage() != null) {
                markError(candidate, session.errorMessage());
                return;
            }

            importCandidate(candidate, session.securedInfo(), session.lmsAssignments(), notifyOnLmsFailure);
            log.info("Recreated copy candidate ID: [{}] in destination context ID: [{}]", candidate.getUuid(), destinationContextId);
        } catch (ObjectOptimisticLockingFailureException _) {
            // a redelivered notice already claimed this candidate on another thread
            log.info("Copy candidate ID: [{}] is already being recreated - skipping", candidate.getUuid());
        } catch (Exception e) {
            // importCandidate already recorded the failure on the candidate
            log.error("Error recreating copy candidate ID: [{}] in destination context ID: [{}]", candidate.getUuid(), destinationContextId, e);
        }
    }

    // the LMS session everything for one instructor runs under: a SecuredInfo standing in for the
    // live launch the import pipeline normally gets its identity from, and the destination
    // course's LMS assignments (used to match copied assignments back to their source). Set
    // errorMessage instead, if the instructor can't act in the destination course at all.
    private record RecreationSession(SecuredInfo securedInfo, List<LmsAssignment> lmsAssignments, String errorMessage) { }

    private RecreationSession startSession(LtiUserEntity instructor, LtiContextEntity destination) {
        Optional<String> lmsCourseId;

        try {
            lmsCourseId = apiClient.getLmsCourseId(instructor, destination);
        } catch (Exception e) {
            log.warn(
                "Could not look up the LMS course for destination context ID: [{}] as user ID: [{}]: {}",
                destination.getContextId(),
                instructor.getUserId(),
                ExceptionUtils.getRootCauseMessage(e)
            );

            return new RecreationSession(null, null, describeLmsFailure(e, "Could not find the copied course in the LMS"));
        }

        if (lmsCourseId.isEmpty()) {
            return new RecreationSession(null, null, "Could not find the copied course in the LMS");
        }

        SecuredInfo securedInfo = SecuredInfo.builder()
            .platformDeploymentId(instructor.getPlatformDeployment().getKeyId())
            .contextId(destination.getContextId())
            .userId(instructor.getUserKey())
            .roles(Roles.INSTRUCTOR_ROLE_LIST)
            .lmsCourseId(lmsCourseId.get())
            .build();

        try {
            return new RecreationSession(securedInfo, assignmentService.getAllAssignmentsForLmsCourse(securedInfo), null);
        } catch (Exception e) {
            log.warn(
                "Could not list LMS assignments for destination context ID: [{}] as user ID: [{}]: {}",
                destination.getContextId(),
                instructor.getUserId(),
                ExceptionUtils.getRootCauseMessage(e)
            );

            return new RecreationSession(null, null, describeLmsFailure(e, "Could not list the copied course's assignments in the LMS"));
        }
    }

    private String describeLmsFailure(Exception e, String fallback) {
        if (LmsAuthorizationUtils.isAuthorizationFailure(e)) {
            return "The source course's instructor has not authorized Terracotta to access the LMS";
        }

        return fallback;
    }

    // the source Experiment's own creator is the instructor whose LMS access built its
    // assignments in the first place; fall back to any instructor in the source course
    private Optional<LtiUserEntity> resolveInstructor(Experiment sourceExperiment) {
        if (sourceExperiment.getCreatedBy() != null) {
            return Optional.of(sourceExperiment.getCreatedBy());
        }

        return ltiMembershipRepository.findFirstByContextAndRoleGreaterThanEqual(sourceExperiment.getLtiContextEntity(), INSTRUCTOR_ROLE)
            .map(LtiMembershipEntity::getUser);
    }

    private void markError(ExperimentCopyCandidate candidate, String errorMessage) {
        log.warn("Could not recreate copy candidate ID: [{}]: {}", candidate.getUuid(), errorMessage);
        candidate.setStatus(ExperimentCopyCandidateStatus.ERROR);
        candidate.setErrorMessage(StringUtils.abbreviate(errorMessage, ERROR_MESSAGE_MAX_LENGTH));
        experimentCopyCandidateRepository.save(candidate);
    }

    @Override
    public boolean resetFailedForRetry(long contextId) {
        boolean reset = false;

        for (ExperimentCopyCandidate candidate : experimentCopyCandidateRepository
            .findAllByDestinationContext_ContextIdAndStatusInAndAcknowledgedAtIsNull(contextId, RECREATION_STATUSES)) {
            Optional<ExperimentImport> experimentImport = findResultingImport(candidate);
            boolean importFailed = experimentImport.map(existing -> IMPORT_ERROR_STATUSES.contains(existing.getStatus())).orElse(false);

            if (candidate.getStatus() != ExperimentCopyCandidateStatus.ERROR && !importFailed) {
                continue;
            }

            // a failed import rolled back everything it created, so it's safe to run again; the
            // failed import itself is acknowledged so it doesn't show up as its own alert
            experimentImport.ifPresent(existing -> {
                existing.setStatus(ExperimentImportStatus.ERROR_ACKNOWLEDGED);
                experimentImportRepository.save(existing);
            });

            candidate.setStatus(ExperimentCopyCandidateStatus.PENDING);
            candidate.setErrorMessage(null);
            candidate.setResultingImportUuid(null);
            experimentCopyCandidateRepository.save(candidate);
            reset = true;
        }

        if (reset) {
            log.info("Reset failed copy candidates for retry in destination context ID: [{}]", contextId);
        }

        return reset;
    }

    @Override
    public boolean hasUnfinishedForContext(long contextId) {
        if (experimentCopyCandidateRepository.existsByDestinationContext_ContextIdAndStatusIn(contextId, UNFINISHED_STATUSES)) {
            return true;
        }

        boolean importStillProcessing = experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(contextId, ExperimentCopyCandidateStatus.IMPORTED).stream()
            .map(this::findResultingImport)
            .flatMap(Optional::stream)
            .anyMatch(experimentImport -> experimentImport.getStatus() == ExperimentImportStatus.PROCESSING);

        if (importStillProcessing) {
            return true;
        }

        // an ERROR'd candidate that hasn't yet had its one launch-triggered retry still counts as
        // unfinished too - the obsolete-assignment process must not run until that retry has
        // actually been attempted, or it would mark a copied assignment obsolete while a fresh
        // recreation attempt is still pending. Once the retry has happened (attempts reaches
        // ATTEMPTS_BEFORE_OBSOLETE_CHECK_RUNS) and also ended in ERROR, this returns false, so the
        // caller both shows the failure alert and runs the obsolete-assignment process.
        return experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatusInAndAcknowledgedAtIsNull(contextId, ERROR_STATUS).stream()
            .anyMatch(candidate -> candidate.getAttempts() < ATTEMPTS_BEFORE_OBSOLETE_CHECK_RUNS);
    }

    @Override
    public boolean hasFailedForContext(long contextId) {
        return !experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatusInAndAcknowledgedAtIsNull(contextId, ERROR_STATUS).isEmpty();
    }

    @Override
    public boolean hasLmsAuthorization(SecuredInfo securedInfo) {
        LtiUserEntity user = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        if (user == null) {
            return false;
        }

        try {
            LmsOAuthService<?> lmsOAuthService = lmsOAuthServiceManager.getLmsOAuthService(user.getPlatformDeployment());

            // without LMS OAuth configured there's nothing to authorize, so don't hold the retry
            return !lmsOAuthService.isConfigured(user.getPlatformDeployment()) || lmsOAuthService.isAccessTokenAvailable(user);
        } catch (TerracottaConnectorException e) {
            log.warn("Could not look up the LMS OAuth service for user ID: [{}] - not holding the copy retry for authorization", user.getUserId(), e);

            return true;
        }
    }

    @Override
    public CopyStatusDto getCopyStatus(SecuredInfo securedInfo) {
        List<ExperimentCopyCandidate> candidates = experimentCopyCandidateRepository
            .findAllByDestinationContext_ContextIdAndStatusInAndAcknowledgedAtIsNull(securedInfo.getContextId(), RECREATION_STATUSES);

        if (candidates.isEmpty()) {
            return CopyStatusDto.builder()
                .status(ExperimentCopyStatus.NONE)
                .importIds(List.of())
                .build();
        }

        Map<ExperimentCopyCandidate, Optional<ExperimentImport>> imports = new HashMap<>();
        candidates.forEach(candidate -> imports.put(candidate, findResultingImport(candidate)));

        boolean inProgress = candidates.stream()
            .anyMatch(
                candidate -> UNFINISHED_STATUSES.contains(candidate.getStatus())
                    || imports.get(candidate).map(experimentImport -> experimentImport.getStatus() == ExperimentImportStatus.PROCESSING).orElse(false)
            );
        boolean error = candidates.stream()
            .anyMatch(
                candidate -> candidate.getStatus() == ExperimentCopyCandidateStatus.ERROR
                    || imports.get(candidate).map(experimentImport -> IMPORT_ERROR_STATUSES.contains(experimentImport.getStatus())).orElse(false)
            );

        ExperimentCopyStatus status = ExperimentCopyStatus.COMPLETE;

        if (inProgress) {
            status = ExperimentCopyStatus.IN_PROGRESS;
        } else if (error) {
            status = ExperimentCopyStatus.ERROR;
        }

        String sourceCourseTitle = candidates.stream()
            .max(Comparator.comparing(ExperimentCopyCandidate::getCreatedAt))
            .map(candidate -> candidate.getSourceExperiment().getLtiContextEntity().getTitle())
            .orElse(null);

        return CopyStatusDto.builder()
            .status(status)
            .sourceCourseTitle(sourceCourseTitle)
            .importIds(
                candidates.stream()
                    .map(ExperimentCopyCandidate::getResultingImportUuid)
                    .filter(Objects::nonNull)
                    .toList()
            )
            .build();
    }

    @Override
    public void acknowledgeCopyStatus(SecuredInfo securedInfo) {
        if (getCopyStatus(securedInfo).getStatus() == ExperimentCopyStatus.IN_PROGRESS) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());
        boolean acknowledgedAny = false;

        for (ExperimentCopyCandidate candidate : experimentCopyCandidateRepository
            .findAllByDestinationContext_ContextIdAndStatusInAndAcknowledgedAtIsNull(securedInfo.getContextId(), RECREATION_STATUSES)) {
            // the instructor was shown one combined message for these imports instead of each
            // one's own alert - acknowledge them too, so those alerts don't show up later and
            // the import cleanup job can remove their files
            findResultingImport(candidate).ifPresent(experimentImport -> {
                if (experimentImport.getStatus() == ExperimentImportStatus.COMPLETE) {
                    experimentImport.setStatus(ExperimentImportStatus.COMPLETE_ACKNOWLEDGED);
                    experimentImportRepository.save(experimentImport);
                } else if (experimentImport.getStatus() == ExperimentImportStatus.ERROR) {
                    experimentImport.setStatus(ExperimentImportStatus.ERROR_ACKNOWLEDGED);
                    experimentImportRepository.save(experimentImport);
                }
            });

            candidate.setAcknowledgedAt(now);
            experimentCopyCandidateRepository.save(candidate);
            acknowledgedAny = true;
        }

        // the instructor has now seen the final outcome (success, or a failure that already had
        // its one retry), so the obsolete-assignment check hasUnfinishedForContext was holding back
        // can run right away instead of waiting for their next launch
        if (acknowledgedAny && !hasUnfinishedForContext(securedInfo.getContextId())) {
            try {
                assignmentAsyncService.handleAssignmentTasksInLmsByContext(securedInfo);
            } catch (Exception e) {
                log.error("Error running obsolete-assignment check after acknowledging copy status for context ID: [{}]", securedInfo.getContextId(), e);
            }
        }
    }

    private Optional<ExperimentImport> findResultingImport(ExperimentCopyCandidate candidate) {
        if (candidate.getResultingImportUuid() == null) {
            return Optional.empty();
        }

        return experimentImportRepository.findByUuid(candidate.getResultingImportUuid());
    }

    private ImportDto importCandidate(ExperimentCopyCandidate candidate, SecuredInfo securedInfo, List<LmsAssignment> lmsAssignments, boolean notifyOwnerOnLmsFailure) throws ExperimentCopyCandidateNotFoundException, ExperimentImportException {
        Experiment sourceExperiment = experimentRepository.findByExperimentId(candidate.getSourceExperiment().getExperimentId());

        if (sourceExperiment == null) {
            // belt-and-suspenders: the source_experiment_id FK's ON DELETE CASCADE should already
            // have removed this row when the source Experiment was deleted - this only covers a
            // race between that deletion and this request
            experimentCopyCandidateRepository.delete(candidate);
            throw new ExperimentCopyCandidateNotFoundException(String.format("Experiment copy candidate with ID: [%s] no longer has a source experiment", candidate.getUuid()));
        }

        candidate.setStatus(ExperimentCopyCandidateStatus.IMPORTING);
        candidate.setAttempts(candidate.getAttempts() + 1);
        experimentCopyCandidateRepository.save(candidate);

        try {
            removeLmsAssignmentsFromEarlierAttempt(candidate, securedInfo);
            LmsRepointTargets repointTargets = repointTargets(candidate, sourceExperiment, lmsAssignments, securedInfo);
            ExportDto exportDto = experimentExportService.export(sourceExperiment);
            ImportDto importDto = experimentImportService.preprocessFromFile(exportDto.getFile(), exportDto.getFilename(), securedInfo, repointTargets, notifyOwnerOnLmsFailure);

            candidate.setStatus(ExperimentCopyCandidateStatus.IMPORTED);
            candidate.setResultingImportUuid(importDto.getId());
            experimentCopyCandidateRepository.save(candidate);

            return importDto;
        } catch (ObjectOptimisticLockingFailureException e) {
            // another thread already claimed this candidate concurrently - let recreateCandidate's
            // own catch for this handle it; this isn't a real recreation failure to record
            throw e;
        } catch (Exception e) {
            // catch broadly, not just ExperimentExportException/ExperimentImportException - any
            // unexpected failure here (e.g. a NullPointerException deep in the export/import
            // pipeline) must still mark this candidate ERROR. Without that, the candidate is left
            // sitting in IMPORTING forever, which getCopyStatus treats as still IN_PROGRESS -
            // the "your experiments are being copied" alert would never clear, with no visible
            // error beyond a raw stack trace in the logs.
            candidate.setStatus(ExperimentCopyCandidateStatus.ERROR);
            candidate.setErrorMessage(StringUtils.abbreviate(ExceptionUtils.getRootCauseMessage(e), ERROR_MESSAGE_MAX_LENGTH));
            experimentCopyCandidateRepository.save(candidate);

            throw new ExperimentImportException(String.format("Error recreating experiment from copy candidate ID: [%s]", candidate.getUuid()), e);
        }
    }

    // matches a copied Canvas assignment already sitting in the destination course back to the
    // source experiment's own assignment it was copied from, by parsing the same "assignment="
    // query parameter the obsolete-assignment check already parses (see LmsExternalToolUrlUtils)
    // - Canvas course-copy duplicates external_tool_tag_attributes.url verbatim, so a copied
    // assignment's URL still carries the source course's old assignment ID.
    // an earlier attempt that was interrupted (or failed without cleaning up) may have created LMS
    // assignments whose Terracotta side was then rolled back - remove them, or this attempt would
    // leave the course with duplicates
    private void removeLmsAssignmentsFromEarlierAttempt(ExperimentCopyCandidate candidate, SecuredInfo securedInfo) {
        if (candidate.getId() == null) {
            return;
        }

        List<ExperimentCopyCreatedAssignment> created = experimentCopyCreatedAssignmentRepository.findAllByCopyCandidate_Id(candidate.getId());

        if (created.isEmpty()) {
            return;
        }

        LtiUserEntity apiUser = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        for (ExperimentCopyCreatedAssignment createdAssignment : created) {
            try {
                apiClient.deleteAssignmentInLms(LmsAssignment.builder().id(createdAssignment.getLmsAssignmentId()).build(), securedInfo.getLmsCourseId(), apiUser);
            } catch (Exception e) {
                // most likely already gone - the failed attempt's own rollback deletes what it
                // created when it gets the chance to
                log.warn(
                    "Could not remove LMS assignment ID: [{}] left by an earlier attempt of copy candidate ID: [{}]: {}",
                    createdAssignment.getLmsAssignmentId(),
                    candidate.getUuid(),
                    ExceptionUtils.getRootCauseMessage(e)
                );
            }
        }

        experimentCopyCreatedAssignmentRepository.deleteAll(created);
        log.info("Removed [{}] LMS assignment(s) left by an earlier attempt of copy candidate ID: [{}]", created.size(), candidate.getUuid());
    }

    // the first attempt matches copied LMS assignments by launch URL and saves the result; later
    // attempts reuse it, since an interrupted attempt may already have changed those URLs to
    // point at assignments it then rolled back
    private LmsRepointTargets repointTargets(ExperimentCopyCandidate candidate, Experiment sourceExperiment, List<LmsAssignment> lmsAssignments, SecuredInfo securedInfo) {
        RepointPlan plan = readRepointPlan(candidate);
        LmsRepointTargets repointTargets;

        if (plan != null) {
            repointTargets = fromRepointPlan(plan, lmsAssignments, candidate);
        } else {
            repointTargets = buildRepointTargets(sourceExperiment, lmsAssignments, securedInfo);
            candidate.setRepointPlan(writeRepointPlan(repointTargets));
            experimentCopyCandidateRepository.save(candidate);
        }

        return repointTargets.toBuilder()
            .copyCandidateId(candidate.getId())
            .build();
    }

    private RepointPlan readRepointPlan(ExperimentCopyCandidate candidate) {
        if (StringUtils.isBlank(candidate.getRepointPlan())) {
            return null;
        }

        try {
            return JSON_MAPPER.readValue(candidate.getRepointPlan(), RepointPlan.class);
        } catch (Exception e) {
            log.warn("Could not read the saved repoint plan for copy candidate ID: [{}] - matching by URL instead", candidate.getUuid(), e);

            return null;
        }
    }

    private String writeRepointPlan(LmsRepointTargets repointTargets) {
        Map<Long, String> assignments = new HashMap<>();
        repointTargets.getAssignments().forEach((sourceAssignmentId, lmsAssignment) -> assignments.put(sourceAssignmentId, lmsAssignment.getId()));

        RepointPlan plan = RepointPlan.builder()
            .assignments(assignments)
            .consentAssignment(repointTargets.getConsentAssignment() != null ? repointTargets.getConsentAssignment().getId() : null)
            .build();

        try {
            return JSON_MAPPER.writeValueAsString(plan);
        } catch (Exception e) {
            log.warn("Could not save the repoint plan", e);

            return null;
        }
    }

    private LmsRepointTargets fromRepointPlan(RepointPlan plan, List<LmsAssignment> lmsAssignments, ExperimentCopyCandidate candidate) {
        Map<String, LmsAssignment> lmsAssignmentsById = new HashMap<>();
        CollectionUtils.emptyIfNull(lmsAssignments).forEach(lmsAssignment -> lmsAssignmentsById.put(lmsAssignment.getId(), lmsAssignment));

        Map<Long, LmsAssignment> assignments = new HashMap<>();
        MapUtils.emptyIfNull(plan.getAssignments()).forEach((sourceAssignmentId, lmsAssignmentId) -> {
            LmsAssignment lmsAssignment = lmsAssignmentsById.get(lmsAssignmentId);

            if (lmsAssignment == null) {
                // deleted from the LMS since - the retry creates a new one instead
                log.info("Copied LMS assignment ID: [{}] from copy candidate ID: [{}]'s repoint plan no longer exists", lmsAssignmentId, candidate.getUuid());
                return;
            }

            assignments.put(sourceAssignmentId, lmsAssignment);
        });

        return LmsRepointTargets.builder()
            .assignments(assignments)
            .consentAssignment(plan.getConsentAssignment() != null ? lmsAssignmentsById.get(plan.getConsentAssignment()) : null)
            .build();
    }

    @Override
    public Set<Long> resetStalledForRecovery(Duration stalledAfter, Duration importStalledAfter, int maxAttempts) {
        Instant now = Instant.now();
        Timestamp stalledBefore = Timestamp.from(now.minus(stalledAfter));
        Timestamp importStalledBefore = Timestamp.from(now.minus(importStalledAfter));

        List<ExperimentCopyCandidate> stalled = new ArrayList<>(experimentCopyCandidateRepository.findAllByStatusInAndUpdatedAtBefore(UNFINISHED_STATUSES, stalledBefore));

        for (ExperimentCopyCandidate candidate : experimentCopyCandidateRepository
            .findAllByStatusAndAcknowledgedAtIsNullAndUpdatedAtBefore(ExperimentCopyCandidateStatus.IMPORTED, importStalledBefore)) {
            Optional<ExperimentImport> experimentImport = findResultingImport(candidate)
                .filter(existing -> existing.getStatus() == ExperimentImportStatus.PROCESSING)
                .filter(existing -> existing.getUpdatedAt() == null || existing.getUpdatedAt().before(importStalledBefore));

            if (experimentImport.isEmpty()) {
                continue;
            }

            // abandoned - if it's somehow still queued, it skips itself when it finally starts
            // (see ExperimentImportAsyncServiceImpl), rather than recreating this experiment twice
            experimentImport.get().setStatus(ExperimentImportStatus.ERROR_ACKNOWLEDGED);
            experimentImportRepository.save(experimentImport.get());
            stalled.add(candidate);
        }

        Set<Long> contextIds = new HashSet<>();
        Map<Long, LtiUserEntity> instructorsToNotify = new HashMap<>();

        for (ExperimentCopyCandidate candidate : stalled) {
            if (candidate.getAttempts() >= maxAttempts) {
                markError(candidate, String.format("Recreation stopped part-way through and was already retried [%s] time(s)", candidate.getAttempts()));
                resolveInstructor(candidate.getSourceExperiment()).ifPresent(instructor -> instructorsToNotify.putIfAbsent(instructor.getUserId(), instructor));
                continue;
            }

            log.info(
                "Recreation of copy candidate ID: [{}] stopped part-way through (status: [{}], attempts: [{}]) - retrying",
                candidate.getUuid(),
                candidate.getStatus(),
                candidate.getAttempts()
            );
            candidate.setStatus(ExperimentCopyCandidateStatus.PENDING);
            candidate.setResultingImportUuid(null);
            candidate.setErrorMessage(null);
            experimentCopyCandidateRepository.save(candidate);
            contextIds.add(candidate.getDestinationContext().getContextId());
        }

        instructorsToNotify.values().forEach(experimentCopyNotificationService::notifyLmsFailure);

        return contextIds;
    }

    private LmsRepointTargets buildRepointTargets(Experiment sourceExperiment, List<LmsAssignment> lmsAssignments, SecuredInfo securedInfo) {
        if (CollectionUtils.isEmpty(lmsAssignments)) {
            return LmsRepointTargets.none();
        }

        List<Long> sourceAssignmentIds = assignmentRepository.findByExposure_Experiment_ExperimentId(sourceExperiment.getExperimentId()).stream()
            .map(Assignment::getAssignmentId)
            .toList();
        boolean consent = ParticipationTypes.CONSENT == sourceExperiment.getParticipationType();

        if (CollectionUtils.isEmpty(sourceAssignmentIds) && !consent) {
            return LmsRepointTargets.none();
        }

        List<String> convertedLmsAssignmentIds = obsoleteAssignmentRepository.findAllByContext_ContextId(securedInfo.getContextId()).stream()
            .map(ObsoleteAssignment::getLmsAssignmentId)
            .toList();

        LtiUserEntity apiUser = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        String localUrl = apiUser.getPlatformDeployment().getLocalUrl();

        Map<Long, LmsAssignment> repointMap = new HashMap<>();
        LmsAssignment consentAssignment = null;

        for (LmsAssignment lmsAssignment : lmsAssignments) {
            if (lmsAssignment.getLmsExternalToolFields() == null
                || convertedLmsAssignmentIds.contains(lmsAssignment.getId())
                || !Strings.CI.contains(lmsAssignment.getLmsExternalToolFields().getUrl(), localUrl)) {
                continue;
            }

            String url = lmsAssignment.getLmsExternalToolFields().getUrl();

            if (consent && consentAssignment == null && isConsentAssignmentFor(url, sourceExperiment)) {
                consentAssignment = lmsAssignment;
            } else {
                LmsExternalToolUrlUtils.extractQueryParam(url, "assignment")
                    .flatMap(this::resolveAssignmentId)
                    .filter(sourceAssignmentIds::contains)
                    .ifPresent(oldAssignmentId -> repointMap.put(oldAssignmentId, lmsAssignment));
            }
        }

        return LmsRepointTargets.builder()
            .assignments(repointMap)
            .consentAssignment(consentAssignment)
            .build();
    }

    // a consent LMS assignment's launch URL has no assignment of its own, just the experiment
    // (e.g. ?consent=true&experiment=<id>) - by numeric id or uuid, depending on when it was made
    private boolean isConsentAssignmentFor(String url, Experiment sourceExperiment) {
        if (!Strings.CI.equals(LmsExternalToolUrlUtils.extractQueryParam(url, "consent").orElse(null), "true")) {
            return false;
        }

        return LmsExternalToolUrlUtils.extractQueryParam(url, "experiment")
            .filter(
                experimentId -> experimentId.equals(String.valueOf(sourceExperiment.getExperimentId()))
                    || (sourceExperiment.getUuid() != null && Strings.CI.equals(experimentId, sourceExperiment.getUuid().toString()))
            )
            .isPresent();
    }

    // the "assignment=" query parameter is a legacy numeric id or a uuid depending on when the
    // LMS-side copy of the URL was created relative to the uuid migration (see
    // resolveExperimentUuid's permanent-dual-format comment in the LMS JWT services) - mirrors
    // AssignmentAsyncServiceImpl's identical isStillLive resolution for the same reason
    private Optional<Long> resolveAssignmentId(String idText) {
        try {
            return assignmentRepository.findIdByUuid(UUID.fromString(idText));
        } catch (IllegalArgumentException _) {
            try {
                return Optional.of(Long.parseLong(idText));
            } catch (NumberFormatException _) {
                return Optional.empty();
            }
        }
    }

}
