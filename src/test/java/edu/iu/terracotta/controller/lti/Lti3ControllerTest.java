package edu.iu.terracotta.controller.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.canvas.service.lti.advantage.CanvasAdvantageNoticeService;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthServiceManager;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageDeepLinkService;
import edu.iu.terracotta.dao.entity.ObsoleteAssignment;
import edu.iu.terracotta.dao.exceptions.FeatureNotFoundException;
import edu.iu.terracotta.service.app.async.ParticipantAsyncService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.lti.Lti3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.SignatureException;

/**
 * Lti3Controller#home() gets its Lti3Request from the static Lti3Request.getInstance(link)
 * factory rather than through constructor injection, so it is mocked statically here; the shared
 * {@code lti3Request} mock from BaseModelTest stands in for whatever getInstance() would have returned.
 */
@SuppressWarnings("unchecked")
public class Lti3ControllerTest extends BaseTest {

    @Mock private AdvantageDeepLinkService advantageDeepLinkService;
    @Mock private LmsOAuthServiceManager lmsOAuthServiceManager;
    @Mock private CanvasAdvantageNoticeService canvasAdvantageNoticeService;
    @Mock private ParticipantAsyncService participantAsyncService;

    private Lti3Controller lti3Controller;

    private final Model model = mock(Model.class);
    private final Jws<Claims> claimsJws = mock(Jws.class);
    private final Claims claims = mock(Claims.class);

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        lti3Controller = new Lti3Controller(ltiLinkRepository, apiJwtService, advantageDeepLinkService, caliperService, ltiDataService, ltiJwtService, lmsOAuthServiceManager, canvasAdvantageNoticeService, participantAsyncService);

        when(httpServletRequest.getParameter("state")).thenReturn("state123");
        when(httpServletRequest.getParameter("link")).thenReturn(null);

        when(ltiJwtService.validateState(anyString())).thenReturn(claimsJws);
        when(claimsJws.getPayload()).thenReturn(claims);
        when(claims.get("clientId")).thenReturn("aud123");
        when(claims.containsKey("ltiDeploymentId")).thenReturn(false);

        when(lti3Request.getAud()).thenReturn("aud123");
        when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://example.com/launch");
        when(lti3Request.getLtiCustom()).thenReturn(Collections.emptyMap());
        when(lti3Request.getLtiRoles()).thenReturn(Collections.emptyList());

