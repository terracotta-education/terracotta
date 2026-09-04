package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.service.api.impl.ApiClientImpl;
import edu.iu.terracotta.dao.repository.AdminUserRepository;

@SuppressWarnings("deprecation")
public class AdminServiceImplTest extends BaseTest {

    @Mock private AdminUserRepository adminUserRepository;
    @Mock private ApiClientImpl apiClientImpl;

    @InjectMocks private AdminServiceImpl adminService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(ltiUserEntity.getLmsUserId()).thenReturn("instructorLmsId");
        when(assignmentRepository.findAllByExposure_Experiment_PlatformDeployment_KeyId(anyLong())).thenReturn(List.of(assignment));
        when(consentDocumentRepository.findAllByExperiment_PlatformDeployment_KeyId(anyLong())).thenReturn(List.of(consentDocument));
        when(ltiMembershipRepository.findByRoleAndContext_ToolDeployment_PlatformDeployment_KeyId(eq(1), anyLong())).thenReturn(List.of(ltiMembershipEntity));
    }

    @Test
    public void testResyncTargetUrisPlatformDeploymentNotFound() throws Exception {
        when(platformDeploymentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> adminService.resyncTargetUris(1L, "token"));

        verify(ltiMembershipRepository, never()).findByRoleAndContext_ToolDeployment_PlatformDeployment_KeyId(anyInt(), anyLong());
    }

    @Test
    public void testResyncTargetUrisNoInstructors() throws Exception {
        when(ltiMembershipRepository.findByRoleAndContext_ToolDeployment_PlatformDeployment_KeyId(eq(1), anyLong())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> adminService.resyncTargetUris(1L, "token"));

        verify(assignmentRepository, never()).findAllByExposure_Experiment_PlatformDeployment_KeyId(anyLong());
    }

    @Test
    public void testResyncTargetUrisNoAssignmentsFound() throws Exception {
        when(assignmentRepository.findAllByExposure_Experiment_PlatformDeployment_KeyId(anyLong())).thenReturn(Collections.emptyList());
        when(consentDocumentRepository.findAllByExperiment_PlatformDeployment_KeyId(anyLong())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> adminService.resyncTargetUris(1L, "token"));

        verify(apiClientImpl, never()).listCoursesForUser(any(), anyString(), anyString());
    }

    @Test
    public void testResyncTargetUrisNoCoursesForInstructor() throws Exception {
        when(apiClientImpl.listCoursesForUser(any(), anyString(), anyString())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> adminService.resyncTargetUris(1L, "token"));

        verify(apiClientImpl, never()).resyncAssignmentTargetUrisInLms(any(), any(), anyLong(), anyString(), any(), any(), any());
    }

    @Test
    public void testResyncTargetUrisSuccess() throws Exception {
        when(apiClientImpl.listCoursesForUser(any(), anyString(), anyString())).thenReturn(List.of(lmsCourse));

        assertDoesNotThrow(() -> adminService.resyncTargetUris(1L, "token"));

        verify(apiClientImpl, times(1)).resyncAssignmentTargetUrisInLms(eq(platformDeployment), eq(null), eq(1L), eq("token"), any(), any(), any());
    }

    @Test
    public void testResyncTargetUrisListCoursesThrowsHandledGracefully() throws Exception {
        when(apiClientImpl.listCoursesForUser(any(), anyString(), anyString())).thenThrow(new ApiException("lms error"));

        assertDoesNotThrow(() -> adminService.resyncTargetUris(1L, "token"));

        verify(apiClientImpl, never()).resyncAssignmentTargetUrisInLms(any(), any(), anyLong(), anyString(), any(), any(), any());
    }

    @Test
    public void testResyncTargetUrisResyncThrowsHandledGracefully() throws Exception {
        when(apiClientImpl.listCoursesForUser(any(), anyString(), anyString())).thenReturn(List.of(lmsCourse));
        doThrow(new ApiException("resync error")).when(apiClientImpl).resyncAssignmentTargetUrisInLms(any(), any(), anyLong(), anyString(), any(), any(), any());

        assertDoesNotThrow(() -> adminService.resyncTargetUris(1L, "token"));
    }

    @Test
    public void testIsTerracottaAdminTrue() {
        when(adminUserRepository.existsByLtiUserEntity_UserKeyAndEnabledTrue(anyString())).thenReturn(true);

        assertTrue(adminService.isTerracottaAdmin("userKey"));
    }

    @Test
    public void testIsTerracottaAdminFalse() {
        when(adminUserRepository.existsByLtiUserEntity_UserKeyAndEnabledTrue(anyString())).thenReturn(false);

        assertFalse(adminService.isTerracottaAdmin("userKey"));
    }

}
