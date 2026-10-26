package edu.iu.terracotta.security.app.roles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import edu.iu.Terracotta;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lti.Roles;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.utils.TextConstants;

/**
 * Role annotations enforced over real HTTP: the real filter chains, the real API token filter
 * granting roles from a token the app itself signed, and method security on the real controllers.
 * Each call uses an experiment that doesn't exist, so a caller the role check lets through gets
 * some other error further in - never the role check's own 401.
 */
@SpringBootTest(
    classes = Terracotta.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "server.ssl.enabled=false",
        "aws.enabled=false",
        "lti13.demoMode=false",
        // an isolated in-memory database - this test must never touch a real one
        "spring.datasource.url=jdbc:h2:mem:controller-role-http-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@ActiveProfiles("test")
class ControllerRoleHttpTest {

    private static KeyPair keyPair;

    @LocalServerPort private int port;

    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private ApiJwtService apiJwtService;

    // java.net.http rather than the default HttpURLConnection client, which doesn't return the
    // body of a 401 to a DELETE
    private final RestTemplate restTemplate = new RestTemplate(new JdkClientHttpRequestFactory());
    private long platformDeploymentId;

    {
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
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
        StringBuilder pem = new StringBuilder("-----BEGIN ").append(type).append("-----\n");

        for (int i = 0; i < base64.length(); i += 64) {
            pem.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }

        return pem.append("-----END ").append(type).append("-----\n").toString();
    }

    @BeforeEach
    void beforeEach() {
        platformDeploymentId = platformDeploymentRepository.save(
            PlatformDeployment.builder()
                .iss("https://platform.example.com/" + UUID.randomUUID())
                .clientId("client-" + UUID.randomUUID())
                .oidcEndpoint("https://platform.example.com/oidc/auth")
                .lmsConnector(LmsConnector.CANVAS)
                .enableAutomaticDeployments(false)
                .build()
        ).getKeyId();
    }

    private String token(List<String> roles) throws Exception {
        return apiJwtService.buildJwt(false, roles, 1L, platformDeploymentId, "user-1", null, null, false,
            "1", "global-1", "login-1", "User One", "course-1", "assignment-1", "", "", "", "nonce", null, null);
    }

    private ResponseEntity<String> call(HttpMethod method, String path, List<String> roles) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token(roles));

        return restTemplate.exchange("http://localhost:" + port + path, method, new HttpEntity<>(headers), String.class);
    }

    private static void assertRefusedByRoleCheck(ResponseEntity<String> response) {
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    private static void assertPassedRoleCheck(ResponseEntity<String> response) {
        assertFalse(
            TextConstants.NOT_ENOUGH_PERMISSIONS.equals(response.getBody()),
            () -> "Expected the role check to let this caller through, got " + response.getStatusCode() + " " + response.getBody()
        );
    }

    @Test
    void anInstructorOnlyEndpointRefusesALearner() throws Exception {
        assertRefusedByRoleCheck(call(HttpMethod.DELETE, "/api/experiments/" + UUID.randomUUID(), Roles.STUDENT_ROLE_LIST));
    }

    @Test
    void anInstructorOnlyEndpointLetsAnInstructorThrough() throws Exception {
        assertPassedRoleCheck(call(HttpMethod.DELETE, "/api/experiments/" + UUID.randomUUID(), Roles.INSTRUCTOR_ROLE_LIST));
    }

    @Test
    void aLearnerOrHigherEndpointLetsALearnerThrough() throws Exception {
        assertPassedRoleCheck(call(HttpMethod.GET, "/api/experiments/" + UUID.randomUUID() + "/conditions", Roles.STUDENT_ROLE_LIST));
    }

    @Test
    void aLearnerOrHigherEndpointRefusesATokenWithNoRole() throws Exception {
        assertRefusedByRoleCheck(call(HttpMethod.GET, "/api/experiments/" + UUID.randomUUID() + "/conditions", List.of()));
    }

}
