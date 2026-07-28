package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.GeneralSecurityException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthServiceManager;
import edu.iu.terracotta.dao.exceptions.FeatureNotFoundException;
import edu.iu.terracotta.utils.TextConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@SuppressWarnings("unchecked")
public class LmsOAuthControllerTest extends BaseTest {

    @Mock private LmsOAuthServiceManager lmsOAuthServiceManager;

    private LmsOAuthController lmsOAuthController;

    private final Model model = mock(Model.class);
    private final Jws<Claims> claimsJws = mock(Jws.class);
    private final Claims claims = mock(Claims.class);

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        lmsOAuthController = new LmsOAuthController(ltiUserRepository, lmsOAuthServiceManager, apiJwtService);

        when(httpServletRequest.getParameter("code")).thenReturn("code123");
        when(httpServletRequest.getParameter("state")).thenReturn("state123");
    }

    @Test
    void handleOauthResponseErrorParamTest() throws Exception {
        when(httpServletRequest.getParameter("error")).thenReturn("access_denied");

        String result = lmsOAuthController.handleOauthResponse(httpServletRequest, model);

        assertEquals(TextConstants.OAUTH2_ERROR, result);
        verify(model).addAttribute(eq(TextConstants.ERROR), anyString());
    }

    @Test
    void handleOauthResponseValidateStateThrowsTest() throws Exception {
        when(apiJwtService.validateStateForAPITokenRequest("state123")).thenThrow(new RuntimeException("boom"));

        String result = lmsOAuthController.handleOauthResponse(httpServletRequest, model);

        assertEquals(TextConstants.OAUTH2_ERROR, result);
    }

    @Test
    void handleOauthResponseClaimsEmptyTest() throws Exception {
        when(apiJwtService.validateStateForAPITokenRequest("state123")).thenReturn(Optional.empty());

        String result = lmsOAuthController.handleOauthResponse(httpServletRequest, model);

        assertEquals(TextConstants.OAUTH2_ERROR, result);
    }

    @Test
    void handleOauthResponseFetchTokenLmsOAuthExceptionTest() throws Exception {
        stubValidClaims();
        doThrow(new LmsOAuthException("failed")).when(lmsOAuthService).fetchAndSaveAccessToken(ltiUserEntity, "code123");

        String result = lmsOAuthController.handleOauthResponse(httpServletRequest, model);

        assertEquals(TextConstants.OAUTH2_ERROR, result);
    }

    @Test
    void handleOauthResponseFetchTokenFeatureNotFoundTest() throws Exception {
        stubValidClaims();
        doThrow(new FeatureNotFoundException("no feature")).when(lmsOAuthService).fetchAndSaveAccessToken(ltiUserEntity, "code123");

        String result = lmsOAuthController.handleOauthResponse(httpServletRequest, model);

        assertEquals(TextConstants.OAUTH2_ERROR, result);
    }

    @Test
    void handleOauthResponseHappyPathTest() throws Exception {
        stubValidClaims();
        when(lmsOAuthService.fetchAndSaveAccessToken(ltiUserEntity, "code123")).thenReturn(apiTokenEntity);
        when(apiJwtService.buildJwt(1L, "user1", claims)).thenReturn("finalToken");

        String result = lmsOAuthController.handleOauthResponse(httpServletRequest, model);

        assertEquals("redirect:/app/app.html?token=finalToken", result);
    }

    @Test
    void handleOauthResponseGetLmsOAuthServiceThrowsTest() throws Exception {
        when(apiJwtService.validateStateForAPITokenRequest("state123")).thenReturn(Optional.of(claimsJws));
        when(claimsJws.getPayload()).thenReturn(claims);
        when(claims.get("platformDeploymentId", Long.class)).thenReturn(1L);
        when(claims.get("userId", String.class)).thenReturn("user1");
        when(lmsOAuthServiceManager.getLmsOAuthService(anyLong())).thenThrow(new TerracottaConnectorException("boom"));

        assertThrows(TerracottaConnectorException.class, () -> lmsOAuthController.handleOauthResponse(httpServletRequest, model));
    }

    @Test
    void handleOauthResponseBuildJwtThrowsTest() throws Exception {
        stubValidClaims();
        when(lmsOAuthService.fetchAndSaveAccessToken(ltiUserEntity, "code123")).thenReturn(apiTokenEntity);
        when(apiJwtService.buildJwt(anyLong(), anyString(), any(Claims.class))).thenThrow(new GeneralSecurityException("boom"));

        assertThrows(GeneralSecurityException.class, () -> lmsOAuthController.handleOauthResponse(httpServletRequest, model));
    }

    private void stubValidClaims() throws TerracottaConnectorException {
        when(apiJwtService.validateStateForAPITokenRequest("state123")).thenReturn(Optional.of(claimsJws));
        when(claimsJws.getPayload()).thenReturn(claims);
        when(claims.get("platformDeploymentId", Long.class)).thenReturn(1L);
        when(claims.get("userId", String.class)).thenReturn("user1");
        doReturn(lmsOAuthService).when(lmsOAuthServiceManager).getLmsOAuthService(1L);
    }

}
