package edu.iu.terracotta.service.app.distribute.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.service.lti.LtiNoticeService;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ObsoleteAssignment;
import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCandidate;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateDto;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateResolutionDto;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.dao.model.dto.distribute.ImportDto;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyCandidateStatus;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ObsoleteAssignmentRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentCopyCandidateRepository;
import edu.iu.terracotta.exceptions.ExperimentCopyCandidateNotFoundException;
import edu.iu.terracotta.exceptions.ExperimentExportException;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyCandidateService;
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

    private final ExperimentCopyCandidateRepository experimentCopyCandidateRepository;
    private final ExperimentRepository experimentRepository;
    private final ConditionRepository conditionRepository;
    private final AssignmentRepository assignmentRepository;
    private final ObsoleteAssignmentRepository obsoleteAssignmentRepository;
    private final LtiUserRepository ltiUserRepository;
    private final LtiNoticeService ltiNoticeService;
    private final FeatureService featureService;
    private final AssignmentService assignmentService;
    private final AssignmentAsyncService assignmentAsyncService;
    private final ExperimentExportService experimentExportService;
    private final ExperimentImportService experimentImportService;

    @Override
    public void stageFromNotice(Claims noticeClaims) {
        LtiContextEntity destination = ltiNoticeService.resolveOrCreateContext(noticeClaims).orElse(null);

        if (destination == null) {
            log.warn("Could not resolve or create a destination context for a course-copy notice. Issuer: [{}]", noticeClaims.getIssuer());
            return;
        }

        long platformDeploymentKeyId = destination.getToolDeployment().getPlatformDeployment().getKeyId();

        if (!featureService.isFeatureEnabled(FeatureType.PLATFORM_NOTIFICATIONS, platformDeploymentKeyId)) {
            log.info("Platform notifications feature is disabled for platform deployment ID: [{}] - not staging copy candidates for destination context ID: [{}]", platformDeploymentKeyId, destination.getContextId());
            return;
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
    }

    @Override
    public boolean hasPendingForContext(long contextId) {
        return experimentCopyCandidateRepository.existsByDestinationContext_ContextIdAndStatus(contextId, ExperimentCopyCandidateStatus.PENDING);
    }

    @Override
    public List<CopyCandidateDto> getPendingForContext(SecuredInfo securedInfo) {
        if (!experimentRepository.findAllByLtiContextEntity_ContextId(securedInfo.getContextId()).isEmpty()) {
            // this course already has at least one Experiment of its own by the time of this
            // live launch - don't surface candidates, matching the same "is this course new"
            // gating the rest of the app already applies to the zero-state experience
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
                imports.add(importCandidate(candidate, securedInfo, lmsAssignments));
            } catch (ExperimentCopyCandidateNotFoundException | ExperimentImportException e) {
                log.error("Error importing copy candidate ID: [{}] during bulk resolve", candidate.getUuid(), e);
            }
        }

        List<UUID> declinedIds = new ArrayList<>();

        for (ExperimentCopyCandidate candidate : toDecline) {
            candidate.setStatus(ExperimentCopyCandidateStatus.NOT_SELECTED);
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

    private ImportDto importCandidate(ExperimentCopyCandidate candidate, SecuredInfo securedInfo, List<LmsAssignment> lmsAssignments) throws ExperimentCopyCandidateNotFoundException, ExperimentImportException {
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
            Map<Long, LmsAssignment> assignmentRepointMap = buildAssignmentRepointMap(sourceExperiment, lmsAssignments, securedInfo);
            ExportDto exportDto = experimentExportService.export(sourceExperiment);
            ImportDto importDto = experimentImportService.preprocessFromFile(exportDto.getFile(), exportDto.getFilename(), securedInfo, assignmentRepointMap);

            candidate.setStatus(ExperimentCopyCandidateStatus.IMPORTED);
            candidate.setResultingImportUuid(importDto.getId());
            experimentCopyCandidateRepository.save(candidate);

            return importDto;
        } catch (ExperimentExportException | ExperimentImportException e) {
            candidate.setStatus(ExperimentCopyCandidateStatus.ERROR);
            experimentCopyCandidateRepository.save(candidate);

            throw new ExperimentImportException(String.format("Error recreating experiment from copy candidate ID: [%s]", candidate.getUuid()), e);
        }
    }

    // matches a copied Canvas assignment already sitting in the destination course back to the
    // source experiment's own assignment it was copied from, by parsing the same "assignment="
    // query parameter the obsolete-assignment check already parses (see LmsExternalToolUrlUtils)
    // - Canvas course-copy duplicates external_tool_tag_attributes.url verbatim, so a copied
    // assignment's URL still carries the source course's old assignment ID.
    private Map<Long, LmsAssignment> buildAssignmentRepointMap(Experiment sourceExperiment, List<LmsAssignment> lmsAssignments, SecuredInfo securedInfo) {
        if (CollectionUtils.isEmpty(lmsAssignments)) {
            return Map.of();
        }

        List<Long> sourceAssignmentIds = assignmentRepository.findByExposure_Experiment_ExperimentId(sourceExperiment.getExperimentId()).stream()
            .map(Assignment::getAssignmentId)
            .toList();

        if (CollectionUtils.isEmpty(sourceAssignmentIds)) {
            return Map.of();
        }

        List<String> convertedLmsAssignmentIds = obsoleteAssignmentRepository.findAllByContext_ContextId(securedInfo.getContextId()).stream()
            .map(ObsoleteAssignment::getLmsAssignmentId)
            .toList();

        LtiUserEntity apiUser = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        String localUrl = apiUser.getPlatformDeployment().getLocalUrl();

        Map<Long, LmsAssignment> repointMap = new HashMap<>();

        for (LmsAssignment lmsAssignment : lmsAssignments) {
            if (lmsAssignment.getLmsExternalToolFields() == null
                || convertedLmsAssignmentIds.contains(lmsAssignment.getId())
                || !Strings.CI.contains(lmsAssignment.getLmsExternalToolFields().getUrl(), localUrl)) {
                continue;
            }

            LmsExternalToolUrlUtils.extractQueryParamAsLong(lmsAssignment.getLmsExternalToolFields().getUrl(), "assignment")
                .filter(sourceAssignmentIds::contains)
                .ifPresent(oldAssignmentId -> repointMap.put(oldAssignmentId, lmsAssignment));
        }

        return repointMap;
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
