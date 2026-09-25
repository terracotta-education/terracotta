package edu.iu.terracotta.service.app.distribute.impl;

import java.sql.Timestamp;
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
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.connectors.generic.service.lti.LtiNoticeService;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ObsoleteAssignment;
import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCandidate;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateDto;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateResolutionDto;
import edu.iu.terracotta.dao.model.dto.distribute.CopyStatusDto;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.dao.model.distribute.LmsRepointTargets;
import edu.iu.terracotta.dao.model.dto.distribute.ImportDto;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyCandidateStatus;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyStatus;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ObsoleteAssignmentRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentCopyCandidateRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentImportRepository;
import edu.iu.terracotta.exceptions.ExperimentCopyCandidateNotFoundException;
import edu.iu.terracotta.exceptions.ExperimentExportException;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyCandidateService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyNotificationService;
import edu.iu.terracotta.service.app.distribute.ExperimentExportService;
import edu.iu.terracotta.service.app.distribute.ExperimentImportService;
import edu.iu.terracotta.utils.LmsExternalToolUrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement")
public class ExperimentCopyCandidateServiceImpl implements ExperimentCopyCandidateService {

    // role is an ordinal scale (see Lti3Request.makeUserRoleNum): 0 = general/learner,
    // 1 = instructor, 2 = admin
    private static final int INSTRUCTOR_ROLE = 1;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1024;

