package edu.iu.terracotta.service.canvas.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import edu.iu.terracotta.BaseTest;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.LMSOAuthException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.canvas.CanvasAPITokenEntity;
import edu.iu.terracotta.service.canvas.AssignmentWriterExtended;
import edu.ksu.canvas.exception.InvalidOauthTokenException;

@SuppressWarnings({"PMD.EmptyCatchBlock"})
public class CanvasAPIClientImplTest extends BaseTest {

    @Spy
    @InjectMocks
    private CanvasAPIClientImpl canvasAPIClient;

    @Mock private CanvasOAuthServiceImpl canvasOAuthService;

    // Test data
    private LtiUserEntity apiUser;
    private PlatformDeployment platformDeployment;
    private CanvasAPITokenEntity canvasAPIToken;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        platformDeployment = new PlatformDeployment();
        platformDeployment.setKeyId(829);

        apiUser = new LtiUserEntity("test-user-key", new Date(), platformDeployment);
        apiUser.setUserId(1040);

        canvasAPIToken = new CanvasAPITokenEntity();
        canvasAPIToken.setTokenId(1043);
        canvasAPIToken.setAccessToken("test-access-token");
    }

    @Test
    public void testCreateCanvasAssignmentUsesUserAPIToken() throws LMSOAuthException, CanvasApiException, IOException {
        when(canvasOAuthService.isConfigured(eq(platformDeployment))).thenReturn(true);
        when(canvasOAuthService.getAccessToken(eq(apiUser))).thenReturn(canvasAPIToken);

        doReturn(assignmentWriterExtended)
                .when(canvasAPIClient)
                .getWriterInternal(
                        eq(apiUser),
                        eq(AssignmentWriterExtended.class),
                        argThat(token -> token.getAccessToken().equals(canvasAPIToken.getAccessToken())));

        AssignmentExtended assignmentExtended = new AssignmentExtended();
        String canvasCourseId = "2911";
        canvasAPIClient.createCanvasAssignment(apiUser, assignmentExtended, canvasCourseId);

        verify(canvasOAuthService).isConfigured(platformDeployment);
        verify(canvasOAuthService).getAccessToken(apiUser);
        verify(assignmentWriterExtended).createAssignment(canvasCourseId, assignmentExtended);
    }

    @Test
    public void testCreateCanvasAssignmentUsesAdminAPIToken() throws CanvasApiException, IOException {
        platformDeployment.setApiToken("admin-api-token");
        when(canvasOAuthService.isConfigured(eq(platformDeployment))).thenReturn(false);

        doReturn(assignmentWriterExtended)
                .when(canvasAPIClient)
                .getWriterInternal(
                        eq(apiUser),
                        eq(AssignmentWriterExtended.class),
                        argThat(token -> token.getAccessToken().equals(platformDeployment.getApiToken())));

        AssignmentExtended assignmentExtended = new AssignmentExtended();
        String canvasCourseId = "2911";
        canvasAPIClient.createCanvasAssignment(apiUser, assignmentExtended, canvasCourseId);

        verify(canvasOAuthService).isConfigured(platformDeployment);
        verify(assignmentWriterExtended).createAssignment(canvasCourseId, assignmentExtended);
    }

    @Test
    public void testCreateCanvasAssignmentThrowsExceptionWhenUserAPITokenDoesNotExist()
            throws CanvasApiException, IOException, LMSOAuthException {
        when(canvasOAuthService.isConfigured(eq(platformDeployment))).thenReturn(true);
        when(canvasOAuthService.getAccessToken(eq(apiUser))).thenThrow(new LMSOAuthException("access token doesn't exist for user"));

        doReturn(assignmentWriterExtended)
                .when(canvasAPIClient)
                .getWriterInternal(eq(apiUser), eq(AssignmentWriterExtended.class), any());

        AssignmentExtended assignmentExtended = new AssignmentExtended();
        String canvasCourseId = "2911";
        try {
            canvasAPIClient.createCanvasAssignment(apiUser, assignmentExtended, canvasCourseId);
            fail();
        } catch (CanvasApiException e) {

        }

        verify(canvasOAuthService).isConfigured(platformDeployment);
        verify(assignmentWriterExtended, never()).createAssignment(canvasCourseId, assignmentExtended);
    }

    @Test
    public void testCreateCanvasAssignmentThrowsExceptionWhenAdminAPITokenDoesNotExist() throws IOException {
        platformDeployment.setApiToken(null);
        when(canvasOAuthService.isConfigured(eq(platformDeployment))).thenReturn(false);

        doReturn(assignmentWriterExtended)
                .when(canvasAPIClient)
                .getWriterInternal(eq(apiUser), eq(AssignmentWriterExtended.class), any());

        AssignmentExtended assignmentExtended = new AssignmentExtended();
        String canvasCourseId = "2911";
        try {
            canvasAPIClient.createCanvasAssignment(apiUser, assignmentExtended, canvasCourseId);
            fail();
        } catch (CanvasApiException e) {
        }

        verify(canvasOAuthService).isConfigured(platformDeployment);
        verify(assignmentWriterExtended, never()).createAssignment(canvasCourseId, assignmentExtended);
    }

    @Test
    public void testCreateCanvasAssignmentThrowsCanvasApiExceptionWhenWriterThrowsCanvasException() throws LMSOAuthException, IOException {
        when(canvasOAuthService.isConfigured(eq(platformDeployment))).thenReturn(true);
        when(canvasOAuthService.getAccessToken(eq(apiUser))).thenReturn(canvasAPIToken);

        AssignmentExtended assignmentExtended = new AssignmentExtended();
        String canvasCourseId = "2911";

        doReturn(assignmentWriterExtended)
                .when(canvasAPIClient)
                .getWriterInternal(
                        eq(apiUser),
                        eq(AssignmentWriterExtended.class),
                        argThat(token -> token.getAccessToken().equals(canvasAPIToken.getAccessToken())));
        doThrow(new InvalidOauthTokenException()).when(assignmentWriterExtended).createAssignment(canvasCourseId, assignmentExtended);

        try {
            canvasAPIClient.createCanvasAssignment(apiUser, assignmentExtended, canvasCourseId);
            fail();
        } catch (CanvasApiException e) {
            assertTrue(e.getCause() instanceof InvalidOauthTokenException);
        }

        verify(canvasOAuthService).isConfigured(platformDeployment);
        verify(canvasOAuthService).getAccessToken(apiUser);
        verify(assignmentWriterExtended).createAssignment(canvasCourseId, assignmentExtended);
    }

}
