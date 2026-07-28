package edu.iu.terracotta.controller.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.dao.entity.LtiNonce;
import edu.iu.terracotta.utils.TextConstants;

public class OidcControllerTest extends BaseTest {

    // PKCS#8 RSA test key generated solely for signing the JWT `state` value in these tests; not used anywhere else.
    private static final String TEST_PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCPrqfySSiY0lRY
            H3TP9Y4v/CLB4YtKpkdXLU3Lz8/rk5ghUnsiy1g+5J0fFnqE6JUsJOXPw/xffBP9
            PEGmu9caLEuJDSrfvrNlbA+CG6UPXfBAnn7sAYx/HGPxLA1bRQkUX9eQ1fgQMUmE
            gOLk3IYFU6v8hIhAJrqKAdEZ6vNhCIOxbUVwHBFl/hksys78Jvl7laLsT0GwlXUA
            aRYjK15nKt+G4CmmbMMnN4sQIJN+DAiQNGtuY0wer+/G4GeE6ZG9UtgTAAPhLrwh
            kbnha6StAe4Dxqc/BgjBrtwoOQg3LYYzMYfUCy/pB2rZ6+zZ6lZoo8fKCcgFBrzU
            9y8G4yltAgMBAAECggEADeQ4xB3LIUValr+J025eDikeMikMhs3fQWA1sJIZu+DD
            +VrIYXOQB1qij0Yk19NF0uQJXQZJkPDF3Pq1rTllgVgNgIVnowTEwvZI5I8oLP1r
            1E+OnWJD82krB373Fp8s0s22YYAycNxXC4rLhQ2GHp85lls48hzcdanZzdvvpm6f
            dNfTTCmB8Vn2IUG1T1gPVCjuNZoxZKJCEpHVlgL0ZlMI+2T67t0c8tjh9U3X/tNL
            CpbblfKvefPqnu8ImFkROczPZSGZuhi3VFSl2EFNP8UxEwP5NfuX0/jSsYVa542Y
            5rn3S78CokJdXk1BupBwNQioOC9PfFm9y8rYn3NsOQKBgQDFkQhlABX5vXSzKpUW
            3ygN/uHtGwzu7FMk2P5gAR/Xme9Bnho1G65aSlfR6hebxAxo9+2I8hwqNU2iwuNh
            OV6tGd8ehTmAGYxoIGU+gA9w97fhQreemCfW/Xvq4Wt7J9Ceyiuv1MeAiHfVdzUZ
            szCTC7Hz+c7w1lEcmJS65866mQKBgQC6LbcpJG24b3tzRaNRd/9Y41gSma3qSvwn
            8Lc3oQnSuhCGfSHwOad6jPHKfm74xSsST+4nY7uGXeOGbGRkoSYWBpaNz3hSUbth
            clTysVIfFG3nNj58x8NyDd3jftv/dpdRT+sGlSpRMsPHQXU4WK/F1ChUhZM0M9yZ
            J2uJl+ld9QKBgA+iNGnZoeOLTGrJGdcffYnt+27JzzIw3TtzOF3ceOqUscwdeLFv
            KVLXwy9HOOsIjnrX86H8lqH4adZRWbDd21ITVAaUQEKUwvmSZrCVbaNg1toqb2FL
            ZQusL1wczmaGdgm6sc5OJiNTqTfpIPiRp2xbZo+J2whPzUdYA5zurvsxAoGBAKsI
            wFDthFaBI9nbyZNYN55DDG/Z+mlYhZlzi/1w1YNfJztFzV3QuUjeHmo2CGBFUbI6
            97/74RQpQJIHFZtUZ5aoarrQM+r75rY4wE1MFPM0Y1qa/IDJS4WFs6gArL5dBdnP
            H0wm0H9TsYNlYPhokOGea4ZqR7cXRbr0+denVN3ZAoGABktNe5zYZK0rsG9d8+Ig
            /XQ+rp1AQrq3ckHI+sL9bkoK0VVXh+G2IJ27bnnhBcqJ2XmWTvfpOX035wmHpidR
            JR8A4/FbbzzZlwuAlgCejljbpGyCtZD888p129Xps4/WS4htal8amm+dhqR1eBgg
            e4KHpXJqnVXm/guvAWSli+U=
            -----END PRIVATE KEY-----
            """;

    private static final String ISS = "https://platform.example.com";
    private static final String CLIENT_ID = "client-id-value";
    private static final String DEPLOYMENT_ID = "deployment-id-value";
    private static final String OIDC_ENDPOINT = "https://platform.example.com/oidc/auth";

    @InjectMocks private OidcController oidcController;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        ReflectionTestUtils.setField(oidcController, "ltiDataVerboseLoggingEnabled", false);

        when(httpServletRequest.getParameter("iss")).thenReturn(ISS);
        when(httpServletRequest.getParameter("login_hint")).thenReturn("login-hint");
        when(httpServletRequest.getParameter("target_link_uri")).thenReturn("https://tool.example.com/target");
        when(httpServletRequest.getParameter("lti_message_hint")).thenReturn("message-hint");
        when(httpServletRequest.getParameter("client_id")).thenReturn(null);
        when(httpServletRequest.getParameter("lti_deployment_id")).thenReturn(null);

        when(platformDeployment.getClientId()).thenReturn(CLIENT_ID);
        when(platformDeployment.getIss()).thenReturn(ISS);
        when(platformDeployment.getOidcEndpoint()).thenReturn(OIDC_ENDPOINT);
        when(ltiDataService.getOwnPrivateKey()).thenReturn(TEST_PRIVATE_KEY);
    }

    @Test
    void loginInitiationsNoMatchesReturnsError() {
        when(platformDeploymentRepository.findByIss(ISS)).thenReturn(Collections.emptyList());

        Model model = new ConcurrentModel();
        String ret = oidcController.loginInitiations(httpServletRequest, model);

        assertEquals(TextConstants.LTI3ERROR, ret);
        assertNotNull(model.getAttribute(TextConstants.ERROR));
        assertTrue(((String) model.getAttribute(TextConstants.ERROR)).contains(ISS));
    }

    @Test
    void loginInitiationsMultipleMatchesReturnsError() {
        when(platformDeploymentRepository.findByIss(ISS)).thenReturn(List.of(platformDeployment, platformDeployment));

        Model model = new ConcurrentModel();
        String ret = oidcController.loginInitiations(httpServletRequest, model);

        assertEquals(TextConstants.LTI3ERROR, ret);
        assertNotNull(model.getAttribute(TextConstants.ERROR));
    }

    @Test
    void loginInitiationsSingleMatchByIssOnlyDemoModeFalseRedirectsToPlatform() {
        when(platformDeploymentRepository.findByIss(ISS)).thenReturn(List.of(platformDeployment));
        when(ltiDataService.getDemoMode()).thenReturn(false);

        Model model = new ConcurrentModel();
        String ret = oidcController.loginInitiations(httpServletRequest, model);

        assertTrue(ret.startsWith("redirect:" + OIDC_ENDPOINT), () -> "Expected a redirect to the platform's OIDC endpoint, got: " + ret);
    }

    @Test
    void loginInitiationsSingleMatchByIssOnlyDemoModeTrueReturnsOidcRedirect() {
        when(platformDeploymentRepository.findByIss(ISS)).thenReturn(List.of(platformDeployment));
        when(ltiDataService.getDemoMode()).thenReturn(true);

        Model model = new ConcurrentModel();
        String ret = oidcController.loginInitiations(httpServletRequest, model);

        assertEquals("oidcRedirect", ret);
        assertNotNull(model.getAttribute("initiation_dto"));
        assertEquals(CLIENT_ID, model.getAttribute("client_id_received"));
    }

    @Test
    void loginInitiationsWithClientIdAndDeploymentIdMatchFound() {
        when(httpServletRequest.getParameter("client_id")).thenReturn(CLIENT_ID);
        when(httpServletRequest.getParameter("lti_deployment_id")).thenReturn(DEPLOYMENT_ID);
        when(platformDeploymentRepository.findByIssAndClientIdAndToolDeployments_LtiDeploymentId(ISS, CLIENT_ID, DEPLOYMENT_ID)).thenReturn(List.of(platformDeployment));
        when(ltiDataService.getDemoMode()).thenReturn(false);

        Model model = new ConcurrentModel();
        String ret = oidcController.loginInitiations(httpServletRequest, model);

        assertTrue(ret.startsWith("redirect:" + OIDC_ENDPOINT), () -> "Expected a redirect to the platform's OIDC endpoint, got: " + ret);
    }

    @Test
    void loginInitiationsWithClientIdAndDeploymentIdNoMatchAutoCreatesToolDeployment() {
        when(httpServletRequest.getParameter("client_id")).thenReturn(CLIENT_ID);
        when(httpServletRequest.getParameter("lti_deployment_id")).thenReturn(DEPLOYMENT_ID);
        when(platformDeploymentRepository.findByIssAndClientIdAndToolDeployments_LtiDeploymentId(ISS, CLIENT_ID, DEPLOYMENT_ID)).thenReturn(Collections.emptyList());

        ToolDeployment createdToolDeployment = mock(ToolDeployment.class);
        when(createdToolDeployment.getPlatformDeployment()).thenReturn(platformDeployment);
        when(ltiDataService.findOrCreateToolDeployment(ISS, CLIENT_ID, DEPLOYMENT_ID)).thenReturn(createdToolDeployment);
        when(ltiDataService.getDemoMode()).thenReturn(false);

        Model model = new ConcurrentModel();
        String ret = oidcController.loginInitiations(httpServletRequest, model);

        assertTrue(ret.startsWith("redirect:" + OIDC_ENDPOINT), () -> "Expected a redirect to the platform's OIDC endpoint, got: " + ret);
    }

    @Test
    void loginInitiationsWithClientIdAndDeploymentIdNoMatchAndNoToolDeploymentCreatedReturnsError() {
        when(httpServletRequest.getParameter("client_id")).thenReturn(CLIENT_ID);
        when(httpServletRequest.getParameter("lti_deployment_id")).thenReturn(DEPLOYMENT_ID);
        when(platformDeploymentRepository.findByIssAndClientIdAndToolDeployments_LtiDeploymentId(ISS, CLIENT_ID, DEPLOYMENT_ID)).thenReturn(Collections.emptyList());
        when(ltiDataService.findOrCreateToolDeployment(ISS, CLIENT_ID, DEPLOYMENT_ID)).thenReturn(null);

        Model model = new ConcurrentModel();
        String ret = oidcController.loginInitiations(httpServletRequest, model);

        assertEquals(TextConstants.LTI3ERROR, ret);
    }

    @Test
    void loginInitiationsWithClientIdOnly() {
        when(httpServletRequest.getParameter("client_id")).thenReturn(CLIENT_ID);
        when(platformDeploymentRepository.findByIssAndClientId(ISS, CLIENT_ID)).thenReturn(List.of(platformDeployment));
        when(ltiDataService.getDemoMode()).thenReturn(false);

        Model model = new ConcurrentModel();
        String ret = oidcController.loginInitiations(httpServletRequest, model);

        assertTrue(ret.startsWith("redirect:" + OIDC_ENDPOINT), () -> "Expected a redirect to the platform's OIDC endpoint, got: " + ret);
    }

    @Test
    void loginInitiationsWithDeploymentIdOnly() {
        when(httpServletRequest.getParameter("lti_deployment_id")).thenReturn(DEPLOYMENT_ID);
        when(platformDeploymentRepository.findByIssAndToolDeployments_LtiDeploymentId(ISS, DEPLOYMENT_ID)).thenReturn(List.of(platformDeployment));
        when(ltiDataService.getDemoMode()).thenReturn(false);

        Model model = new ConcurrentModel();
        String ret = oidcController.loginInitiations(httpServletRequest, model);

        assertTrue(ret.startsWith("redirect:" + OIDC_ENDPOINT), () -> "Expected a redirect to the platform's OIDC endpoint, got: " + ret);
    }

    @Test
    void loginInitiationsUsesDeploymentClientIdWhenNoneReceived() {
        // when neither the DTO nor the URL provide a client_id, the matched platformDeployment's own clientId is used
        when(platformDeploymentRepository.findByIss(ISS)).thenReturn(List.of(platformDeployment));
        when(ltiDataService.getDemoMode()).thenReturn(true);

        Model model = new ConcurrentModel();
        String ret = oidcController.loginInitiations(httpServletRequest, model);

        assertEquals("oidcRedirect", ret);
        assertEquals(CLIENT_ID, model.getAttribute("client_id_received"));
    }

    @Test
    void loginInitiationsGenerateAuthRequestPayloadFailureReturnsError() {
        when(platformDeploymentRepository.findByIss(ISS)).thenReturn(List.of(platformDeployment));
        // an unparsable private key makes OAuthUtils.loadPrivateKey throw GeneralSecurityException,
        // which is caught by the outer try/catch in loginInitiations()
        when(ltiDataService.getOwnPrivateKey()).thenReturn("not-a-valid-key");

        Model model = new ConcurrentModel();
        String ret = oidcController.loginInitiations(httpServletRequest, model);

        assertEquals(TextConstants.LTI3ERROR, ret);
        assertNotNull(model.getAttribute(TextConstants.ERROR));
    }

    @Test
    void loginInitiationsPersistsGeneratedNonceToRepository() {
        when(platformDeploymentRepository.findByIss(ISS)).thenReturn(List.of(platformDeployment));
        when(ltiDataService.getDemoMode()).thenReturn(false);

        oidcController.loginInitiations(httpServletRequest, new ConcurrentModel());

        ArgumentCaptor<LtiNonce> nonceCaptor = ArgumentCaptor.forClass(LtiNonce.class);
        verify(ltiNonceRepository).save(nonceCaptor.capture());
        assertNotNull(nonceCaptor.getValue().getNonce());
    }

    @Test
    void loginInitiationsDemoModeAddsAllParametersToModel() {
        when(platformDeploymentRepository.findByIss(ISS)).thenReturn(List.of(platformDeployment));
        when(ltiDataService.getDemoMode()).thenReturn(true);

        Model model = new ConcurrentModel();
        oidcController.loginInitiations(httpServletRequest, model);

        assertEquals(CLIENT_ID, model.getAttribute("client_id"));
        assertEquals("login-hint", model.getAttribute("login_hint"));
        assertEquals(OIDC_ENDPOINT, model.getAttribute("oicdEndpoint"));
        // demo mode short-circuits before the non-demo-mode-only attributes are added
        assertNull(model.getAttribute("targetLinkUri"));
    }

    @Test
    void loginInitiationsNonDemoModeDoesNotAddAnyModelAttributes() {
        when(platformDeploymentRepository.findByIss(ISS)).thenReturn(List.of(platformDeployment));
        when(ltiDataService.getDemoMode()).thenReturn(false);

        Model model = new ConcurrentModel();
        oidcController.loginInitiations(httpServletRequest, model);

        // non-demo-mode redirects straight to the platform now; nothing needs to be rendered, so
        // no model attributes are added at all
        assertTrue(model.asMap().isEmpty(), () -> "Expected no model attributes, got: " + model.asMap());
    }

    @Test
    void loginInitiationsVerboseLoggingEnabledStillSucceeds() {
        ReflectionTestUtils.setField(oidcController, "ltiDataVerboseLoggingEnabled", true);
        when(platformDeploymentRepository.findByIss(ISS)).thenReturn(List.of(platformDeployment));
        when(ltiDataService.getDemoMode()).thenReturn(false);

        String ret = oidcController.loginInitiations(httpServletRequest, new ConcurrentModel());

        assertTrue(ret.startsWith("redirect:" + OIDC_ENDPOINT), () -> "Expected a redirect to the platform's OIDC endpoint, got: " + ret);
    }

}
