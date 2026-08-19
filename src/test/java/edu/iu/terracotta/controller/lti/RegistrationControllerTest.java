package edu.iu.terracotta.controller.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import com.sun.net.httpserver.HttpServer;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.PlatformRegistrationDto;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.ToolRegistrationDto;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.service.lti.RegistrationService;
import edu.iu.terracotta.utils.LtiStrings;
import jakarta.servlet.http.HttpSession;

public class RegistrationControllerTest extends BaseTest {

    @Mock private RegistrationService registrationService;
    @Mock private HttpSession httpSession;

    private RegistrationController registrationController;
    private HttpServer openidConfigurationServer;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        registrationController = new RegistrationController(platformDeploymentRepository, apiJwtService, registrationService);

        ReflectionTestUtils.setField(registrationController, "clientName", "Terracotta");
        ReflectionTestUtils.setField(registrationController, "description", "Terracotta description");

        when(httpServletRequest.getSession()).thenReturn(httpSession);
    }

    @AfterEach
    public void afterEach() {
        if (openidConfigurationServer != null) {
            openidConfigurationServer.stop(0);
        }
    }

    @Test
    void registrationPlatformDeploymentNotFoundReturnsErrorViewTest() throws Exception {
        // securedInfo.getPlatformDeploymentId() default-stubbed to 1L in BaseModelTest; force lookup to miss.
        when(platformDeploymentRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);

        Model model = new ConcurrentModel();

        String view = registrationController.registration("http://openid.config", "regToken", httpServletRequest, model);

        assertEquals("registrationError", view);
    }

    @Test
    void registrationHttpCallFailureReturnsErrorViewTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        // platformDeploymentRepository.findById(anyLong()) -> Optional.of(platformDeployment) already stubbed by BaseRepositoryTest.

        Model model = new ConcurrentModel();

        // The controller builds its own RestTemplate internally (not injected), so we can't mock the
        // HTTP call. A syntactically invalid URI (unmatched bracket) fails URI parsing synchronously,
        // with no real network I/O, and exercises the catch-all error branch exactly as a genuine
        // connection failure would.
        String view = registrationController.registration("http://[invalid", "regToken", httpServletRequest, model);

        assertEquals("registrationError", view);
        assertNotNull(model.getAttribute("Error"));
        assertEquals(platformDeployment.getLocalUrl() + "/registration/", model.getAttribute("own_redirect_post_endpoint"));
    }

    @Test
    void registrationHappyPathGeneratesToolConfigurationAndRedirectsTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        // platformDeploymentRepository.findById(anyLong()) -> Optional.of(platformDeployment) already stubbed by BaseRepositoryTest.

        // The controller builds its own RestTemplate internally (not injected), so a real HTTP call
        // is the only way to reach the success branch (and therefore generateToolConfiguration()).
        // A tiny loopback JDK HttpServer stands in for the platform's openid-configuration endpoint.
        String responseBody = "{\"issuer\":\"https://issuer.example.com\",\"registration_endpoint\":\"https://issuer.example.com/register\"}";
        openidConfigurationServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        openidConfigurationServer.createContext(
            "/openid-configuration",
            exchange -> {
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        );
        openidConfigurationServer.start();
        String openidConfigurationUrl = "http://localhost:" + openidConfigurationServer.getAddress().getPort() + "/openid-configuration";

        Model model = new ConcurrentModel();

        String view = registrationController.registration(openidConfigurationUrl, "regToken", httpServletRequest, model);

        assertEquals("registrationRedirect", view);

        ToolRegistrationDto toolRegistrationDto = (ToolRegistrationDto) model.getAttribute(LtiStrings.TOOL_CONFIGURATION);
        assertNotNull(toolRegistrationDto);
        assertEquals("web", toolRegistrationDto.getApplication_type());
        assertEquals("Terracotta", toolRegistrationDto.getClient_name());
        assertEquals(platformDeployment.getLocalUrl() + "/oidc/login_initiations", toolRegistrationDto.getInitiate_login_uri());
        assertEquals(platformDeployment.getLocalUrl() + "/jwks/jwk", toolRegistrationDto.getJwks_uri());
        assertEquals("private_key_jwt", toolRegistrationDto.getToken_endpoint_auth_method());
        assertTrue(toolRegistrationDto.getScope().contains("openid"));
        assertTrue(toolRegistrationDto.getScope().contains("https://purl.imsglobal.org/spec/lti/scope/noticehandlers"));
        assertNotNull(toolRegistrationDto.getToolConfiguration());
        assertEquals("lti.url", toolRegistrationDto.getToolConfiguration().getDomain());
        assertEquals("Terracotta description", toolRegistrationDto.getToolConfiguration().getDescription());
        assertEquals(2, toolRegistrationDto.getToolConfiguration().getMessages_supported().size());

        PlatformRegistrationDto platformRegistrationDto = (PlatformRegistrationDto) model.getAttribute(LtiStrings.PLATFORM_CONFIGURATION);
        assertNotNull(platformRegistrationDto);
        assertEquals("https://issuer.example.com", platformRegistrationDto.getIssuer());
    }

    @Test
    void registrationPOSTHappyPathTest() throws Exception {
        String token = "regToken";
        String encodedIssuer = URLEncoder.encode("https://issuer.example.com", StandardCharsets.UTF_8.name());
        PlatformRegistrationDto platformRegistrationDto = PlatformRegistrationDto.builder()
            .issuer(encodedIssuer)
            .registration_endpoint("https://issuer.example.com/register")
            .build();
        ToolRegistrationDto toolRegistrationDto = ToolRegistrationDto.builder().build();

        when(httpSession.getAttribute(LtiStrings.REGISTRATION_TOKEN)).thenReturn(token);
        when(httpSession.getAttribute(LtiStrings.PLATFORM_CONFIGURATION)).thenReturn(platformRegistrationDto);
        when(httpSession.getAttribute(LtiStrings.TOOL_CONFIGURATION)).thenReturn(toolRegistrationDto);
        when(registrationService.callDynamicRegistration(token, toolRegistrationDto, platformRegistrationDto.getRegistration_endpoint())).thenReturn("registration succeeded");

        Model model = new ConcurrentModel();

        String view = registrationController.registrationPOST(httpServletRequest, model);

        assertEquals("registrationConfirmation", view);
        assertEquals("registration succeeded", model.getAttribute("registration_confirmation"));
        assertEquals("https://issuer.example.com", model.getAttribute("issuer"));
    }

    @Test
    void registrationPOSTConnectionExceptionKeepsDefaultAnswerTest() throws Exception {
        String token = "regToken";
        String encodedIssuer = URLEncoder.encode("https://issuer.example.com", StandardCharsets.UTF_8.name());
        PlatformRegistrationDto platformRegistrationDto = PlatformRegistrationDto.builder()
            .issuer(encodedIssuer)
            .registration_endpoint("https://issuer.example.com/register")
            .build();
        ToolRegistrationDto toolRegistrationDto = ToolRegistrationDto.builder().build();

        when(httpSession.getAttribute(LtiStrings.REGISTRATION_TOKEN)).thenReturn(token);
        when(httpSession.getAttribute(LtiStrings.PLATFORM_CONFIGURATION)).thenReturn(platformRegistrationDto);
        when(httpSession.getAttribute(LtiStrings.TOOL_CONFIGURATION)).thenReturn(toolRegistrationDto);
        when(registrationService.callDynamicRegistration(token, toolRegistrationDto, platformRegistrationDto.getRegistration_endpoint())).thenThrow(new ConnectionException("network down"));

        Model model = new ConcurrentModel();

        String view = registrationController.registrationPOST(httpServletRequest, model);

        assertEquals("registrationConfirmation", view);
        assertEquals("Error during the registration", model.getAttribute("registration_confirmation"));
        assertEquals("https://issuer.example.com", model.getAttribute("issuer"));
    }

    @Test
    void registrationPOSTMissingSessionDataThrowsNpeTest() {
        // BUG: if the platform configuration was never stored in the session (e.g. session expired,
        // or the GET /registration/ step never completed), platformRegistrationDto is null and
        // platformRegistrationDto.getRegistration_endpoint() throws an uncaught NullPointerException -
        // only ConnectionException is caught here, so this crashes instead of showing a friendly error.
        when(httpSession.getAttribute(LtiStrings.REGISTRATION_TOKEN)).thenReturn("regToken");
        when(httpSession.getAttribute(LtiStrings.PLATFORM_CONFIGURATION)).thenReturn(null);

        Model model = new ConcurrentModel();

        assertThrows(NullPointerException.class, () -> registrationController.registrationPOST(httpServletRequest, model));
    }

}
