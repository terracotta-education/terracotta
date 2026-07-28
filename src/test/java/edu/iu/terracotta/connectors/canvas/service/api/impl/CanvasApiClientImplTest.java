package edu.iu.terracotta.connectors.canvas.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.canvas.dao.model.extended.AssignmentExtended;
import edu.iu.terracotta.connectors.canvas.dao.model.extended.ConversationExtended;
import edu.iu.terracotta.connectors.canvas.dao.model.extended.FileExtended;
import edu.iu.terracotta.connectors.canvas.dao.model.extended.FolderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.AssignmentReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.AssignmentWriterExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.ConversationReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.ConversationWriterExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.CourseReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.FileReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.FolderReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.SubmissionReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.UserReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.impl.CanvasApiFactoryExtended;
import edu.iu.terracotta.connectors.canvas.service.lms.impl.CanvasLmsUtilsImpl;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsConversation;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFile;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsCreateConversationOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetSingleConversationOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetUsersInCourseOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentState;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentType;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.ksu.canvas.exception.CanvasException;
import edu.ksu.canvas.exception.ObjectNotFoundException;
import edu.ksu.canvas.model.assignment.Assignment;

public class CanvasApiClientImplTest extends BaseTest {

    @Mock private CourseReaderExtended courseReaderExtended;
    @Mock private SubmissionReaderExtended submissionReaderExtended;
    @Mock private ConversationReaderExtended conversationReaderExtended;
    @Mock private ConversationWriterExtended conversationWriterExtended;
    @Mock private UserReaderExtended userReaderExtended;
    @Mock private FileReaderExtended fileReaderExtended;
    @Mock private FolderReaderExtended folderReaderExtended;
    @Mock private ConversationExtended conversationExtendedMock;
    @Mock private FileExtended fileExtendedMock;

