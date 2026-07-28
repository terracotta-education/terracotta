package edu.iu.terracotta.connectors.generic.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsConversation;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFile;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsCreateConversationOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetSingleConversationOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetUsersInCourseOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;

public class ApiClientImplTest extends BaseTest {

    private ApiClientImpl apiClientImpl;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // constructed manually (not via @InjectMocks): ApiClientImpl's constructor takes a raw
        // ConnectorService<ApiClient>, and multiple ConnectorService<?> mocks of other generic
        // parameterizations exist in BaseServiceTest, which would make @InjectMocks resolution
        // by type ambiguous.
        apiClientImpl = new ApiClientImpl(apiClientConnectorService);

        when(apiClientConnectorService.instance(any(PlatformDeployment.class), any())).thenReturn(apiClient);
    }

    @Test
    public void testCreateLmsAssignmentDelegatesThroughApiUser() throws Exception {
        when(apiClient.createLmsAssignment(ltiUserEntity, assignment, "course1")).thenReturn(lmsAssignment);

        LmsAssignment result = apiClientImpl.createLmsAssignment(ltiUserEntity, assignment, "course1");

        assertEquals(lmsAssignment, result);
        verify(apiClient).createLmsAssignment(ltiUserEntity, assignment, "course1");
    }

    @Test
    public void testListAssignmentsByLtiContextDelegatesThroughApiUser() throws Exception {
        when(apiClient.listAssignments(ltiUserEntity, ltiContextEntity)).thenReturn(List.of(lmsAssignment));

        List<?> result = apiClientImpl.listAssignments(ltiUserEntity, ltiContextEntity);

        assertEquals(List.of(lmsAssignment), result);
    }

    @Test
    public void testListAssignmentsByExperimentDelegatesThroughApiUser() throws Exception {
        when(apiClient.listAssignments(ltiUserEntity, experiment)).thenReturn(List.of(lmsAssignment));

        List<?> result = apiClientImpl.listAssignments(ltiUserEntity, experiment);

        assertEquals(List.of(lmsAssignment), result);
    }

    @Test
    public void testListAssignmentsByPlatformDeploymentDelegatesDirectly() throws Exception {
        when(apiClient.listAssignments(platformDeployment, "course1", "token")).thenReturn(List.of(lmsAssignment));

        List<?> result = apiClientImpl.listAssignments(platformDeployment, "course1", "token");

        assertEquals(List.of(lmsAssignment), result);
        verify(apiClientConnectorService).instance(platformDeployment, ApiClient.class);
    }

    @Test
    public void testCheckAssignmentExistsDelegatesThroughApiUser() throws Exception {
        when(apiClient.checkAssignmentExists(ltiUserEntity, "lmsAssignmentId", "course1")).thenReturn(Optional.of(lmsAssignment));

        Optional<?> result = apiClientImpl.checkAssignmentExists(ltiUserEntity, "lmsAssignmentId", "course1");

        assertEquals(Optional.of(lmsAssignment), result);
    }

    @Test
    public void testListAssignmentByLmsAssignmentIdDelegatesThroughApiUser() throws Exception {
        when(apiClient.listAssignment(ltiUserEntity, "course1", "lmsAssignmentId")).thenReturn(Optional.of(lmsAssignment));

        Optional<?> result = apiClientImpl.listAssignment(ltiUserEntity, "course1", "lmsAssignmentId");

        assertEquals(Optional.of(lmsAssignment), result);
    }

    @Test
    public void testListAssignmentByAssignmentDelegatesThroughApiUser() throws Exception {
        when(apiClient.listAssignment(ltiUserEntity, "course1", assignment)).thenReturn(Optional.of(lmsAssignment));

        Optional<?> result = apiClientImpl.listAssignment(ltiUserEntity, "course1", assignment);

        assertEquals(Optional.of(lmsAssignment), result);
    }

    @Test
    public void testEditAssignmentByApiUserDelegatesThroughApiUser() throws Exception {
        when(apiClient.editAssignment(ltiUserEntity, lmsAssignment, "course1")).thenReturn(Optional.of(lmsAssignment));

        Optional<?> result = apiClientImpl.editAssignment(ltiUserEntity, lmsAssignment, "course1");

        assertEquals(Optional.of(lmsAssignment), result);
    }

    @Test
    public void testEditAssignmentByPlatformDeploymentDelegatesDirectly() throws Exception {
        when(apiClient.editAssignment(platformDeployment, lmsAssignment, "course1", "token")).thenReturn(Optional.of(lmsAssignment));

        Optional<?> result = apiClientImpl.editAssignment(platformDeployment, lmsAssignment, "course1", "token");

        assertEquals(Optional.of(lmsAssignment), result);
    }

    @Test
    public void testRestoreAssignmentDelegatesThroughAssignmentsExperiment() throws Exception {
        when(apiClient.restoreAssignment(assignment)).thenReturn(lmsAssignment);

        Object result = apiClientImpl.restoreAssignment(assignment);

        assertEquals(lmsAssignment, result);
        // assignment -> exposure -> experiment -> platformDeployment resolution chain
        verify(apiClientConnectorService).instance(platformDeployment, ApiClient.class);
    }

    @Test
    public void testEditAssignmentNameInLmsDelegatesThroughAssignmentsExperiment() throws Exception {
        apiClientImpl.editAssignmentNameInLms(assignment, "course1", "newName", ltiUserEntity);

        verify(apiClient).editAssignmentNameInLms(assignment, "course1", "newName", ltiUserEntity);
    }

    @Test
    public void testDeleteAssignmentInLmsByAssignmentDelegatesThroughAssignmentsExperiment() throws Exception {
        apiClientImpl.deleteAssignmentInLms(assignment, "course1", ltiUserEntity);

        verify(apiClient).deleteAssignmentInLms(assignment, "course1", ltiUserEntity);
    }

    @Test
    public void testDeleteAssignmentInLmsByLmsAssignmentDelegatesThroughApiUser() throws Exception {
        apiClientImpl.deleteAssignmentInLms(lmsAssignment, "course1", ltiUserEntity);

        verify(apiClient).deleteAssignmentInLms(lmsAssignment, "course1", ltiUserEntity);
    }

    @Test
    public void testUploadConsentFileDelegatesThroughApiUser() throws Exception {
        when(apiClient.uploadConsentFile(experiment, consentDocument, ltiUserEntity)).thenReturn(lmsAssignment);

        Object result = apiClientImpl.uploadConsentFile(experiment, consentDocument, ltiUserEntity);

        assertEquals(lmsAssignment, result);
    }

    @Test
    public void testResyncAssignmentTargetUrisInLmsDelegatesDirectly() throws Exception {
        apiClientImpl.resyncAssignmentTargetUrisInLms(platformDeployment, ltiUserEntity, 1L, "token", List.of("a"), List.of("b"), List.of("a", "b"));

        verify(apiClient).resyncAssignmentTargetUrisInLms(platformDeployment, ltiUserEntity, 1L, "token", List.of("a"), List.of("b"), List.of("a", "b"));
    }

    @Test
    public void testUpdateAssignmentMetadataDelegatesThroughAssignmentsExperiment() throws Exception {
        apiClientImpl.updateAssignmentMetadata(assignment, lmsAssignment);

        verify(apiClient).updateAssignmentMetadata(assignment, lmsAssignment);
    }

    @Test
    public void testListCoursesForUserDelegatesDirectly() throws Exception {
        when(apiClient.listCoursesForUser(platformDeployment, "lmsUserId", "token")).thenReturn(List.of(lmsCourse));

        List<?> result = apiClientImpl.listCoursesForUser(platformDeployment, "lmsUserId", "token");

        assertEquals(List.of(lmsCourse), result);
    }

    @Test
    public void testAddLmsExtensionsDelegatesThroughSubmissionsExperiment() throws Exception {
        Score score = Score.builder().build();

        apiClientImpl.addLmsExtensions(score, submission, true);

        verify(apiClient).addLmsExtensions(score, submission, true);
        // submission -> assessment -> treatment -> condition -> experiment -> platformDeployment
        verify(apiClientConnectorService).instance(platformDeployment, ApiClient.class);
    }

    @Test
    public void testListSubmissionsByOutcomeDelegatesThroughApiUser() throws Exception {
        when(apiClient.listSubmissions(ltiUserEntity, outcome, "course1")).thenReturn(List.of(lmsSubmission));

        List<?> result = apiClientImpl.listSubmissions(ltiUserEntity, outcome, "course1");

        assertEquals(List.of(lmsSubmission), result);
    }

    @Test
    public void testListSubmissionsByLmsAssignmentIdDelegatesThroughApiUser() throws Exception {
        when(apiClient.listSubmissions(ltiUserEntity, "lmsAssignmentId", "course1")).thenReturn(List.of(lmsSubmission));

        List<?> result = apiClientImpl.listSubmissions(ltiUserEntity, "lmsAssignmentId", "course1");

        assertEquals(List.of(lmsSubmission), result);
    }

    @Test
    public void testListSubmissionsForMultipleAssignmentsDelegatesThroughApiUser() throws Exception {
        when(apiClient.listSubmissionsForMultipleAssignments(ltiUserEntity, "course1", List.of("a1", "a2"))).thenReturn(List.of(lmsSubmission));

        List<?> result = apiClientImpl.listSubmissionsForMultipleAssignments(ltiUserEntity, "course1", List.of("a1", "a2"));

        assertEquals(List.of(lmsSubmission), result);
    }

    @Test
    public void testSendConversationDelegatesThroughApiUsersPlatformDeployment() throws Exception {
        LmsCreateConversationOptions options = LmsCreateConversationOptions.builder().build();
        LmsConversation lmsConversation = mock(LmsConversation.class);
        when(apiClient.sendConversation(options, ltiUserEntity)).thenReturn(List.of(lmsConversation));

        List<?> result = apiClientImpl.sendConversation(options, ltiUserEntity);

        assertEquals(List.of(lmsConversation), result);
    }

    @Test
    public void testGetConversationDelegatesThroughApiUsersPlatformDeployment() throws Exception {
        LmsGetSingleConversationOptions options = LmsGetSingleConversationOptions.builder().build();
        LmsConversation lmsConversation = mock(LmsConversation.class);
        when(apiClient.getConversation(options, ltiUserEntity)).thenReturn(Optional.of(lmsConversation));

        Optional<?> result = apiClientImpl.getConversation(options, ltiUserEntity);

        assertEquals(Optional.of(lmsConversation), result);
    }

    @Test
    public void testListUsersForCourseDelegatesThroughApiUsersPlatformDeployment() throws Exception {
        LmsGetUsersInCourseOptions options = LmsGetUsersInCourseOptions.builder().build();
        LmsUser lmsUser = mock(LmsUser.class);
        when(apiClient.listUsersForCourse(options, ltiUserEntity)).thenReturn(List.of(lmsUser));

        List<?> result = apiClientImpl.listUsersForCourse(options, ltiUserEntity);

        assertEquals(List.of(lmsUser), result);
    }

    @Test
    public void testGetFileDelegatesThroughApiUsersPlatformDeployment() throws Exception {
        LmsFile lmsFile = mock(LmsFile.class);
        when(apiClient.getFile(ltiUserEntity, "fileId")).thenReturn(Optional.of(lmsFile));

        Optional<?> result = apiClientImpl.getFile(ltiUserEntity, "fileId");

        assertEquals(Optional.of(lmsFile), result);
    }

    @Test
    public void testGetFilesDelegatesThroughApiUsersPlatformDeployment() throws Exception {
        LmsFile lmsFile = mock(LmsFile.class);
        when(apiClient.getFiles(ltiUserEntity)).thenReturn(List.of(lmsFile));

        List<?> result = apiClientImpl.getFiles(ltiUserEntity);

        assertEquals(List.of(lmsFile), result);
    }

    @Test
    public void testConnectorServiceFailurePropagates() throws Exception {
        when(apiClientConnectorService.instance(any(PlatformDeployment.class), any())).thenThrow(new TerracottaConnectorException("boom"));

        assertThrows(TerracottaConnectorException.class, () -> apiClientImpl.createLmsAssignment(ltiUserEntity, assignment, "course1"));
    }

}