        when(ltiDataService.getDemoMode()).thenReturn(false);
        when(apiJwtService.buildJwt(true, lti3Request)).thenReturn("oneTimeToken123");
    }

    private MockedStatic<Lti3Request> mockLti3Request() {
        MockedStatic<Lti3Request> mockedStatic = mockStatic(Lti3Request.class);
        mockedStatic.when(() -> Lti3Request.getInstance(any())).thenReturn(lti3Request);

        return mockedStatic;
    }

    private String callHome() throws Exception {
        return lti3Controller.home(httpServletRequest, null, model);
    }

    // the launch token (and, when present, the LMS API OAuth URL) are delivered via lti3Launch.html
    // model attributes rather than a redirect query string - see Lti3Controller#home for why.
    private void assertLaunchView(String result, String expectedLmsApiOAuthUrl) {
        assertEquals("lti3Launch", result);
        verify(model).addAttribute("token", "oneTimeToken123");
        verify(model).addAttribute("lmsApiOAuthUrl", expectedLmsApiOAuthUrl);
    }

    @Test
    void homeHappyPathRedirectTest() throws Exception {
        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertLaunchView(result, null);
        }
    }

    @Test
    void homeRegistersNoticeHandlerForCanvasDeploymentTest() throws Exception {
        when(platformDeployment.getLmsConnector()).thenReturn(LmsConnector.CANVAS);

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            callHome();
        }

        verify(canvasAdvantageNoticeService).ensureNoticeHandlerRegistered(toolDeployment);
    }

    @Test
    void homeSkipsNoticeHandlerRegistrationForNonCanvasDeploymentTest() throws Exception {
        when(platformDeployment.getLmsConnector()).thenReturn(LmsConnector.BRIGHTSPACE);

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            callHome();
        }

        verify(canvasAdvantageNoticeService, never()).ensureNoticeHandlerRegistered(any());
    }

    @Test
    void homeObsoleteAssignmentRedirectTest() throws Exception {
        when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://example.com/" + ObsoleteAssignment.URL);

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertEquals("redirect:/" + ObsoleteAssignment.URL, result);
        }
    }

    @Test
    void homeBadClientIdTest() throws Exception {
        when(claims.get("clientId")).thenReturn("different-aud");

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertEquals(TextConstants.LTI3ERROR, result);
            verify(model).addAttribute(eq(TextConstants.ERROR), anyString());
        }
    }

    @Test
    void homeBadDeploymentIdTest() throws Exception {
        when(claims.containsKey("ltiDeploymentId")).thenReturn(true);
        when(claims.get("ltiDeploymentId")).thenReturn("dep-claim");
        when(lti3Request.getLtiDeploymentId()).thenReturn("dep-actual");

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertEquals(TextConstants.LTI3ERROR, result);
        }
    }

    @Test
    void homeDemoModeLinkFoundTest() throws Exception {
        when(ltiDataService.getDemoMode()).thenReturn(true);
        when(httpServletRequest.getParameter("link")).thenReturn("linkKey1");
        when(lti3Request.getContext()).thenReturn(ltiContextEntity);
        when(ltiLinkRepository.findByLinkKeyAndContext("linkKey1", ltiContextEntity)).thenReturn(List.of(ltiLinkEntity));
        when(ltiLinkEntity.createHtmlFromLink()).thenReturn("<b>the html</b>");

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertEquals("lti3Result", result);
            verify(model).addAttribute(TextConstants.HTML_CONTENT, "<b>the html</b>");
        }
    }

    @Test
    void homeDemoModeLinkNotFoundTest() throws Exception {
        when(ltiDataService.getDemoMode()).thenReturn(true);
        when(httpServletRequest.getParameter("link")).thenReturn("linkKey1");
        when(lti3Request.getContext()).thenReturn(ltiContextEntity);
        when(ltiLinkRepository.findByLinkKeyAndContext("linkKey1", ltiContextEntity)).thenReturn(Collections.emptyList());

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertEquals("lti3Result", result);
            verify(model).addAttribute(TextConstants.HTML_CONTENT, "<b>No element was found for that context and linkKey</b>");
        }
    }

    @Test
    void homeDemoModeNoLinkRequestedTest() throws Exception {
        when(ltiDataService.getDemoMode()).thenReturn(true);
        when(httpServletRequest.getParameter("link")).thenReturn(null);
        // targetLinkUrl ends with the "?link=" marker itself, so the fallback substring computed by
        // the controller resolves to an empty (blank) string and this "nothing requested" branch is hit.
        when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://example.com/launch?link=");

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertEquals("lti3Result", result);
            verify(model).addAttribute(TextConstants.HTML_CONTENT, "<b>No element was requested or it doesn't exists</b>");
        }
    }

    @Test
    void homeDeepLinkingRedirectTest() throws Exception {
        UUID uuid = UUID.randomUUID();
        LtiDeepLink ltiDeepLink = mock(LtiDeepLink.class);
        when(ltiDeepLink.getUuid()).thenReturn(uuid);
        when(lti3Request.getLtiMessageType()).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
        when(advantageDeepLinkService.generateLtiDeepLink(lti3Request, httpServletRequest, "state123")).thenReturn(ltiDeepLink);

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertEquals("redirect:/app/deepLink.html?id=" + uuid, result);
        }
    }

    @Test
    void homeDeepLinkingPropagatesTerracottaConnectorExceptionTest() throws Exception {
        when(lti3Request.getLtiMessageType()).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
        when(advantageDeepLinkService.generateLtiDeepLink(lti3Request, httpServletRequest, "state123")).thenThrow(new TerracottaConnectorException("boom"));

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            assertThrows(TerracottaConnectorException.class, this::callHome);
        }
    }

    @Test
    void homeBuildJwtPropagatesTerracottaConnectorExceptionTest() throws Exception {
        when(apiJwtService.buildJwt(true, lti3Request)).thenThrow(new TerracottaConnectorException("boom"));

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            assertThrows(TerracottaConnectorException.class, this::callHome);
        }
    }

    @Test
    void homeBuildJwtGeneralSecurityExceptionCaughtTest() throws Exception {
        when(apiJwtService.buildJwt(true, lti3Request)).thenThrow(new GeneralSecurityException("boom"));

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertEquals(TextConstants.LTI3ERROR, result);
        }
    }

    @Test
    void homeValidateStateSignatureExceptionCaughtTest() throws Exception {
        when(ltiJwtService.validateState(anyString())).thenThrow(new SignatureException("bad signature"));

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertEquals(TextConstants.LTI3ERROR, result);
        }
    }

    @Test
    void homeInstructorOAuthTerracottaConnectorExceptionCaughtInternallyTest() throws Exception {
        when(lti3Request.isRoleInstructor()).thenReturn(true);
        when(lti3Request.getKey()).thenReturn(platformDeployment);
        when(lti3Request.getUser()).thenReturn(ltiUserEntity);
        when(lmsOAuthServiceManager.getLmsOAuthService(platformDeployment)).thenThrow(new TerracottaConnectorException("no oauth settings"));

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertLaunchView(result, null);
        }
    }

    @Test
    void homeInstructorOAuthNotConfiguredTest() throws Exception {
        when(lti3Request.isRoleInstructor()).thenReturn(true);
        when(lti3Request.getKey()).thenReturn(platformDeployment);
        when(lti3Request.getUser()).thenReturn(ltiUserEntity);
        doReturn(lmsOAuthService).when(lmsOAuthServiceManager).getLmsOAuthService(platformDeployment);
        when(lmsOAuthService.isConfigured(platformDeployment)).thenReturn(false);

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertLaunchView(result, null);
        }
    }

    @Test
    void homeInstructorOAuthAlreadyHasAccessTokenTest() throws Exception {
        when(lti3Request.isRoleInstructor()).thenReturn(true);
        when(lti3Request.getKey()).thenReturn(platformDeployment);
        when(lti3Request.getUser()).thenReturn(ltiUserEntity);
        doReturn(lmsOAuthService).when(lmsOAuthServiceManager).getLmsOAuthService(platformDeployment);
        when(lmsOAuthService.isConfigured(platformDeployment)).thenReturn(true);
        when(lmsOAuthService.isAccessTokenAvailable(ltiUserEntity)).thenReturn(true);

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertLaunchView(result, null);
        }
    }

    @Test
    void homeInstructorOAuthUrlAppendedTest() throws Exception {
        when(lti3Request.isRoleInstructor()).thenReturn(true);
        when(lti3Request.getKey()).thenReturn(platformDeployment);
        when(lti3Request.getUser()).thenReturn(ltiUserEntity);
        doReturn(lmsOAuthService).when(lmsOAuthServiceManager).getLmsOAuthService(platformDeployment);
        when(lmsOAuthService.isConfigured(platformDeployment)).thenReturn(true);
        when(lmsOAuthService.isAccessTokenAvailable(ltiUserEntity)).thenReturn(false);
        when(apiJwtService.generateStateForAPITokenRequest(lti3Request)).thenReturn("state456");
        when(lmsOAuthService.getAuthorizationRequestURI(platformDeployment, "state456")).thenReturn("https://oauth.example.com/authorize");

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertLaunchView(result, "https://oauth.example.com/authorize");
        }
    }

    @Test
    void homeInstructorOAuthFeatureNotFoundCaughtInternallyTest() throws Exception {
        when(lti3Request.isRoleInstructor()).thenReturn(true);
        when(lti3Request.getKey()).thenReturn(platformDeployment);
        when(lti3Request.getUser()).thenReturn(ltiUserEntity);
        doReturn(lmsOAuthService).when(lmsOAuthServiceManager).getLmsOAuthService(platformDeployment);
        when(lmsOAuthService.isConfigured(platformDeployment)).thenReturn(true);
        when(lmsOAuthService.isAccessTokenAvailable(ltiUserEntity)).thenReturn(false);
        when(apiJwtService.generateStateForAPITokenRequest(lti3Request)).thenReturn("state456");
        when(lmsOAuthService.getAuthorizationRequestURI(platformDeployment, "state456")).thenThrow(new FeatureNotFoundException("no feature"));

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertLaunchView(result, null);
        }
    }

    @Test
    void homeInstructorOAuthLmsOAuthExceptionPropagatesTest() throws Exception {
        when(lti3Request.isRoleInstructor()).thenReturn(true);
        when(lti3Request.getKey()).thenReturn(platformDeployment);
        when(lti3Request.getUser()).thenReturn(ltiUserEntity);
        doReturn(lmsOAuthService).when(lmsOAuthServiceManager).getLmsOAuthService(platformDeployment);
        when(lmsOAuthService.isConfigured(platformDeployment)).thenReturn(true);
        when(lmsOAuthService.isAccessTokenAvailable(ltiUserEntity)).thenReturn(false);
        when(apiJwtService.generateStateForAPITokenRequest(lti3Request)).thenReturn("state456");
        when(lmsOAuthService.getAuthorizationRequestURI(platformDeployment, "state456")).thenThrow(new LmsOAuthException("lms rejected"));

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            assertThrows(LmsOAuthException.class, this::callHome);
        }
    }

    @Test
    void homeInstructorSkipsOAuthLookupWhenNotInstructorTest() throws Exception {
        when(lti3Request.isRoleInstructor()).thenReturn(false);

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            String result = callHome();

            assertLaunchView(result, null);
            verify(lmsOAuthServiceManager, never()).getLmsOAuthService(any(PlatformDeployment.class));
        }
    }

    @Test
    void homeRefreshesParticipantsForInstructorLaunchTest() throws Exception {
        when(lti3Request.isRoleInstructor()).thenReturn(true);
        when(lti3Request.getContext()).thenReturn(ltiContextEntity);
        when(lti3Request.getKey()).thenReturn(platformDeployment);
        when(lti3Request.getUser()).thenReturn(ltiUserEntity);
        doReturn(lmsOAuthService).when(lmsOAuthServiceManager).getLmsOAuthService(platformDeployment);
        when(lmsOAuthService.isConfigured(platformDeployment)).thenReturn(false);

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            callHome();
        }

        verify(participantAsyncService).refreshParticipantsForContext(ltiContextEntity);
    }

    @Test
    void homeSkipsParticipantRefreshWhenNotInstructorTest() throws Exception {
        when(lti3Request.isRoleInstructor()).thenReturn(false);

        try (MockedStatic<Lti3Request> _ = mockLti3Request()) {
            callHome();
        }

        verify(participantAsyncService, never()).refreshParticipantsForContext(any());
    }

}
