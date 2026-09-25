package edu.iu.terracotta.service.app.distribute.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import io.jsonwebtoken.Claims;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsExternalToolFields;
import edu.iu.terracotta.connectors.generic.dao.model.lti.Roles;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.service.lti.LtiNoticeService;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCandidate;
import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCreatedAssignment;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.model.distribute.LmsRepointTargets;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateDto;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateResolutionDto;
import edu.iu.terracotta.dao.model.dto.distribute.CopyStatusDto;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyCandidateStatus;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyStatus;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.dao.repository.distribute.ExperimentCopyCandidateRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentCopyCreatedAssignmentRepository;
import edu.iu.terracotta.exceptions.ExperimentExportException;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyNotificationService;
import edu.iu.terracotta.service.app.distribute.ExperimentExportService;

class ExperimentCopyCandidateServiceImplTest extends BaseTest {

    @Mock private ExperimentCopyCandidateRepository experimentCopyCandidateRepository;
    @Mock private LtiNoticeService ltiNoticeService;
    @Mock private FeatureService featureService;
    @Mock private ExperimentExportService experimentExportService;
    @Mock private AssignmentAsyncService assignmentAsyncService;
    @Mock private ExperimentCopyNotificationService experimentCopyNotificationService;
    @Mock private ExperimentCopyCreatedAssignmentRepository experimentCopyCreatedAssignmentRepository;
    @Mock private Claims noticeClaims;
    @Mock private ExperimentCopyCandidate copyCandidate;

