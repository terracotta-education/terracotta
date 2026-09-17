package edu.iu.terracotta.service.app.distribute.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.jsonwebtoken.Claims;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsExternalToolFields;
import edu.iu.terracotta.connectors.generic.service.lti.LtiNoticeService;
import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCandidate;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateDto;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateResolutionDto;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyCandidateStatus;
import edu.iu.terracotta.dao.repository.distribute.ExperimentCopyCandidateRepository;
import edu.iu.terracotta.exceptions.ExperimentExportException;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentExportService;

class ExperimentCopyCandidateServiceImplTest extends BaseTest {

    @Mock private ExperimentCopyCandidateRepository experimentCopyCandidateRepository;
    @Mock private LtiNoticeService ltiNoticeService;
    @Mock private FeatureService featureService;
    @Mock private ExperimentExportService experimentExportService;
    @Mock private AssignmentAsyncService assignmentAsyncService;
    @Mock private Claims noticeClaims;
    @Mock private ExperimentCopyCandidate copyCandidate;

    @InjectMocks private ExperimentCopyCandidateServiceImpl experimentCopyCandidateService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        when(featureService.isFeatureEnabled(eq(FeatureType.PLATFORM_NOTIFICATIONS), anyLong())).thenReturn(true);
    }

    @Test
    void testStageFromNoticeNoDestinationResolvedDoesNothing() {
        when(ltiNoticeService.resolveOrCreateContext(noticeClaims)).thenReturn(java.util.Optional.empty());

        experimentCopyCandidateService.stageFromNotice(noticeClaims);

        verify(experimentCopyCandidateRepository, never()).save(any());
    }

    @Test
    void testStageFromNoticeFeatureDisabledDoesNothing() {
        when(ltiNoticeService.resolveOrCreateContext(noticeClaims)).thenReturn(java.util.Optional.of(ltiContextEntity));
        when(featureService.isFeatureEnabled(eq(FeatureType.PLATFORM_NOTIFICATIONS), anyLong())).thenReturn(false);

        experimentCopyCandidateService.stageFromNotice(noticeClaims);

        verify(ltiNoticeService, never()).resolveOriginContexts(any());
        verify(experimentCopyCandidateRepository, never()).save(any());
    }

    @Test
    void testStageFromNoticeStagesOneCandidatePerExperimentAcrossOrigins() {
        LtiContextEntity origin1 = mock(LtiContextEntity.class);
        LtiContextEntity origin2 = mock(LtiContextEntity.class);
        when(origin1.getContextId()).thenReturn(10L);
        when(origin2.getContextId()).thenReturn(20L);

        when(ltiNoticeService.resolveOrCreateContext(noticeClaims)).thenReturn(java.util.Optional.of(ltiContextEntity));
        when(ltiNoticeService.resolveOriginContexts(noticeClaims)).thenReturn(List.of(origin1, origin2));
        when(experimentRepository.findAllByLtiContextEntity_ContextId(10L)).thenReturn(List.of(experiment));
        when(experimentRepository.findAllByLtiContextEntity_ContextId(20L)).thenReturn(List.of());
        when(experimentCopyCandidateRepository.existsBySourceExperiment_ExperimentIdAndDestinationContext_ContextId(1L, 1L)).thenReturn(false);

        experimentCopyCandidateService.stageFromNotice(noticeClaims);

        verify(experimentCopyCandidateRepository, times(1)).save(any(ExperimentCopyCandidate.class));
    }

    // PNS notices can be redelivered - staging the same (experiment, destination) pair twice
    // must not create a second candidate row
    @Test
    void testStageFromNoticeIsIdempotentOnRedelivery() {
        LtiContextEntity origin = mock(LtiContextEntity.class);
        when(origin.getContextId()).thenReturn(10L);

        when(ltiNoticeService.resolveOrCreateContext(noticeClaims)).thenReturn(java.util.Optional.of(ltiContextEntity));
        when(ltiNoticeService.resolveOriginContexts(noticeClaims)).thenReturn(List.of(origin));
        when(experimentRepository.findAllByLtiContextEntity_ContextId(10L)).thenReturn(List.of(experiment));
        when(experimentCopyCandidateRepository.existsBySourceExperiment_ExperimentIdAndDestinationContext_ContextId(1L, 1L)).thenReturn(true);

        experimentCopyCandidateService.stageFromNotice(noticeClaims);

        verify(experimentCopyCandidateRepository, never()).save(any());
    }

    @Test
    void testGetPendingForContextEmptyWhenDestinationAlreadyHasExperiments() {
        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentRepository.findAllByLtiContextEntity_ContextId(1L)).thenReturn(List.of(experiment));

        List<CopyCandidateDto> result = experimentCopyCandidateService.getPendingForContext(securedInfo);

        assertTrue(result.isEmpty());
        verify(experimentCopyCandidateRepository, never()).findAllByDestinationContext_ContextIdAndStatus(anyLong(), any());
    }

    @Test
    void testGetPendingForContextReturnsCandidates() {
        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentRepository.findAllByLtiContextEntity_ContextId(1L)).thenReturn(List.of());
        when(copyCandidate.getUuid()).thenReturn(UUID.randomUUID());
        when(copyCandidate.getSourceExperiment()).thenReturn(experiment);
        when(copyCandidate.getStatus()).thenReturn(ExperimentCopyCandidateStatus.PENDING);
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(copyCandidate));
        when(conditionRepository.countByExperiment_ExperimentId(1L)).thenReturn(2L);
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(1L)).thenReturn(List.of());

        List<CopyCandidateDto> result = experimentCopyCandidateService.getPendingForContext(securedInfo);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getConditionCount());
    }

    // a destination course re-copied from a different source before ever resolving the first
    // prompt could accumulate PENDING candidates from two different source courses - only the
    // most recently staged notice's source course should be surfaced, so the dialog's single
    // "copied from X" heading is never wrong for some of the candidates it lists
    @Test
    void testGetPendingForContextOnlySurfacesMostRecentSourceCourse() {
        edu.iu.terracotta.dao.entity.Experiment olderExperiment = mock(edu.iu.terracotta.dao.entity.Experiment.class);
        LtiContextEntity olderSourceContext = mock(LtiContextEntity.class);
        when(olderSourceContext.getContextId()).thenReturn(10L);
        when(olderExperiment.getLtiContextEntity()).thenReturn(olderSourceContext);
        when(olderExperiment.getExperimentId()).thenReturn(2L);

        LtiContextEntity newerSourceContext = mock(LtiContextEntity.class);
        when(newerSourceContext.getContextId()).thenReturn(20L);
        when(experiment.getLtiContextEntity()).thenReturn(newerSourceContext);

        ExperimentCopyCandidate olderCandidate = mock(ExperimentCopyCandidate.class);
        when(olderCandidate.getUuid()).thenReturn(UUID.randomUUID());
        when(olderCandidate.getSourceExperiment()).thenReturn(olderExperiment);
        when(olderCandidate.getCreatedAt()).thenReturn(new java.sql.Timestamp(1000L));

        when(copyCandidate.getUuid()).thenReturn(UUID.randomUUID());
        when(copyCandidate.getSourceExperiment()).thenReturn(experiment);
        when(copyCandidate.getCreatedAt()).thenReturn(new java.sql.Timestamp(2000L));

        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentRepository.findAllByLtiContextEntity_ContextId(1L)).thenReturn(List.of());
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(olderCandidate, copyCandidate));
        when(conditionRepository.countByExperiment_ExperimentId(anyLong())).thenReturn(0L);
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(List.of());

        List<CopyCandidateDto> result = experimentCopyCandidateService.getPendingForContext(securedInfo);

        assertEquals(1, result.size());
        assertEquals(copyCandidate.getUuid(), result.get(0).getId());
    }

    @Test
    void testHasPendingForContextTrue() {
        when(experimentCopyCandidateRepository.existsByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING)).thenReturn(true);

        assertTrue(experimentCopyCandidateService.hasPendingForContext(1L));
    }

    @Test
    void testHasPendingForContextFalse() {
        when(experimentCopyCandidateRepository.existsByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING)).thenReturn(false);

        assertFalse(experimentCopyCandidateService.hasPendingForContext(1L));
    }

    @Test
    void testResolveImportsSelectedAndDeclinesRestAndFiresObsoleteCheckOnce() throws Exception {
        UUID selectedId = UUID.randomUUID();
        UUID declinedId = UUID.randomUUID();
        ExperimentCopyCandidate selected = mock(ExperimentCopyCandidate.class);
        ExperimentCopyCandidate declined = mock(ExperimentCopyCandidate.class);
        when(selected.getUuid()).thenReturn(selectedId);
        when(selected.getSourceExperiment()).thenReturn(experiment);
        when(declined.getUuid()).thenReturn(declinedId);

        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(selected, declined));

        File exportFile = mock(File.class);
        ExportDto exportDto = ExportDto.builder().file(exportFile).filename("export.zip").build();
        when(assignmentService.getAllAssignmentsForLmsCourse(securedInfo)).thenReturn(List.of());
        when(experimentExportService.export(experiment)).thenReturn(exportDto);
        when(experimentImportService.preprocessFromFile(eq(exportFile), eq("export.zip"), eq(securedInfo), anyMap())).thenReturn(importDto);

        CopyCandidateResolutionDto result = experimentCopyCandidateService.resolve(List.of(selectedId), securedInfo);

        assertEquals(1, result.getImports().size());
        assertEquals(List.of(declinedId), result.getDeclinedCandidateIds());
        verify(selected).setStatus(ExperimentCopyCandidateStatus.IMPORTING);
        verify(selected).setStatus(ExperimentCopyCandidateStatus.IMPORTED);
        verify(declined).setStatus(ExperimentCopyCandidateStatus.NOT_SELECTED);
        verify(experimentCopyCandidateRepository).save(declined);
        verify(assignmentAsyncService, times(1)).handleAssignmentTasksInLmsByContext(securedInfo);
    }

    @Test
    void testResolveWithEmptySelectionDismissesEverythingAndStillFiresObsoleteCheck() throws Exception {
        ExperimentCopyCandidate onlyCandidate = mock(ExperimentCopyCandidate.class);
        when(onlyCandidate.getUuid()).thenReturn(UUID.randomUUID());

        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(onlyCandidate));

        CopyCandidateResolutionDto result = experimentCopyCandidateService.resolve(List.of(), securedInfo);

        assertTrue(result.getImports().isEmpty());
        assertEquals(1, result.getDeclinedCandidateIds().size());
        // an empty selection ("No thank you") declines every pending candidate at once - that's
        // DISMISSED, not NOT_SELECTED (which only applies to a candidate left out of an
        // otherwise non-empty selection - see testResolveImportsSelectedAndDeclinesRestAndFiresObsoleteCheckOnce)
        verify(onlyCandidate).setStatus(ExperimentCopyCandidateStatus.DISMISSED);
        verify(assignmentService, never()).getAllAssignmentsForLmsCourse(any());
        verify(assignmentAsyncService, times(1)).handleAssignmentTasksInLmsByContext(securedInfo);
    }

    @Test
    void testResolveImportFailureIsLoggedAndDoesNotBlockDeclinesOrObsoleteCheck() throws Exception {
        UUID selectedId = UUID.randomUUID();
        ExperimentCopyCandidate selected = mock(ExperimentCopyCandidate.class);
        when(selected.getUuid()).thenReturn(selectedId);
        when(selected.getSourceExperiment()).thenReturn(experiment);

        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(selected));
        when(assignmentService.getAllAssignmentsForLmsCourse(securedInfo)).thenReturn(List.of());
        doThrow(new ExperimentExportException("export failed")).when(experimentExportService).export(experiment);

        CopyCandidateResolutionDto result = experimentCopyCandidateService.resolve(List.of(selectedId), securedInfo);

        assertTrue(result.getImports().isEmpty());
        verify(selected).setStatus(ExperimentCopyCandidateStatus.ERROR);
        verify(assignmentAsyncService, times(1)).handleAssignmentTasksInLmsByContext(securedInfo);
    }

    @Test
    void testResolveBuildsRepointMapForMatchingLmsAssignmentAndPassesItToImport() throws Exception {
        UUID selectedId = UUID.randomUUID();
        ExperimentCopyCandidate selected = mock(ExperimentCopyCandidate.class);
        when(selected.getUuid()).thenReturn(selectedId);
        when(selected.getSourceExperiment()).thenReturn(experiment);

        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(selected));

        // matches: not already obsoleted (id "999", default obsoleted id is "1"), URL contains
        // localUrl and carries assignment=1, which is the source experiment's own assignment id
        LmsAssignment matchingLmsAssignment = LmsAssignment.builder()
            .id("999")
            .lmsExternalToolFields(
                LmsExternalToolFields.builder()
                    .url(LTI_URL + "/lti3?experiment=55&assignment=1")
                    .build()
            )
            .build();
        when(assignmentService.getAllAssignmentsForLmsCourse(securedInfo)).thenReturn(List.of(matchingLmsAssignment));

        File exportFile = mock(File.class);
        ExportDto exportDto = ExportDto.builder().file(exportFile).filename("export.zip").build();
        when(experimentExportService.export(experiment)).thenReturn(exportDto);
        when(experimentImportService.preprocessFromFile(eq(exportFile), eq("export.zip"), eq(securedInfo), anyMap())).thenReturn(importDto);

        experimentCopyCandidateService.resolve(List.of(selectedId), securedInfo);

        verify(experimentImportService).preprocessFromFile(
            eq(exportFile),
            eq("export.zip"),
            eq(securedInfo),
            eq(Map.of(1L, matchingLmsAssignment))
        );
    }

    @Test
    void testResolveSkipsRepointForAlreadyObsoletedLmsAssignment() throws Exception {
        UUID selectedId = UUID.randomUUID();
        ExperimentCopyCandidate selected = mock(ExperimentCopyCandidate.class);
        when(selected.getUuid()).thenReturn(selectedId);
        when(selected.getSourceExperiment()).thenReturn(experiment);

        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(selected));

        // id "1" matches the default-stubbed obsoleteAssignmentRepository entry (already
        // converted to an OBSOLETE assignment) - must not be offered for re-pointing
        LmsAssignment alreadyObsoletedLmsAssignment = LmsAssignment.builder()
            .id("1")
            .lmsExternalToolFields(
                LmsExternalToolFields.builder()
                    .url(LTI_URL + "/lti3?experiment=55&assignment=1")
                    .build()
            )
            .build();
        when(assignmentService.getAllAssignmentsForLmsCourse(securedInfo)).thenReturn(List.of(alreadyObsoletedLmsAssignment));

        File exportFile = mock(File.class);
        ExportDto exportDto = ExportDto.builder().file(exportFile).filename("export.zip").build();
        when(experimentExportService.export(experiment)).thenReturn(exportDto);
        when(experimentImportService.preprocessFromFile(eq(exportFile), eq("export.zip"), eq(securedInfo), anyMap())).thenReturn(importDto);

        experimentCopyCandidateService.resolve(List.of(selectedId), securedInfo);

        verify(experimentImportService).preprocessFromFile(exportFile, "export.zip", securedInfo, Map.of());
    }

}