    private static final List<ExperimentCopyCandidateStatus> UNFINISHED_STATUSES = List.of(
        ExperimentCopyCandidateStatus.PENDING,
        ExperimentCopyCandidateStatus.IMPORTING
    );

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
    private final ConditionRepository conditionRepository;
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
            try {
                Optional<LtiUserEntity> instructor = actingUser != null ? Optional.of(actingUser) : resolveInstructor(candidate.getSourceExperiment());

                if (instructor.isEmpty()) {
                    markError(candidate, "No instructor found in the source course to recreate this experiment as");
                    continue;
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
                    continue;
                }

                importCandidate(candidate, session.securedInfo(), session.lmsAssignments(), notifyOnLmsFailure);
                log.info("Recreated copy candidate ID: [{}] in destination context ID: [{}]", candidate.getUuid(), destinationContextId);
            } catch (ObjectOptimisticLockingFailureException e) {
                // a redelivered notice already claimed this candidate on another thread
                log.info("Copy candidate ID: [{}] is already being recreated - skipping", candidate.getUuid());
            } catch (Exception e) {
                // importCandidate already recorded the failure on the candidate
                log.error("Error recreating copy candidate ID: [{}] in destination context ID: [{}]", candidate.getUuid(), destinationContextId, e);
            }
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
        if (ExceptionUtils.getRootCause(e) instanceof LmsOAuthException) {
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

        return experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(contextId, ExperimentCopyCandidateStatus.IMPORTED).stream()
            .map(this::findResultingImport)
            .flatMap(Optional::stream)
            .anyMatch(experimentImport -> experimentImport.getStatus() == ExperimentImportStatus.PROCESSING);
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
        }
    }

    private Optional<ExperimentImport> findResultingImport(ExperimentCopyCandidate candidate) {
        if (candidate.getResultingImportUuid() == null) {
            return Optional.empty();
        }

        return experimentImportRepository.findByUuid(candidate.getResultingImportUuid());
    }

    @Override
    public List<CopyCandidateDto> getPendingForContext(SecuredInfo securedInfo) {
        if (!experimentRepository.findAllByLtiContextEntity_ContextId(securedInfo.getContextId()).isEmpty()) {
            // this course already has at least one Experiment of its own by the time of this
            // live launch - don't surface candidates, matching the same "is this course new"
            // gating the rest of the app already applies to the zero-state experience. Any
            // still-PENDING candidates for this context are now stale - they'd never be shown
            // or resolved through the normal flow - so dismiss them instead of leaving them
            // PENDING indefinitely.
            List<ExperimentCopyCandidate> stalePending = experimentCopyCandidateRepository
                .findAllByDestinationContext_ContextIdAndStatus(securedInfo.getContextId(), ExperimentCopyCandidateStatus.PENDING);

            for (ExperimentCopyCandidate candidate : stalePending) {
                candidate.setStatus(ExperimentCopyCandidateStatus.DISMISSED);
                experimentCopyCandidateRepository.save(candidate);
            }

            return List.of();
        }

        List<ExperimentCopyCandidate> pending = experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(securedInfo.getContextId(), ExperimentCopyCandidateStatus.PENDING);

        if (pending.isEmpty()) {
            return List.of();
        }

        // a destination course could theoretically have pending candidates staged from more than
        // one course-copy notice (e.g. copied again from a different course before ever resolving
        // the first prompt) - only surface the most recently staged notice's source course, so the
        // dialog's single "this course was copied from X" heading is never wrong for some of the
        // candidates it lists
        long mostRecentSourceContextId = pending.stream()
            .max(Comparator.comparing(ExperimentCopyCandidate::getCreatedAt))
            .map(candidate -> candidate.getSourceExperiment().getLtiContextEntity().getContextId())
            .orElseThrow();

        return pending.stream()
            .filter(candidate -> candidate.getSourceExperiment().getLtiContextEntity().getContextId() == mostRecentSourceContextId)
            .map(this::toDto)
            .toList();
    }

    @Override
    public CopyCandidateResolutionDto resolve(List<UUID> importCandidateIds, SecuredInfo securedInfo) {
        List<ExperimentCopyCandidate> pending = experimentCopyCandidateRepository
            .findAllByDestinationContext_ContextIdAndStatus(securedInfo.getContextId(), ExperimentCopyCandidateStatus.PENDING);

        // "declined" is computed here, against the live PENDING set - not trusted from the
        // caller's own idea of what was shown, which could theoretically be stale
        Set<UUID> toImportIds = new HashSet<>(CollectionUtils.emptyIfNull(importCandidateIds));
        List<ExperimentCopyCandidate> toImport = pending.stream().filter(candidate -> toImportIds.contains(candidate.getUuid())).toList();
        List<ExperimentCopyCandidate> toDecline = pending.stream().filter(candidate -> !toImportIds.contains(candidate.getUuid())).toList();

        List<LmsAssignment> lmsAssignments = List.of();

        if (CollectionUtils.isNotEmpty(toImport)) {
            try {
                lmsAssignments = assignmentService.getAllAssignmentsForLmsCourse(securedInfo);
            } catch (Exception e) {
                log.error(
                    "Error listing LMS assignments for context ID: [{}] while resolving copy candidates - importing without re-pointing any existing assignments",
                    securedInfo.getContextId(),
                    e
                );
            }
        }

        List<ImportDto> imports = new ArrayList<>();

        for (ExperimentCopyCandidate candidate : toImport) {
            try {
                imports.add(importCandidate(candidate, securedInfo, lmsAssignments, false));
            } catch (ExperimentCopyCandidateNotFoundException | ExperimentImportException e) {
                log.error("Error importing copy candidate ID: [{}] during bulk resolve", candidate.getUuid(), e);
            }
        }

        List<UUID> declinedIds = new ArrayList<>();

        // an empty selection means every pending candidate was declined at once (e.g. "No thank
        // you"), not left out of some other choice - anything else is a candidate that simply
        // wasn't part of an otherwise non-empty selection
        ExperimentCopyCandidateStatus declineStatus = toImportIds.isEmpty()
            ? ExperimentCopyCandidateStatus.DISMISSED
            : ExperimentCopyCandidateStatus.NOT_SELECTED;

        for (ExperimentCopyCandidate candidate : toDecline) {
            candidate.setStatus(declineStatus);
            experimentCopyCandidateRepository.save(candidate);
            declinedIds.add(candidate.getUuid());
        }

        // safe now, regardless of import/decline outcomes above: repointed assignments' URLs
        // already carry this context's new IDs (won't be flagged obsolete); declined candidates'
        // stale copied-assignment URLs still carry the old (source) context's IDs and correctly
        // WILL be flagged - see NoticeController/ExperimentServiceImpl for the other half of this
        // (both suppress this same call while any candidate for this context is still PENDING).
        try {
            assignmentAsyncService.handleAssignmentTasksInLmsByContext(securedInfo);
        } catch (Exception e) {
            log.error("Error running obsolete-assignment check after resolving copy candidates for context ID: [{}]", securedInfo.getContextId(), e);
        }

        return CopyCandidateResolutionDto.builder()
            .imports(imports)
            .declinedCandidateIds(declinedIds)
            .build();
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
        experimentCopyCandidateRepository.save(candidate);

        try {
            LmsRepointTargets repointTargets = buildRepointTargets(sourceExperiment, lmsAssignments, securedInfo);
            ExportDto exportDto = experimentExportService.export(sourceExperiment);
            ImportDto importDto = experimentImportService.preprocessFromFile(exportDto.getFile(), exportDto.getFilename(), securedInfo, repointTargets, notifyOwnerOnLmsFailure);

            candidate.setStatus(ExperimentCopyCandidateStatus.IMPORTED);
            candidate.setResultingImportUuid(importDto.getId());
            experimentCopyCandidateRepository.save(candidate);

            return importDto;
        } catch (ExperimentExportException | ExperimentImportException e) {
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
                continue;
            }

            LmsExternalToolUrlUtils.extractQueryParam(url, "assignment")
                .flatMap(this::resolveAssignmentId)
                .filter(sourceAssignmentIds::contains)
                .ifPresent(oldAssignmentId -> repointMap.put(oldAssignmentId, lmsAssignment));
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
        } catch (IllegalArgumentException e) {
            try {
                return Optional.of(Long.parseLong(idText));
            } catch (NumberFormatException nfe) {
                return Optional.empty();
            }
        }
    }

    private CopyCandidateDto toDto(ExperimentCopyCandidate candidate) {
        Experiment experiment = candidate.getSourceExperiment();

        return CopyCandidateDto.builder()
            .id(candidate.getUuid())
            .sourceExperimentId(experiment.getExperimentId())
            .experimentTitle(experiment.getTitle())
            .sourceCourseTitle(experiment.getLtiContextEntity().getTitle())
            .conditionCount(Math.toIntExact(conditionRepository.countByExperiment_ExperimentId(experiment.getExperimentId())))
            .assignmentCount(assignmentRepository.findByExposure_Experiment_ExperimentId(experiment.getExperimentId()).size())
            .status(candidate.getStatus())
            .build();
    }

}