    private ExperimentCopyCandidateServiceImpl experimentCopyCandidateService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        // constructed manually rather than via @InjectMocks - see the note in BaseServiceTest
        // about apiClient colliding with canvasApiClient
        experimentCopyCandidateService = new ExperimentCopyCandidateServiceImpl(
            experimentCopyCandidateRepository,
            experimentRepository,
            conditionRepository,
            assignmentRepository,
            obsoleteAssignmentRepository,
            ltiUserRepository,
            ltiContextRepository,
            ltiMembershipRepository,
            experimentImportRepository,
            apiClient,
            ltiNoticeService,
            featureService,
            assignmentService,
            assignmentAsyncService,
            experimentExportService,
            experimentImportService,
            experimentCopyNotificationService,
            experimentCopyCreatedAssignmentRepository
        );

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
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of());

        List<CopyCandidateDto> result = experimentCopyCandidateService.getPendingForContext(securedInfo);

        assertTrue(result.isEmpty());
        verify(experimentCopyCandidateRepository, never()).save(any());
    }

    @Test
    void testGetPendingForContextDismissesStalePendingCandidatesWhenDestinationAlreadyHasExperiments() {
        ExperimentCopyCandidate stalePending = mock(ExperimentCopyCandidate.class);

        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentRepository.findAllByLtiContextEntity_ContextId(1L)).thenReturn(List.of(experiment));
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(stalePending));

        List<CopyCandidateDto> result = experimentCopyCandidateService.getPendingForContext(securedInfo);

        assertTrue(result.isEmpty());
        verify(stalePending).setStatus(ExperimentCopyCandidateStatus.DISMISSED);
        verify(experimentCopyCandidateRepository).save(stalePending);
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
    void testStageFromNoticeReturnsDestinationContextId() {
        when(ltiNoticeService.resolveOrCreateContext(noticeClaims)).thenReturn(Optional.of(ltiContextEntity));
        when(ltiNoticeService.resolveOriginContexts(noticeClaims)).thenReturn(List.of());

        assertEquals(Optional.of(1L), experimentCopyCandidateService.stageFromNotice(noticeClaims));
    }

    @Test
    void testStageFromNoticeFeatureDisabledReturnsEmpty() {
        when(ltiNoticeService.resolveOrCreateContext(noticeClaims)).thenReturn(Optional.of(ltiContextEntity));
        when(featureService.isFeatureEnabled(eq(FeatureType.PLATFORM_NOTIFICATIONS), anyLong())).thenReturn(false);

        assertTrue(experimentCopyCandidateService.stageFromNotice(noticeClaims).isEmpty());
    }

    @Test
    void testRecreateForContextDestinationNotFoundDoesNothing() {
        when(ltiContextRepository.findById(1L)).thenReturn(Optional.empty());

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(experimentCopyCandidateRepository, never()).findAllByDestinationContext_ContextIdAndStatus(anyLong(), any());
    }

    @Test
    void testRecreateForContextNothingPendingDoesNothing() throws Exception {
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of());

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(apiClient, never()).getLmsCourseId(any(), any());
    }

    @Test
    void testRecreateForContextImportsEveryPendingCandidateAsTheSourceInstructor() throws Exception {
        ExperimentCopyCandidate first = pendingCandidate();
        ExperimentCopyCandidate second = pendingCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(first, second));
        when(apiClient.getLmsCourseId(ltiUserEntity, ltiContextEntity)).thenReturn(Optional.of("123"));
        when(assignmentService.getAllAssignmentsForLmsCourse(any(SecuredInfo.class))).thenReturn(List.of());

        File exportFile = mock(File.class);
        when(experimentExportService.export(experiment)).thenReturn(ExportDto.builder().file(exportFile).filename("export.zip").build());
        when(experimentImportService.preprocessFromFile(eq(exportFile), eq("export.zip"), any(SecuredInfo.class), any(LmsRepointTargets.class), eq(true))).thenReturn(importDto);

        experimentCopyCandidateService.recreateForContext(1L, null);

        ArgumentCaptor<SecuredInfo> securedInfoCaptor = ArgumentCaptor.forClass(SecuredInfo.class);
        verify(experimentImportService, times(2)).preprocessFromFile(eq(exportFile), eq("export.zip"), securedInfoCaptor.capture(), any(LmsRepointTargets.class), eq(true));
        SecuredInfo actingAs = securedInfoCaptor.getValue();
        assertEquals(1L, actingAs.getContextId());
        assertEquals(USER_ID, actingAs.getUserId());
        assertEquals("123", actingAs.getLmsCourseId());
        assertTrue(actingAs.getRoles().contains(Roles.MEMBERSHIP_INSTRUCTOR));

        // one LMS session per instructor, not per candidate
        verify(apiClient, times(1)).getLmsCourseId(ltiUserEntity, ltiContextEntity);
        verify(assignmentService, times(1)).getAllAssignmentsForLmsCourse(any(SecuredInfo.class));
        verify(first).setStatus(ExperimentCopyCandidateStatus.IMPORTED);
        verify(second).setStatus(ExperimentCopyCandidateStatus.IMPORTED);
        verify(experimentCopyNotificationService, never()).notifyLmsFailure(any());
    }

    @Test
    void testRecreateForContextFallsBackToAnyInstructorInTheSourceCourse() throws Exception {
        ExperimentCopyCandidate candidate = pendingCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(candidate));
        when(experiment.getCreatedBy()).thenReturn(null);
        LtiMembershipEntity membership = mock(LtiMembershipEntity.class);
        when(membership.getUser()).thenReturn(ltiUserEntity);
        when(ltiMembershipRepository.findFirstByContextAndRoleGreaterThanEqual(ltiContextEntity, 1)).thenReturn(Optional.of(membership));
        when(apiClient.getLmsCourseId(ltiUserEntity, ltiContextEntity)).thenReturn(Optional.of("123"));
        when(assignmentService.getAllAssignmentsForLmsCourse(any(SecuredInfo.class))).thenReturn(List.of());
        when(experimentExportService.export(experiment)).thenReturn(ExportDto.builder().file(mock(File.class)).filename("export.zip").build());
        when(experimentImportService.preprocessFromFile(any(), any(), any(), any(LmsRepointTargets.class), eq(true))).thenReturn(importDto);

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(candidate).setStatus(ExperimentCopyCandidateStatus.IMPORTED);
    }

    @Test
    void testRecreateForContextNoInstructorMarksError() throws Exception {
        ExperimentCopyCandidate candidate = pendingCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(candidate));
        when(experiment.getCreatedBy()).thenReturn(null);
        when(ltiMembershipRepository.findFirstByContextAndRoleGreaterThanEqual(any(), eq(1))).thenReturn(Optional.empty());

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(candidate).setStatus(ExperimentCopyCandidateStatus.ERROR);
        verify(candidate).setErrorMessage("No instructor found in the source course to recreate this experiment as");
        verify(experimentExportService, never()).export(any());
        verify(experimentCopyNotificationService, never()).notifyLmsFailure(any());
    }

    @Test
    void testRecreateForContextInstructorWithoutLmsAuthorizationMarksError() throws Exception {
        ExperimentCopyCandidate candidate = pendingCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(candidate));
        when(apiClient.getLmsCourseId(ltiUserEntity, ltiContextEntity))
            .thenThrow(new ApiException("lookup failed", new LmsOAuthException("no token")));

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(candidate).setStatus(ExperimentCopyCandidateStatus.ERROR);
        verify(candidate).setErrorMessage("The source course's instructor has not authorized Terracotta to access the LMS");
        verify(experimentExportService, never()).export(any());
        verify(experimentCopyNotificationService).notifyLmsFailure(ltiUserEntity);
    }

    @Test
    void testRecreateForContextCourseNotFoundMarksError() throws Exception {
        ExperimentCopyCandidate candidate = pendingCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(candidate));
        when(apiClient.getLmsCourseId(ltiUserEntity, ltiContextEntity)).thenReturn(Optional.empty());

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(candidate).setErrorMessage("Could not find the copied course in the LMS");
        verify(assignmentService, never()).getAllAssignmentsForLmsCourse(any());
    }

    // several candidates from the same instructor share one failed LMS session - one email, not one per candidate
    @Test
    void testRecreateForContextEmailsEachFailingInstructorOnce() throws Exception {
        ExperimentCopyCandidate first = pendingCandidate();
        ExperimentCopyCandidate second = pendingCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(first, second));
        when(apiClient.getLmsCourseId(ltiUserEntity, ltiContextEntity)).thenReturn(Optional.empty());

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(first).setStatus(ExperimentCopyCandidateStatus.ERROR);
        verify(second).setStatus(ExperimentCopyCandidateStatus.ERROR);
        verify(experimentCopyNotificationService, times(1)).notifyLmsFailure(ltiUserEntity);
    }

    @Test
    void testRecreateForContextAssignmentListingFailureMarksError() throws Exception {
        ExperimentCopyCandidate candidate = pendingCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(candidate));
        when(apiClient.getLmsCourseId(ltiUserEntity, ltiContextEntity)).thenReturn(Optional.of("123"));
        when(assignmentService.getAllAssignmentsForLmsCourse(any(SecuredInfo.class))).thenThrow(new ApiException("forbidden"));

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(candidate).setErrorMessage("Could not list the copied course's assignments in the LMS");
        verify(experimentExportService, never()).export(any());
        verify(experimentCopyNotificationService).notifyLmsFailure(ltiUserEntity);
    }

    @Test
    void testRecreateForContextImportFailureRecordsErrorMessageAndContinues() throws Exception {
        ExperimentCopyCandidate failing = pendingCandidate();
        ExperimentCopyCandidate next = pendingCandidate();
        Experiment otherExperiment = mock(Experiment.class);
        when(otherExperiment.getExperimentId()).thenReturn(2L);
        when(otherExperiment.getCreatedBy()).thenReturn(ltiUserEntity);
        when(next.getSourceExperiment()).thenReturn(otherExperiment);
        when(experimentRepository.findByExperimentId(2L)).thenReturn(otherExperiment);
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(failing, next));
        when(apiClient.getLmsCourseId(ltiUserEntity, ltiContextEntity)).thenReturn(Optional.of("123"));
        when(assignmentService.getAllAssignmentsForLmsCourse(any(SecuredInfo.class))).thenReturn(List.of());
        doThrow(new ExperimentExportException("export failed")).when(experimentExportService).export(experiment);
        when(experimentExportService.export(otherExperiment)).thenReturn(ExportDto.builder().file(mock(File.class)).filename("export.zip").build());
        when(experimentImportService.preprocessFromFile(any(), any(), any(), any(LmsRepointTargets.class), eq(true))).thenReturn(importDto);

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(failing).setStatus(ExperimentCopyCandidateStatus.ERROR);
        verify(failing).setErrorMessage("ExperimentExportException: export failed");
        verify(next).setStatus(ExperimentCopyCandidateStatus.IMPORTED);
    }

    // a redelivered notice can start a second recreation for the same course - whichever one
    // claims a candidate second loses the optimistic lock and must leave it alone
    @Test
    void testRecreateForContextSkipsCandidateAlreadyClaimedElsewhere() throws Exception {
        ExperimentCopyCandidate candidate = pendingCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(candidate));
        when(apiClient.getLmsCourseId(ltiUserEntity, ltiContextEntity)).thenReturn(Optional.of("123"));
        when(assignmentService.getAllAssignmentsForLmsCourse(any(SecuredInfo.class))).thenReturn(List.of());
        when(experimentCopyCandidateRepository.save(candidate)).thenThrow(new ObjectOptimisticLockingFailureException(ExperimentCopyCandidate.class, 1L));

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(experimentExportService, never()).export(any());
    }

    // an instructor retrying from their own launch acts as themselves, and sees the result on the
    // page rather than by email
    @Test
    void testRecreateForContextActsAsTheRetryingInstructorWithoutEmailing() throws Exception {
        LtiUserEntity retryingInstructor = mock(LtiUserEntity.class);
        when(retryingInstructor.getUserKey()).thenReturn("retrying-user");
        when(retryingInstructor.getPlatformDeployment()).thenReturn(platformDeployment);
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId("retrying-user", 1L)).thenReturn(retryingInstructor);
        when(toolDeployment.getPlatformDeployment()).thenReturn(platformDeployment);

        ExperimentCopyCandidate candidate = pendingCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(candidate));
        when(apiClient.getLmsCourseId(retryingInstructor, ltiContextEntity)).thenReturn(Optional.of("123"));
        when(assignmentService.getAllAssignmentsForLmsCourse(any(SecuredInfo.class))).thenReturn(List.of());
        when(experimentExportService.export(experiment)).thenReturn(ExportDto.builder().file(mock(File.class)).filename("export.zip").build());
        when(experimentImportService.preprocessFromFile(any(), any(), any(), any(LmsRepointTargets.class), eq(false))).thenReturn(importDto);

        experimentCopyCandidateService.recreateForContext(1L, "retrying-user");

        ArgumentCaptor<SecuredInfo> securedInfoCaptor = ArgumentCaptor.forClass(SecuredInfo.class);
        verify(experimentImportService).preprocessFromFile(any(), any(), securedInfoCaptor.capture(), any(LmsRepointTargets.class), eq(false));
        assertEquals("retrying-user", securedInfoCaptor.getValue().getUserId());
        verify(candidate).setStatus(ExperimentCopyCandidateStatus.IMPORTED);
    }

    @Test
    void testRecreateForContextRetryingInstructorLmsFailureDoesNotEmail() throws Exception {
        LtiUserEntity retryingInstructor = mock(LtiUserEntity.class);
        when(retryingInstructor.getPlatformDeployment()).thenReturn(platformDeployment);
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId("retrying-user", 1L)).thenReturn(retryingInstructor);
        when(toolDeployment.getPlatformDeployment()).thenReturn(platformDeployment);

        ExperimentCopyCandidate candidate = pendingCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(candidate));
        when(apiClient.getLmsCourseId(retryingInstructor, ltiContextEntity)).thenReturn(Optional.empty());

        experimentCopyCandidateService.recreateForContext(1L, "retrying-user");

        verify(candidate).setStatus(ExperimentCopyCandidateStatus.ERROR);
        verify(experimentCopyNotificationService, never()).notifyLmsFailure(any());
    }

    @Test
    void testResetFailedForRetryResetsFailedCandidatesAndFailedImports() {
        ExperimentCopyCandidate failedOutright = mock(ExperimentCopyCandidate.class);
        when(failedOutright.getStatus()).thenReturn(ExperimentCopyCandidateStatus.ERROR);
        ExperimentCopyCandidate importFailed = importedCandidate();
        ExperimentCopyCandidate succeeded = importedCandidate();
        ExperimentImport succeededImport = mock(ExperimentImport.class);
        when(succeededImport.getStatus()).thenReturn(ExperimentImportStatus.COMPLETE);
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.ERROR);
        when(experimentImportRepository.findByUuid(importFailed.getResultingImportUuid())).thenReturn(Optional.of(experimentImport));
        when(experimentImportRepository.findByUuid(succeeded.getResultingImportUuid())).thenReturn(Optional.of(succeededImport));
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatusInAndAcknowledgedAtIsNull(eq(1L), anyList()))
            .thenReturn(List.of(failedOutright, importFailed, succeeded));

        assertTrue(experimentCopyCandidateService.resetFailedForRetry(1L));

        verify(failedOutright).setStatus(ExperimentCopyCandidateStatus.PENDING);
        verify(failedOutright).setErrorMessage(null);
        verify(importFailed).setStatus(ExperimentCopyCandidateStatus.PENDING);
        verify(importFailed).setResultingImportUuid(null);
        verify(experimentImport).setStatus(ExperimentImportStatus.ERROR_ACKNOWLEDGED);
        verify(succeeded, never()).setStatus(any());
        verify(succeededImport, never()).setStatus(any());
    }

    @Test
    void testResetFailedForRetryNothingFailed() {
        ExperimentCopyCandidate succeeded = importedCandidate();
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.COMPLETE);
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatusInAndAcknowledgedAtIsNull(eq(1L), anyList()))
            .thenReturn(List.of(succeeded));

        assertFalse(experimentCopyCandidateService.resetFailedForRetry(1L));

        verify(experimentCopyCandidateRepository, never()).save(any());
    }

    // shared setup for recreating one candidate with a working LMS session
    private void stubWorkingRecreation(ExperimentCopyCandidate candidate, List<LmsAssignment> lmsAssignments) throws Exception {
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(candidate));
        when(apiClient.getLmsCourseId(ltiUserEntity, ltiContextEntity)).thenReturn(Optional.of("123"));
        when(assignmentService.getAllAssignmentsForLmsCourse(any(SecuredInfo.class))).thenReturn(lmsAssignments);
        when(experimentExportService.export(experiment)).thenReturn(ExportDto.builder().file(mock(File.class)).filename("export.zip").build());
        when(experimentImportService.preprocessFromFile(any(), any(), any(), any(LmsRepointTargets.class), eq(true))).thenReturn(importDto);
    }

    private LmsRepointTargets capturedRepointTargets() throws Exception {
        ArgumentCaptor<LmsRepointTargets> captor = ArgumentCaptor.forClass(LmsRepointTargets.class);
        verify(experimentImportService).preprocessFromFile(any(), any(), any(), captor.capture(), eq(true));

        return captor.getValue();
    }

    @Test
    void testRecreateCountsTheAttemptAndPassesTheCandidateToTheImport() throws Exception {
        ExperimentCopyCandidate candidate = pendingCandidate();
        when(candidate.getId()).thenReturn(5L);
        when(candidate.getAttempts()).thenReturn(1);
        stubWorkingRecreation(candidate, List.of());

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(candidate).setAttempts(2);
        assertEquals(5L, capturedRepointTargets().getCopyCandidateId());
    }

    // the first attempt matches by launch URL and saves what it found, for any retry to reuse
    @Test
    void testRecreateFirstAttemptSavesItsRepointPlan() throws Exception {
        ExperimentCopyCandidate candidate = pendingCandidate();
        LmsAssignment copied = LmsAssignment.builder()
            .id("999")
            .lmsExternalToolFields(LmsExternalToolFields.builder().url(LTI_URL + "/lti3?experiment=55&assignment=1").build())
            .build();
        stubWorkingRecreation(candidate, List.of(copied));

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(candidate).setRepointPlan("{\"assignments\":{\"1\":\"999\"},\"consentAssignment\":null}");
        assertEquals(Map.of(1L, copied), capturedRepointTargets().getAssignments());
    }

    // an interrupted attempt already changed the copied assignment's URL to point at an
    // assignment it then rolled back - so a retry can't match it by URL, only by the saved plan
    @Test
    void testRecreateRetryUsesTheSavedRepointPlan() throws Exception {
        ExperimentCopyCandidate candidate = pendingCandidate();
        when(candidate.getRepointPlan()).thenReturn("{\"assignments\":{\"1\":\"999\"},\"consentAssignment\":\"888\"}");
        LmsAssignment alreadyRepointed = LmsAssignment.builder()
            .id("999")
            .lmsExternalToolFields(LmsExternalToolFields.builder().url(LTI_URL + "/lti3?experiment=77&assignment=12345").build())
            .build();
        LmsAssignment consent = LmsAssignment.builder()
            .id("888")
            .lmsExternalToolFields(LmsExternalToolFields.builder().url(LTI_URL + "/lti3?consent=true&experiment=77").build())
            .build();
        stubWorkingRecreation(candidate, List.of(alreadyRepointed, consent));

        experimentCopyCandidateService.recreateForContext(1L, null);

        LmsRepointTargets repointTargets = capturedRepointTargets();
        assertEquals(Map.of(1L, alreadyRepointed), repointTargets.getAssignments());
        assertEquals(consent, repointTargets.getConsentAssignment());
        verify(candidate, never()).setRepointPlan(any());
    }

    @Test
    void testRecreateRetrySkipsPlannedAssignmentsDeletedFromTheLms() throws Exception {
        ExperimentCopyCandidate candidate = pendingCandidate();
        when(candidate.getRepointPlan()).thenReturn("{\"assignments\":{\"1\":\"999\"},\"consentAssignment\":null}");
        stubWorkingRecreation(candidate, List.of());

        experimentCopyCandidateService.recreateForContext(1L, null);

        assertTrue(capturedRepointTargets().getAssignments().isEmpty());
    }

    // an interrupted attempt's LMS assignments survive its rollback - removed first, or the
    // retry would leave duplicates
    @Test
    void testRecreateRemovesLmsAssignmentsLeftByAnEarlierAttempt() throws Exception {
        ExperimentCopyCandidate candidate = pendingCandidate();
        when(candidate.getId()).thenReturn(5L);
        ExperimentCopyCreatedAssignment leftOver = mock(ExperimentCopyCreatedAssignment.class);
        when(leftOver.getLmsAssignmentId()).thenReturn("444");
        ExperimentCopyCreatedAssignment alreadyGone = mock(ExperimentCopyCreatedAssignment.class);
        when(alreadyGone.getLmsAssignmentId()).thenReturn("445");
        List<ExperimentCopyCreatedAssignment> created = List.of(leftOver, alreadyGone);
        when(experimentCopyCreatedAssignmentRepository.findAllByCopyCandidate_Id(5L)).thenReturn(created);
        doThrow(new ApiException("not found")).when(apiClient).deleteAssignmentInLms(org.mockito.ArgumentMatchers.argThat((LmsAssignment a) -> a != null && "445".equals(a.getId())), eq("123"), any());
        stubWorkingRecreation(candidate, List.of());

        experimentCopyCandidateService.recreateForContext(1L, null);

        verify(apiClient).deleteAssignmentInLms(org.mockito.ArgumentMatchers.argThat((LmsAssignment a) -> a != null && "444".equals(a.getId())), eq("123"), eq(ltiUserEntity));
        verify(experimentCopyCreatedAssignmentRepository).deleteAll(created);
        verify(candidate).setStatus(ExperimentCopyCandidateStatus.IMPORTED);
    }

    @Test
    void testResetStalledForRecoveryRestartsStalledCandidates() {
        ExperimentCopyCandidate stalledPending = pendingCandidate();
        when(stalledPending.getDestinationContext()).thenReturn(ltiContextEntity);
        when(experimentCopyCandidateRepository.findAllByStatusInAndUpdatedAtBefore(eq(UNFINISHED), any(Timestamp.class))).thenReturn(List.of(stalledPending));

        ExperimentCopyCandidate deadImport = importedCandidate();
        when(deadImport.getDestinationContext()).thenReturn(ltiContextEntity);
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.PROCESSING);
        when(experimentImport.getUpdatedAt()).thenReturn(new Timestamp(0));
        when(experimentCopyCandidateRepository.findAllByStatusAndAcknowledgedAtIsNullAndUpdatedAtBefore(eq(ExperimentCopyCandidateStatus.IMPORTED), any(Timestamp.class)))
            .thenReturn(List.of(deadImport));

        Set<Long> contextIds = experimentCopyCandidateService.resetStalledForRecovery(Duration.ofMinutes(15), Duration.ofMinutes(60), 3);

        assertEquals(Set.of(1L), contextIds);
        verify(stalledPending).setStatus(ExperimentCopyCandidateStatus.PENDING);
        verify(deadImport).setStatus(ExperimentCopyCandidateStatus.PENDING);
        verify(deadImport).setResultingImportUuid(null);
        // abandoned, so it skips itself if it ever does start
        verify(experimentImport).setStatus(ExperimentImportStatus.ERROR_ACKNOWLEDGED);
        verify(experimentCopyNotificationService, never()).notifyLmsFailure(any());
    }

    @Test
    void testResetStalledForRecoveryLeavesImportsThatAreStillRunningOrDone() {
        ExperimentCopyCandidate done = importedCandidate();
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.COMPLETE);
        when(experimentCopyCandidateRepository.findAllByStatusAndAcknowledgedAtIsNullAndUpdatedAtBefore(eq(ExperimentCopyCandidateStatus.IMPORTED), any(Timestamp.class)))
            .thenReturn(List.of(done));

        assertTrue(experimentCopyCandidateService.resetStalledForRecovery(Duration.ofMinutes(15), Duration.ofMinutes(60), 3).isEmpty());

        verify(done, never()).setStatus(any());
        verify(experimentImport, never()).setStatus(any());
    }

    @Test
    void testResetStalledForRecoveryGivesUpAfterMaxAttemptsAndEmailsOncePerInstructor() {
        ExperimentCopyCandidate first = pendingCandidate();
        when(first.getAttempts()).thenReturn(3);
        ExperimentCopyCandidate second = pendingCandidate();
        when(second.getAttempts()).thenReturn(3);
        when(experimentCopyCandidateRepository.findAllByStatusInAndUpdatedAtBefore(eq(UNFINISHED), any(Timestamp.class))).thenReturn(List.of(first, second));

        assertTrue(experimentCopyCandidateService.resetStalledForRecovery(Duration.ofMinutes(15), Duration.ofMinutes(60), 3).isEmpty());

        verify(first).setStatus(ExperimentCopyCandidateStatus.ERROR);
        verify(second).setStatus(ExperimentCopyCandidateStatus.ERROR);
        verify(first).setErrorMessage("Recreation stopped part-way through and was already retried [3] time(s)");
        verify(experimentCopyNotificationService, times(1)).notifyLmsFailure(ltiUserEntity);
    }

    @Test
    void testHasUnfinishedForContextWhenPendingOrImporting() {
        when(experimentCopyCandidateRepository.existsByDestinationContext_ContextIdAndStatusIn(1L, UNFINISHED)).thenReturn(true);

        assertTrue(experimentCopyCandidateService.hasUnfinishedForContext(1L));
    }

    @Test
    void testHasUnfinishedForContextWhenImportStillProcessing() {
        ExperimentCopyCandidate candidate = importedCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.IMPORTED))
            .thenReturn(List.of(candidate));
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.PROCESSING);

        assertTrue(experimentCopyCandidateService.hasUnfinishedForContext(1L));
    }

    @Test
    void testHasUnfinishedForContextFalseOnceImportsFinish() {
        ExperimentCopyCandidate candidate = importedCandidate();
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.IMPORTED))
            .thenReturn(List.of(candidate));
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.COMPLETE);

        assertFalse(experimentCopyCandidateService.hasUnfinishedForContext(1L));
    }

    @Test
    void testGetCopyStatusNoneWhenNothingToShow() {
        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatusInAndAcknowledgedAtIsNull(eq(1L), anyList()))
            .thenReturn(List.of());

        CopyStatusDto result = experimentCopyCandidateService.getCopyStatus(securedInfo);

        assertEquals(ExperimentCopyStatus.NONE, result.getStatus());
        assertTrue(result.getImportIds().isEmpty());
    }

    @Test
    void testGetCopyStatusComplete() {
        ExperimentCopyCandidate candidate = importedCandidate();
        stubUnacknowledged(candidate);
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.COMPLETE);
        when(ltiContextEntity.getTitle()).thenReturn("Psych 101 Fall");

        CopyStatusDto result = experimentCopyCandidateService.getCopyStatus(securedInfo);

        assertEquals(ExperimentCopyStatus.COMPLETE, result.getStatus());
        assertEquals("Psych 101 Fall", result.getSourceCourseTitle());
        assertEquals(List.of(candidate.getResultingImportUuid()), result.getImportIds());
    }

    @Test
    void testGetCopyStatusInProgressWhileAnyCandidateIsUnfinished() {
        ExperimentCopyCandidate done = importedCandidate();
        ExperimentCopyCandidate pending = pendingCandidate();
        when(pending.getCreatedAt()).thenReturn(new Timestamp(0));
        stubUnacknowledged(done, pending);
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.COMPLETE);

        assertEquals(ExperimentCopyStatus.IN_PROGRESS, experimentCopyCandidateService.getCopyStatus(securedInfo).getStatus());
    }

    @Test
    void testGetCopyStatusInProgressWhileImportIsProcessing() {
        stubUnacknowledged(importedCandidate());
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.PROCESSING);

        assertEquals(ExperimentCopyStatus.IN_PROGRESS, experimentCopyCandidateService.getCopyStatus(securedInfo).getStatus());
    }

    @Test
    void testGetCopyStatusErrorWhenCandidateFailed() {
        ExperimentCopyCandidate failed = mock(ExperimentCopyCandidate.class);
        when(failed.getStatus()).thenReturn(ExperimentCopyCandidateStatus.ERROR);
        when(failed.getSourceExperiment()).thenReturn(experiment);
        stubUnacknowledged(failed);

        assertEquals(ExperimentCopyStatus.ERROR, experimentCopyCandidateService.getCopyStatus(securedInfo).getStatus());
    }

    @Test
    void testGetCopyStatusErrorWhenImportFailed() {
        stubUnacknowledged(importedCandidate());
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.ERROR);

        assertEquals(ExperimentCopyStatus.ERROR, experimentCopyCandidateService.getCopyStatus(securedInfo).getStatus());
    }

    @Test
    void testAcknowledgeCopyStatusMarksCandidatesAndTheirImportsAcknowledged() {
        ExperimentCopyCandidate candidate = importedCandidate();
        stubUnacknowledged(candidate);
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.COMPLETE);

        experimentCopyCandidateService.acknowledgeCopyStatus(securedInfo);

        verify(candidate).setAcknowledgedAt(any(Timestamp.class));
        verify(experimentCopyCandidateRepository).save(candidate);
        verify(experimentImport).setStatus(ExperimentImportStatus.COMPLETE_ACKNOWLEDGED);
    }

    @Test
    void testAcknowledgeCopyStatusMarksFailedImportsErrorAcknowledged() {
        stubUnacknowledged(importedCandidate());
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.ERROR);

        experimentCopyCandidateService.acknowledgeCopyStatus(securedInfo);

        verify(experimentImport).setStatus(ExperimentImportStatus.ERROR_ACKNOWLEDGED);
    }

    @Test
    void testAcknowledgeCopyStatusDoesNothingWhileInProgress() {
        ExperimentCopyCandidate candidate = pendingCandidate();
        stubUnacknowledged(candidate);

        experimentCopyCandidateService.acknowledgeCopyStatus(securedInfo);

        verify(candidate, never()).setAcknowledgedAt(any());
        verify(experimentCopyCandidateRepository, never()).save(any());
    }

    private static final List<ExperimentCopyCandidateStatus> UNFINISHED = List.of(
        ExperimentCopyCandidateStatus.PENDING,
        ExperimentCopyCandidateStatus.IMPORTING
    );

    private ExperimentCopyCandidate pendingCandidate() {
        ExperimentCopyCandidate candidate = mock(ExperimentCopyCandidate.class);
        when(candidate.getUuid()).thenReturn(UUID.randomUUID());
        when(candidate.getStatus()).thenReturn(ExperimentCopyCandidateStatus.PENDING);
        when(candidate.getSourceExperiment()).thenReturn(experiment);

        return candidate;
    }

    private ExperimentCopyCandidate importedCandidate() {
        ExperimentCopyCandidate candidate = mock(ExperimentCopyCandidate.class);
        when(candidate.getUuid()).thenReturn(UUID.randomUUID());
        when(candidate.getStatus()).thenReturn(ExperimentCopyCandidateStatus.IMPORTED);
        when(candidate.getSourceExperiment()).thenReturn(experiment);
        when(candidate.getResultingImportUuid()).thenReturn(UUID.randomUUID());
        when(candidate.getCreatedAt()).thenReturn(new Timestamp(1000));

        return candidate;
    }

    private void stubUnacknowledged(ExperimentCopyCandidate... candidates) {
        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatusInAndAcknowledgedAtIsNull(eq(1L), anyList()))
            .thenReturn(List.of(candidates));
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
        when(experimentImportService.preprocessFromFile(eq(exportFile), eq("export.zip"), eq(securedInfo), any(LmsRepointTargets.class), eq(false))).thenReturn(importDto);

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
        when(experimentImportService.preprocessFromFile(eq(exportFile), eq("export.zip"), eq(securedInfo), any(LmsRepointTargets.class), eq(false))).thenReturn(importDto);

        experimentCopyCandidateService.resolve(List.of(selectedId), securedInfo);
        Long candidateId = selected.getId();

        verify(experimentImportService).preprocessFromFile(
            eq(exportFile),
            eq("export.zip"),
            eq(securedInfo),
            eq(LmsRepointTargets.ofAssignments(Map.of(1L, matchingLmsAssignment)).toBuilder().copyCandidateId(candidateId).build()),
            eq(false)
        );
    }

    // the copied consent assignment has no assignment id of its own - matched by its experiment,
    // which can be the numeric id or the uuid depending on when the URL was written
    @Test
    void testResolveMatchesCopiedConsentAssignmentByNumericOrUuidExperimentId() throws Exception {
        UUID experimentUuid = UUID.randomUUID();
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.CONSENT);
        when(experiment.getUuid()).thenReturn(experimentUuid);

        for (String experimentParam : List.of("1", experimentUuid.toString())) {
            UUID selectedId = UUID.randomUUID();
            ExperimentCopyCandidate selected = mock(ExperimentCopyCandidate.class);
            when(selected.getUuid()).thenReturn(selectedId);
            when(selected.getSourceExperiment()).thenReturn(experiment);
            when(securedInfo.getContextId()).thenReturn(1L);
            when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
                .thenReturn(List.of(selected));

            LmsAssignment copiedConsent = LmsAssignment.builder()
                .id("888")
                .lmsExternalToolFields(LmsExternalToolFields.builder().url(LTI_URL + "/lti3?consent=true&experiment=" + experimentParam).build())
                .build();
            when(assignmentService.getAllAssignmentsForLmsCourse(securedInfo)).thenReturn(List.of(copiedConsent));

            File exportFile = mock(File.class);
            when(experimentExportService.export(experiment)).thenReturn(ExportDto.builder().file(exportFile).filename("export.zip").build());
            when(experimentImportService.preprocessFromFile(any(), any(), any(), any(LmsRepointTargets.class), eq(false))).thenReturn(importDto);

            experimentCopyCandidateService.resolve(List.of(selectedId), securedInfo);

            ArgumentCaptor<LmsRepointTargets> captor = ArgumentCaptor.forClass(LmsRepointTargets.class);
            verify(experimentImportService, org.mockito.Mockito.atLeastOnce()).preprocessFromFile(any(), any(), any(), captor.capture(), eq(false));
            assertEquals(copiedConsent, captor.getValue().getConsentAssignment(), experimentParam);
            assertTrue(captor.getValue().getAssignments().isEmpty());
        }
    }

    @Test
    void testResolveIgnoresConsentAssignmentForAnotherExperiment() throws Exception {
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.CONSENT);
        when(experiment.getUuid()).thenReturn(UUID.randomUUID());
        UUID selectedId = UUID.randomUUID();
        ExperimentCopyCandidate selected = mock(ExperimentCopyCandidate.class);
        when(selected.getUuid()).thenReturn(selectedId);
        when(selected.getSourceExperiment()).thenReturn(experiment);
        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentCopyCandidateRepository.findAllByDestinationContext_ContextIdAndStatus(1L, ExperimentCopyCandidateStatus.PENDING))
            .thenReturn(List.of(selected));
        LmsAssignment otherConsent = LmsAssignment.builder()
            .id("889")
            .lmsExternalToolFields(LmsExternalToolFields.builder().url(LTI_URL + "/lti3?consent=true&experiment=999").build())
            .build();
        when(assignmentService.getAllAssignmentsForLmsCourse(securedInfo)).thenReturn(List.of(otherConsent));
        when(experimentExportService.export(experiment)).thenReturn(ExportDto.builder().file(mock(File.class)).filename("export.zip").build());
        when(experimentImportService.preprocessFromFile(any(), any(), any(), any(LmsRepointTargets.class), eq(false))).thenReturn(importDto);

        experimentCopyCandidateService.resolve(List.of(selectedId), securedInfo);

        ArgumentCaptor<LmsRepointTargets> captor = ArgumentCaptor.forClass(LmsRepointTargets.class);
        verify(experimentImportService).preprocessFromFile(any(), any(), any(), captor.capture(), eq(false));
        assertNull(captor.getValue().getConsentAssignment());
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
        when(experimentImportService.preprocessFromFile(eq(exportFile), eq("export.zip"), eq(securedInfo), any(LmsRepointTargets.class), eq(false))).thenReturn(importDto);

        experimentCopyCandidateService.resolve(List.of(selectedId), securedInfo);

        verify(experimentImportService).preprocessFromFile(exportFile, "export.zip", securedInfo, LmsRepointTargets.none().toBuilder().copyCandidateId(selected.getId()).build(), false);
    }

}