    private CanvasApiClientImpl canvasApiClientService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        canvasApiClientService = new CanvasApiClientImpl(canvasLmsOAuthService, new CanvasLmsUtilsImpl());
    }

    /**
     * The production class always creates a brand new CanvasApiFactoryExtended via {@code new
     * CanvasApiFactoryExtended(...)} instead of using an injected factory, so the only way to avoid
     * real HTTP calls in a unit test is to intercept construction and redirect getReader()/getWriter()
     * calls to the shared mocks below.
     */
    private MockedConstruction<CanvasApiFactoryExtended> mockApiFactory() {
        return mockConstruction(
            CanvasApiFactoryExtended.class,
            (mock, context) -> {
                when(mock.getReader(eq(AssignmentReaderExtended.class), any())).thenReturn(assignmentReaderExtended);
                when(mock.getWriter(eq(AssignmentWriterExtended.class), any())).thenReturn(assignmentWriterExtended);
                when(mock.getReader(eq(CourseReaderExtended.class), any())).thenReturn(courseReaderExtended);
                when(mock.getReader(eq(SubmissionReaderExtended.class), any())).thenReturn(submissionReaderExtended);
                when(mock.getReader(eq(ConversationReaderExtended.class), any())).thenReturn(conversationReaderExtended);
                when(mock.getWriter(eq(ConversationWriterExtended.class), any())).thenReturn(conversationWriterExtended);
                when(mock.getReader(eq(UserReaderExtended.class), any())).thenReturn(userReaderExtended);
                when(mock.getReader(eq(FileReaderExtended.class), any())).thenReturn(fileReaderExtended);
                when(mock.getReader(eq(FolderReaderExtended.class), any())).thenReturn(folderReaderExtended);
            }
        );
    }

    // createLmsAssignment

    @Test
    void testCreateLmsAssignmentSuccess() throws Exception {
        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        when(assignmentWriterExtended.createAssignment(eq("course123"), captor.capture())).thenReturn(Optional.of(canvasAssignmentExtended));

        AssignmentExtended result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.createLmsAssignment(ltiUserEntity, assignment, "course123");
        }

        assertEquals(canvasAssignmentExtended, result);
        Assignment sent = captor.getValue();
        assertEquals(ASSIGNMENT_TITLE, sent.getName());
        assertFalse(sent.isPublished());
        assertEquals("percent", sent.getGradingType());
        assertEquals(Double.valueOf(100.0), sent.getPointsPossible());
        assertEquals(List.of("external_tool"), sent.getSubmissionTypes());
        assertTrue(sent.getExternalToolTagAttributes().getUrl().contains("/lti3?experiment=1&assignment=1"));
    }

    @Test
    void testCreateLmsAssignmentThrowsWhenWriterReturnsEmpty() throws Exception {
        when(assignmentWriterExtended.createAssignment(any(), any())).thenReturn(Optional.empty());

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            ApiException ex = assertThrows(ApiException.class, () -> canvasApiClientService.createLmsAssignment(ltiUserEntity, assignment, "course123"));
            assertTrue(ex.getMessage().contains("course123"));
        }
    }

    @Test
    void testCreateLmsAssignmentWrapsIOException() throws Exception {
        when(assignmentWriterExtended.createAssignment(any(), any())).thenThrow(new IOException("network down"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.createLmsAssignment(ltiUserEntity, assignment, "course123"));
        }
    }

    // restoreAssignment

    @Test
    void testRestoreAssignmentDelegatesToCreateLmsAssignment() throws Exception {
        when(assignmentWriterExtended.createAssignment(eq("1"), any())).thenReturn(Optional.of(canvasAssignmentExtended));

        AssignmentExtended result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.restoreAssignment(assignment);
        }

        assertEquals(canvasAssignmentExtended, result);
    }

    // listAssignments(apiUser, ltiContext)

    @Test
    void testListAssignmentsByLtiContextSuccess() throws Exception {
        when(assignmentReaderExtended.listCourseAssignments(any())).thenReturn(List.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);

        List<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listAssignments(ltiUserEntity, ltiContextEntity);
        }

        assertEquals(1, result.size());
        assertEquals(lmsAssignment, result.get(0));
    }

    @Test
    void testListAssignmentsByLtiContextWrapsException() throws Exception {
        when(assignmentReaderExtended.listCourseAssignments(any())).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.listAssignments(ltiUserEntity, ltiContextEntity));
        }
    }

    // listAssignments(apiUser, experiment) -- also used below to exercise getAccessToken() branches

    @Test
    void testListAssignmentsByExperimentSuccess() throws Exception {
        when(assignmentReaderExtended.listCourseAssignments(any())).thenReturn(List.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);

        List<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listAssignments(ltiUserEntity, experiment);
        }

        assertEquals(1, result.size());
        verify(canvasLmsOAuthService).getAccessToken(ltiUserEntity);
    }

    @Test
    void testListAssignmentsByExperimentSwallowsExceptionFromFromConversion() throws Exception {
        when(assignmentReaderExtended.listCourseAssignments(any())).thenReturn(List.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenThrow(new RuntimeException("bad data"));

        List<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listAssignments(ltiUserEntity, experiment);
        }

        assertTrue(result.isEmpty());
    }

    @Test
    void testListAssignmentsByExperimentWrapsLmsOAuthExceptionFromTokenRetrieval() throws LmsOAuthException {
        when(canvasLmsOAuthService.getAccessToken(ltiUserEntity)).thenThrow(new LmsOAuthException("no token"));

        ApiException ex = assertThrows(ApiException.class, () -> canvasApiClientService.listAssignments(ltiUserEntity, experiment));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof ApiException);
        assertTrue(ex.getCause().getCause() instanceof LmsOAuthException);
    }

    @Test
    void testListAssignmentsByExperimentUsesAdminApiTokenWhenOAuthNotConfigured() throws Exception {
        when(canvasLmsOAuthService.isConfigured(platformDeployment)).thenReturn(false);
        when(platformDeployment.getApiToken()).thenReturn("admin-token");
        when(assignmentReaderExtended.listCourseAssignments(any())).thenReturn(List.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);

        List<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listAssignments(ltiUserEntity, experiment);
        }

        assertEquals(1, result.size());
        verify(canvasLmsOAuthService, never()).getAccessToken(any());
    }

    @Test
    void testListAssignmentsByExperimentThrowsWhenNoTokenAvailableAtAll() {
        when(canvasLmsOAuthService.isConfigured(platformDeployment)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> canvasApiClientService.listAssignments(ltiUserEntity, experiment));
        // the outer catch in listAssignments wraps every failure into a generic message, preserving
        // the specific "no token available" message from getAccessToken() as the cause instead
        assertTrue(ex.getCause().getMessage().contains("Could not get a Canvas API token"));
    }

    @Test
    void testListAssignmentsByExperimentWithTokenLoggingEnabled() throws Exception {
        ReflectionTestUtils.setField(canvasApiClientService, "tokenLoggingEnabled", true);
        when(assignmentReaderExtended.listCourseAssignments(any())).thenReturn(List.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);

        List<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listAssignments(ltiUserEntity, experiment);
        }

        assertEquals(1, result.size());
    }

    // listAssignments(platformDeployment, canvasCourseId, tokenOverride)

    @Test
    void testListAssignmentsByPlatformDeploymentSuccess() throws Exception {
        when(assignmentReaderExtended.listCourseAssignments(any())).thenReturn(List.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);

        List<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listAssignments(platformDeployment, "course1", "tok-override");
        }

        assertEquals(1, result.size());
    }

    @Test
    void testListAssignmentsByPlatformDeploymentWrapsException() throws Exception {
        when(assignmentReaderExtended.listCourseAssignments(any())).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.listAssignments(platformDeployment, "course1", "tok-override"));
        }
    }

    @Test
    void testListAssignmentsByPlatformDeploymentWithTokenLoggingEnabled() throws Exception {
        ReflectionTestUtils.setField(canvasApiClientService, "tokenLoggingEnabled", true);
        when(assignmentReaderExtended.listCourseAssignments(any())).thenReturn(List.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);

        List<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listAssignments(platformDeployment, "course1", "tok-override");
        }

        assertEquals(1, result.size());
    }

    // listAssignment(apiUser, canvasCourseId, canvasAssignmentId)

    @Test
    void testListAssignmentSuccess() throws Exception {
        when(assignmentReaderExtended.getSingleAssignment(any())).thenReturn(Optional.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);

        Optional<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listAssignment(ltiUserEntity, "course1", "5");
        }

        assertTrue(result.isPresent());
        assertEquals(lmsAssignment, result.get());
    }

    @Test
    void testListAssignmentReturnsEmptyWhenNotFound() throws Exception {
        when(assignmentReaderExtended.getSingleAssignment(any())).thenThrow(new ObjectNotFoundException());

        Optional<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listAssignment(ltiUserEntity, "course1", "5");
        }

        assertTrue(result.isEmpty());
    }

    @Test
    void testListAssignmentWrapsOtherExceptions() throws Exception {
        when(assignmentReaderExtended.getSingleAssignment(any())).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.listAssignment(ltiUserEntity, "course1", "5"));
        }
    }

    @Test
    void testListAssignmentSwallowsExceptionFromFromConversion() throws Exception {
        when(assignmentReaderExtended.getSingleAssignment(any())).thenReturn(Optional.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenThrow(new RuntimeException("bad data"));

        Optional<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listAssignment(ltiUserEntity, "course1", "5");
        }

        assertTrue(result.isEmpty());
    }

    // listAssignment(apiUser, lmsCourseId, Assignment) delegate

    @Test
    void testListAssignmentDelegatesToOverloadUsingAssignmentEntity() throws Exception {
        when(assignment.getLmsAssignmentId()).thenReturn("5");
        when(assignmentReaderExtended.getSingleAssignment(any())).thenReturn(Optional.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);

        Optional<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listAssignment(ltiUserEntity, "course1", assignment);
        }

        assertTrue(result.isPresent());
        assertEquals(lmsAssignment, result.get());
    }

    // checkAssignmentExists (duplicates listAssignment's implementation almost verbatim)

    @Test
    void testCheckAssignmentExistsSuccess() throws Exception {
        when(assignmentReaderExtended.getSingleAssignment(any())).thenReturn(Optional.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);

        Optional<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.checkAssignmentExists(ltiUserEntity, "5", "course1");
        }

        assertTrue(result.isPresent());
    }

    @Test
    void testCheckAssignmentExistsReturnsEmptyWhenNotFound() throws Exception {
        when(assignmentReaderExtended.getSingleAssignment(any())).thenThrow(new ObjectNotFoundException());

        Optional<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.checkAssignmentExists(ltiUserEntity, "5", "course1");
        }

        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckAssignmentExistsWrapsOtherExceptions() throws Exception {
        when(assignmentReaderExtended.getSingleAssignment(any())).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.checkAssignmentExists(ltiUserEntity, "5", "course1"));
        }
    }

    // editAssignment(apiUser, lmsAssignment, canvasCourseId)

    @Test
    void testEditAssignmentSuccess() throws Exception {
        when(lmsAssignment.getPointsPossible()).thenReturn(100.0f);
        when(assignmentWriterExtended.editAssignment(any(), any())).thenReturn(Optional.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);

        Optional<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.editAssignment(ltiUserEntity, lmsAssignment, "course1");
        }

        assertTrue(result.isPresent());
    }

    @Test
    void testEditAssignmentWrapsException() throws Exception {
        when(lmsAssignment.getPointsPossible()).thenReturn(100.0f);
        when(assignmentWriterExtended.editAssignment(any(), any())).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.editAssignment(ltiUserEntity, lmsAssignment, "course1"));
        }
    }

    // editAssignment(platformDeployment, lmsAssignment, canvasCourseId, tokenOverride)

    @Test
    void testEditAssignmentWithTokenOverrideSuccess() throws Exception {
        when(lmsAssignment.getPointsPossible()).thenReturn(100.0f);
        when(assignmentWriterExtended.editAssignment(any(), any())).thenReturn(Optional.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);

        Optional<LmsAssignment> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.editAssignment(platformDeployment, lmsAssignment, "course1", "tok-override");
        }

        assertTrue(result.isPresent());
    }

    @Test
    void testEditAssignmentWithTokenOverrideWrapsException() throws Exception {
        when(lmsAssignment.getPointsPossible()).thenReturn(100.0f);
        when(assignmentWriterExtended.editAssignment(any(), any())).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.editAssignment(platformDeployment, lmsAssignment, "course1", "tok-override"));
        }
    }

    // editAssignmentNameInLms

    @Test
    void testEditAssignmentNameInLmsWhenAssignmentAlreadyDeleted() throws Exception {
        when(assignment.getLmsAssignmentId()).thenReturn("55");
        when(assignmentReaderExtended.getSingleAssignment(any())).thenReturn(Optional.empty());

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            canvasApiClientService.editAssignmentNameInLms(assignment, "course1", "New Name", ltiUserEntity);
        }

        verify(assignmentWriterExtended, never()).editAssignment(any(), any());
    }

    @Test
    void testEditAssignmentNameInLmsUpdatesNameWhenAssignmentExists() throws Exception {
        when(assignment.getLmsAssignmentId()).thenReturn("55");
        when(assignmentReaderExtended.getSingleAssignment(any())).thenReturn(Optional.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);
        when(lmsAssignment.getPointsPossible()).thenReturn(100.0f);
        when(assignmentWriterExtended.editAssignment(any(), any())).thenReturn(Optional.of(canvasAssignmentExtended));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            canvasApiClientService.editAssignmentNameInLms(assignment, "course1", "New Name", ltiUserEntity);
        }

        verify(lmsAssignment).setName("New Name");
        verify(assignmentWriterExtended, times(1)).editAssignment(eq("course1"), any());
    }

    // deleteAssignmentInLms(Assignment, ...)

    @Test
    void testDeleteAssignmentInLmsByAssignmentWhenAlreadyDeleted() throws Exception {
        when(assignment.getLmsAssignmentId()).thenReturn("77");
        when(assignmentReaderExtended.getSingleAssignment(any())).thenReturn(Optional.empty());

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            canvasApiClientService.deleteAssignmentInLms(assignment, "course1", ltiUserEntity);
        }

        verify(assignmentWriterExtended, never()).deleteAssignment(any(), any());
    }

    @Test
    void testDeleteAssignmentInLmsByAssignmentDelegatesWhenPresent() throws Exception {
        when(assignment.getLmsAssignmentId()).thenReturn("77");
        when(assignmentReaderExtended.getSingleAssignment(any())).thenReturn(Optional.of(canvasAssignmentExtended));
        when(canvasAssignmentExtended.from()).thenReturn(lmsAssignment);
        when(lmsAssignment.getId()).thenReturn("77");

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            canvasApiClientService.deleteAssignmentInLms(assignment, "course1", ltiUserEntity);
        }

        verify(assignmentWriterExtended, times(1)).deleteAssignment("course1", 77L);
    }

    // deleteAssignmentInLms(LmsAssignment, ...)

    @Test
    void testDeleteAssignmentInLmsSuccess() throws Exception {
        when(lmsAssignment.getId()).thenReturn("99");

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            canvasApiClientService.deleteAssignmentInLms(lmsAssignment, "course1", ltiUserEntity);
        }

        verify(assignmentWriterExtended, times(1)).deleteAssignment("course1", 99L);
    }

    @Test
    void testDeleteAssignmentInLmsWrapsException() throws Exception {
        when(lmsAssignment.getId()).thenReturn("99");
        when(assignmentWriterExtended.deleteAssignment(any(), any())).thenThrow(new IOException("boom"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            ApiException ex = assertThrows(ApiException.class, () -> canvasApiClientService.deleteAssignmentInLms(lmsAssignment, "course1", ltiUserEntity));
            assertTrue(ex.getMessage().contains("99"));
        }
    }

    // uploadConsentFile

    @Test
    void testUploadConsentFileSuccessUsesFallbackUrl() throws Exception {
        when(consentDocument.getTitle()).thenReturn("Consent Title");
        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        when(assignmentWriterExtended.createAssignment(eq("1"), captor.capture())).thenReturn(Optional.of(canvasAssignmentExtended));

        AssignmentExtended result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.uploadConsentFile(experiment, consentDocument, ltiUserEntity);
        }

        assertEquals(canvasAssignmentExtended, result);
        Assignment sent = captor.getValue();
        assertEquals("Consent Title", sent.getName());
        assertEquals("", sent.getDescription());
        assertEquals("points", sent.getGradingType());
        assertEquals(Double.valueOf(1.0), sent.getPointsPossible());
        assertEquals(List.of("external_tool"), sent.getSubmissionTypes());
        assertTrue(sent.getExternalToolTagAttributes().getUrl().contains("consent=true&experiment=1"));
    }

    @Test
    void testUploadConsentFileWrapsApiExceptionWhenCreateFails() throws Exception {
        when(consentDocument.getTitle()).thenReturn("Consent Title");
        when(assignmentWriterExtended.createAssignment(any(), any())).thenReturn(Optional.empty());

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            ApiException ex = assertThrows(ApiException.class, () -> canvasApiClientService.uploadConsentFile(experiment, consentDocument, ltiUserEntity));
            assertTrue(ex.getMessage().contains("Error 137"));
        }
    }

    @Test
    void testUploadConsentFilePropagatesIOExceptionUnwrapped() throws Exception {
        when(consentDocument.getTitle()).thenReturn("Consent Title");
        when(assignmentWriterExtended.createAssignment(any(), any())).thenThrow(new IOException("network down"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(IOException.class, () -> canvasApiClientService.uploadConsentFile(experiment, consentDocument, ltiUserEntity));
        }
    }

    // resyncAssignmentTargetUrisInLms

    @Test
    void testResyncAssignmentTargetUrisInLmsSwallowsApiExceptionFromListing() throws Exception {
        when(assignmentReaderExtended.listCourseAssignments(any())).thenThrow(new IOException("down"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertDoesNotThrow(
                () -> canvasApiClientService.resyncAssignmentTargetUrisInLms(platformDeployment, ltiUserEntity, 1L, "token", List.of("1"), List.of(), List.of("1"))
            );
        }

        verify(assignmentWriterExtended, never()).editAssignment(any(), any());
    }

    @Test
    void testResyncAssignmentTargetUrisInLmsHandlesEmptyCanvasAssignmentList() throws Exception {
        when(assignmentReaderExtended.listCourseAssignments(any())).thenReturn(List.of());

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertDoesNotThrow(
                () -> canvasApiClientService.resyncAssignmentTargetUrisInLms(platformDeployment, ltiUserEntity, 1L, "token", List.of("1"), List.of(), List.of("1"))
            );
        }

        verify(assignmentWriterExtended, never()).editAssignment(any(), any());
    }

    @Test
    void testResyncAssignmentTargetUrisInLmsFiltersOutAssignmentsNotInAllAssignmentIds() throws Exception {
        Assignment canvasAssignment = new Assignment();
        canvasAssignment.setId(202L);
        AssignmentExtended notMatching = AssignmentExtended.builder().assignment(canvasAssignment).build();
        when(assignmentReaderExtended.listCourseAssignments(any())).thenReturn(List.of(notMatching));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertDoesNotThrow(
                () -> canvasApiClientService.resyncAssignmentTargetUrisInLms(platformDeployment, ltiUserEntity, 1L, "token", List.of("1"), List.of(), List.of("999"))
            );
        }

        verify(assignmentWriterExtended, never()).editAssignment(any(), any());
    }

    // updateAssignmentMetadata

    @Test
    void testUpdateAssignmentMetadataIsNoOp() {
        assertDoesNotThrow(() -> canvasApiClientService.updateAssignmentMetadata(assignment, lmsAssignment));
    }

    // listCoursesForUser

    @Test
    void testListCoursesForUserSuccess() throws Exception {
        when(courseReaderExtended.listCoursesForUser(any())).thenReturn(List.of(canvasCourseExtended));
        when(canvasCourseExtended.from()).thenReturn(lmsCourse);

        List<LmsCourse> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listCoursesForUser(platformDeployment, "user1", "tok");
        }

        assertEquals(1, result.size());
        assertEquals(lmsCourse, result.get(0));
    }

    @Test
    void testListCoursesForUserWrapsException() throws Exception {
        when(courseReaderExtended.listCoursesForUser(any())).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.listCoursesForUser(platformDeployment, "user1", "tok"));
        }
    }

    // listSubmissions(apiUser, outcome, canvasCourseId)

    @Test
    void testListSubmissionsByOutcomeDelegatesToAssignmentIdOverload() throws Exception {
        when(outcome.getLmsOutcomeId()).thenReturn("42");
        when(submissionReaderExtended.getCourseSubmissions(any())).thenReturn(List.of(canvasSubmissionExtended));
        when(canvasSubmissionExtended.from()).thenReturn(lmsSubmission);

        List<LmsSubmission> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listSubmissions(ltiUserEntity, outcome, "course1");
        }

        assertEquals(1, result.size());
        assertEquals(lmsSubmission, result.get(0));
    }

    // listSubmissions(apiUser, canvasAssignmentId, canvasCourseId)

    @Test
    void testListSubmissionsSuccess() throws Exception {
        when(submissionReaderExtended.getCourseSubmissions(any())).thenReturn(List.of(canvasSubmissionExtended));
        when(canvasSubmissionExtended.from()).thenReturn(lmsSubmission);

        List<LmsSubmission> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listSubmissions(ltiUserEntity, "5", "course1");
        }

        assertEquals(1, result.size());
    }

    @Test
    void testListSubmissionsWrapsException() throws Exception {
        when(submissionReaderExtended.getCourseSubmissions(any())).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.listSubmissions(ltiUserEntity, "5", "course1"));
        }
    }

    // listSubmissionsForMultipleAssignments

    @Test
    void testListSubmissionsForMultipleAssignmentsSuccess() throws Exception {
        when(submissionReaderExtended.listSubmissionsForMultipleAssignments(any())).thenReturn(List.of(canvasSubmissionExtended));
        when(canvasSubmissionExtended.from()).thenReturn(lmsSubmission);

        List<LmsSubmission> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listSubmissionsForMultipleAssignments(ltiUserEntity, "course1", List.of("1", "2"));
        }

        assertEquals(1, result.size());
    }

    @Test
    void testListSubmissionsForMultipleAssignmentsWrapsException() throws Exception {
        when(submissionReaderExtended.listSubmissionsForMultipleAssignments(any())).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.listSubmissionsForMultipleAssignments(ltiUserEntity, "course1", List.of("1", "2")));
        }
    }

    // addLmsExtensions

    @Test
    @SuppressWarnings("unchecked")
    void testAddLmsExtensionsForStudentSubmission() throws Exception {
        when(submission.getDateSubmitted()).thenReturn(new Timestamp(1700000000000L));

        canvasApiClientService.addLmsExtensions(score, submission, true);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(score).setLmsSubmissionExtension(captor.capture());
        assertEquals(true, captor.getValue().get("new_submission"));
        assertNotNull(captor.getValue().get("submitted_at"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAddLmsExtensionsForInstructorGradedSubmission() throws Exception {
        when(submission.getDateSubmitted()).thenReturn(new Timestamp(System.currentTimeMillis()));

        canvasApiClientService.addLmsExtensions(score, submission, false);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(score).setLmsSubmissionExtension(captor.capture());
        assertEquals(false, captor.getValue().get("new_submission"));
    }

    // sendConversation

    @Test
    void testSendConversationSuccess() throws Exception {
        LmsCreateConversationOptions options = LmsCreateConversationOptions.builder()
            .lmsUserId("10")
            .attachmentIds(List.of("1", "2"))
            .forceNew(true)
            .groupConversation(false)
            .subject("subj")
            .body("body")
            .build();
        when(conversationWriterExtended.createConversation(any())).thenReturn(List.of(conversationExtendedMock));
        when(conversationExtendedMock.from()).thenReturn(LmsConversation.builder().id("77").build());

        List<LmsConversation> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.sendConversation(options, ltiUserEntity);
        }

        assertEquals(1, result.size());
        assertEquals("77", result.get(0).getId());
    }

    @Test
    void testSendConversationWrapsException() throws Exception {
        LmsCreateConversationOptions options = LmsCreateConversationOptions.builder()
            .lmsUserId("10")
            .attachmentIds(List.of())
            .subject("subj")
            .body("body")
            .build();
        when(conversationWriterExtended.createConversation(any())).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.sendConversation(options, ltiUserEntity));
        }
    }

    // getConversation

    @Test
    void testGetConversationSuccess() throws Exception {
        LmsGetSingleConversationOptions options = LmsGetSingleConversationOptions.builder().conversationId("55").autoMarkAsRead(true).build();
        when(conversationReaderExtended.getSingleConversation(any())).thenReturn(Optional.of(conversationExtendedMock));
        when(conversationExtendedMock.from()).thenReturn(LmsConversation.builder().id("55").build());

        Optional<LmsConversation> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.getConversation(options, ltiUserEntity);
        }

        assertTrue(result.isPresent());
        assertEquals("55", result.get().getId());
    }

    @Test
    void testGetConversationWrapsException() throws Exception {
        LmsGetSingleConversationOptions options = LmsGetSingleConversationOptions.builder().conversationId("55").build();
        when(conversationReaderExtended.getSingleConversation(any())).thenThrow(new CanvasException());

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.getConversation(options, ltiUserEntity));
        }
    }

    // listUsersForCourse

    @Test
    void testListUsersForCourseAlwaysReturnsEmptyListEvenWhenUsersWereFetched() throws Exception {
        LmsGetUsersInCourseOptions options = LmsGetUsersInCourseOptions.builder()
            .lmsCourseId("course1")
            .enrollmentState(List.of(EnrollmentState.ACTIVE))
            .enrollmentType(List.of(EnrollmentType.STUDENT))
            .batchSize(50)
            .batchId(UUID.randomUUID())
            .build();

        List<LmsUser> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.listUsersForCourse(options, ltiUserEntity);
        }

        verify(userReaderExtended, times(1)).getUsersInCourse(any(), eq(options.getBatchId()));
        assertTrue(result.isEmpty());
    }

    @Test
    void testListUsersForCourseWrapsException() throws Exception {
        LmsGetUsersInCourseOptions options = LmsGetUsersInCourseOptions.builder()
            .lmsCourseId("course1")
            .enrollmentState(List.of(EnrollmentState.ACTIVE))
            .enrollmentType(List.of(EnrollmentType.STUDENT))
            .batchSize(50)
            .batchId(UUID.randomUUID())
            .build();
        doThrow(new IOException("fail")).when(userReaderExtended).getUsersInCourse(any(), any());

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.listUsersForCourse(options, ltiUserEntity));
        }
    }

    // getFile

    @Test
    void testGetFileSuccess() throws Exception {
        when(fileReaderExtended.getFile("file/123")).thenReturn(Optional.of(fileExtendedMock));
        when(fileExtendedMock.from()).thenReturn(LmsFile.builder().id("123").displayName("doc.pdf").build());

        Optional<LmsFile> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.getFile(ltiUserEntity, "123");
        }

        assertTrue(result.isPresent());
        assertEquals("123", result.get().getId());
    }

    @Test
    void testGetFileWrapsException() throws Exception {
        when(fileReaderExtended.getFile(any())).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.getFile(ltiUserEntity, "123"));
        }
    }

    // getFiles

    @Test
    void testGetFilesSuccess() throws Exception {
        FolderExtended folder = FolderExtended.builder().id("10").fullName("my files/conversation attachments").build();
        when(folderReaderExtended.getFolders()).thenReturn(List.of(folder));
        when(fileReaderExtended.getFiles("folders/10/files")).thenReturn(List.of(fileExtendedMock));
        when(fileExtendedMock.from()).thenReturn(LmsFile.builder().id("5").build());

        List<LmsFile> result;

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            result = canvasApiClientService.getFiles(ltiUserEntity);
        }

        assertEquals(1, result.size());
        assertEquals("5", result.get(0).getId());
    }

    @Test
    void testGetFilesThrowsWhenNoFoldersFound() throws Exception {
        when(folderReaderExtended.getFolders()).thenReturn(List.of());

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            ApiException ex = assertThrows(ApiException.class, () -> canvasApiClientService.getFiles(ltiUserEntity));
            assertTrue(ex.getMessage().contains("No folders found"));
        }
    }

    @Test
    void testGetFilesThrowsWhenConversationAttachmentsFolderMissing() throws Exception {
        FolderExtended folder = FolderExtended.builder().id("10").fullName("some other folder").build();
        when(folderReaderExtended.getFolders()).thenReturn(List.of(folder));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            ApiException ex = assertThrows(ApiException.class, () -> canvasApiClientService.getFiles(ltiUserEntity));
            assertTrue(ex.getMessage().contains("conversation attachments"));
        }
    }

    @Test
    void testGetFilesWrapsExceptionFromFolderReader() throws Exception {
        when(folderReaderExtended.getFolders()).thenThrow(new IOException("fail"));

        try (MockedConstruction<CanvasApiFactoryExtended> _ = mockApiFactory()) {
            assertThrows(ApiException.class, () -> canvasApiClientService.getFiles(ltiUserEntity));
        }
    }

}
