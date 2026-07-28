package edu.iu.terracotta.security.lti;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import edu.iu.Terracotta;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;

/**
 * End-to-end verification that the OIDC login-initiation + LTI3 launch flow never sets any
 * {@code Set-Cookie} response header, i.e. that it is genuinely cookieless: unlike the rest of
 * this test suite (pure Mockito unit tests), this boots the real Spring context (real
 * {@code WebSecurityConfig} filter chains, real Spring Security session-management defaults) so it
 * can catch a session/cookie forced by framework machinery that no unit test of individual classes
 * would ever see.
 */
@SpringBootTest(
    classes = Terracotta.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "server.ssl.enabled=false",
        "aws.enabled=false",
        "lti13.demoMode=false",
        // force an isolated in-memory H2 instance, overriding any ambient/profile-based
        // datasource (e.g. a developer's local MySQL) regardless of active Spring profiles -
        // this test must never be able to touch a real database
        "spring.datasource.url=jdbc:h2:mem:lti3-cookieless-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@ActiveProfiles("test")
class Lti3LaunchCookielessVerificationTest {

    private static final String ISS = "https://platform.example.com";
    private static final String CLIENT_ID = "client-id-value";
    private static final String OIDC_ENDPOINT = "https://platform.example.com/oidc/auth";

    private static KeyPair keyPair;

    @LocalServerPort private int port;

    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;

    private final RestTemplate restTemplate = new RestTemplate(
        new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                // the redirect target (a fake platform host) doesn't actually exist - this test
                // needs to inspect the redirect response itself, not follow it
                connection.setInstanceFollowRedirects(false);
            }
        }
    );

    {
        // don't throw on 3xx/4xx/5xx: this test cares about response headers regardless of
        // status, and downstream business logic (e.g. Lti3Controller failing without a real
        // id_token) isn't the point of this check
        restTemplate.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        });
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @BeforeAll
    static void generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
    }

    @DynamicPropertySource
    static void registerSigningKeys(DynamicPropertyRegistry registry) {
        registry.add("oicd.privatekey", () -> pem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        registry.add("oicd.publickey", () -> pem("PUBLIC KEY", keyPair.getPublic().getEncoded()));
    }

    private static String pem(String type, byte[] der) {
        String base64 = Base64.getEncoder().encodeToString(der);
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(type).append("-----\n");

        for (int i = 0; i < base64.length(); i += 64) {
            pem.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }

        pem.append("-----END ").append(type).append("-----\n");

        return pem.toString();
    }

    @Test
    void loginInitiationAndLti3LaunchNeverSetCookies() {
        platformDeploymentRepository.save(
            PlatformDeployment.builder()
                .iss(ISS)
                .clientId(CLIENT_ID)
                .oidcEndpoint(OIDC_ENDPOINT)
                .lmsConnector(LmsConnector.CANVAS)
                .enableAutomaticDeployments(false)
                .build()
        );

        HttpHeaders formHeaders = new HttpHeaders();
        formHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> loginForm = new LinkedMultiValueMap<>();
        loginForm.add("iss", ISS);
        loginForm.add("login_hint", "login-hint");
        loginForm.add("target_link_uri", "https://tool.example.com/target");
        loginForm.add("lti_message_hint", "message-hint");
        loginForm.add("client_id", CLIENT_ID);

        ResponseEntity<String> loginResponse = restTemplate.exchange(
            url("/oidc/login_initiations"),
            HttpMethod.POST,
            new HttpEntity<>(loginForm, formHeaders),
            String.class
        );

        assertNull(
            loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE),
            () -> "Expected no Set-Cookie header from /oidc/login_initiations, got: " + loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE)
        );

        // loginInitiations redirects straight to the platform's OIDC endpoint now (no more
        // storage-access-check intermediate page); pull the generated `state` back out of the
        // Location header so it can be replayed against /lti3 below
        URI location = loginResponse.getHeaders().getLocation();
        assertNotNull(location, () -> "Expected a redirect Location header from /oidc/login_initiations, got status " + loginResponse.getStatusCode());

        Matcher matcher = Pattern.compile("[?&]state=([^&]+)").matcher(location.toString());
        assertTrue(matcher.find(), () -> "Could not find state= in redirect Location: " + location);
        String state = java.net.URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);

        MultiValueMap<String, String> lti3Form = new LinkedMultiValueMap<>();
        lti3Form.add("state", state);

        ResponseEntity<String> lti3Response = restTemplate.exchange(
            url("/lti3"),
            HttpMethod.POST,
            new HttpEntity<>(lti3Form, formHeaders),
            String.class
        );

        assertNull(
            lti3Response.getHeaders().get(HttpHeaders.SET_COOKIE),
            () -> "Expected no Set-Cookie header from /lti3, got: " + lti3Response.getHeaders().get(HttpHeaders.SET_COOKIE)
        );
    }

}
