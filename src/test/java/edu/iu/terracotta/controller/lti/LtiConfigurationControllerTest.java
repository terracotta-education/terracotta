package edu.iu.terracotta.controller.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.utils.TextConstants;

public class LtiConfigurationControllerTest extends BaseTest {

    @InjectMocks private LtiConfigurationController ltiConfigurationController;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    void displayConfigsEmptyTest() {
        when(platformDeploymentRepository.findAll()).thenReturn(Collections.emptyList());

        ResponseEntity<List<PlatformDeployment>> response = ltiConfigurationController.displayConfigs(httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void displayConfigsTest() {
        PlatformDeployment platformDeployment = PlatformDeployment.builder().keyId(1L).iss("iss").clientId("client").build();
        when(platformDeploymentRepository.findAll()).thenReturn(List.of(platformDeployment));

        ResponseEntity<List<PlatformDeployment>> response = ltiConfigurationController.displayConfigs(httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void displayConfigFoundTest() {
        PlatformDeployment platformDeployment = PlatformDeployment.builder().keyId(1L).build();
        when(platformDeploymentRepository.findById(1L)).thenReturn(Optional.of(platformDeployment));

        ResponseEntity<?> response = ltiConfigurationController.displayConfig(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(platformDeployment, response.getBody());
    }

    @Test
    void displayConfigNotFoundTest() {
        when(platformDeploymentRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = ltiConfigurationController.displayConfig(99L, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains(TextConstants.NOT_FOUND_SUFFIX));
    }

    @Test
    void createDeploymentConflictTest() {
        PlatformDeployment newDeployment = PlatformDeployment.builder().iss("iss").clientId("client").build();
        when(platformDeploymentRepository.findByIssAndClientId("iss", "client")).thenReturn(List.of(newDeployment));

        ResponseEntity<String> response = ltiConfigurationController.createDeployment(newDeployment, UriComponentsBuilder.newInstance());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void createDeploymentNoToolDeploymentsTest() {
        PlatformDeployment newDeployment = PlatformDeployment.builder().iss("iss").clientId("client").build();
        PlatformDeployment saved = PlatformDeployment.builder().keyId(5L).iss("iss").clientId("client").build();
        when(platformDeploymentRepository.findByIssAndClientId("iss", "client")).thenReturn(Collections.emptyList());
        when(platformDeploymentRepository.save(newDeployment)).thenReturn(saved);

        ResponseEntity<String> response = ltiConfigurationController.createDeployment(newDeployment, UriComponentsBuilder.newInstance());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().contains("/config/5"));
    }

    @Test
    void createDeploymentWithToolDeploymentsTest() {
        ToolDeployment toolDeployment = ToolDeployment.builder().ltiDeploymentId("dep1").build();
        PlatformDeployment newDeployment = PlatformDeployment.builder().iss("iss").clientId("client").toolDeployments(Set.of(toolDeployment)).build();
        PlatformDeployment saved = PlatformDeployment.builder().keyId(5L).iss("iss").clientId("client").build();
        when(platformDeploymentRepository.findByIssAndClientId("iss", "client")).thenReturn(Collections.emptyList());
        when(platformDeploymentRepository.save(newDeployment)).thenReturn(saved);
        when(toolDeploymentRepository.save(toolDeployment)).thenReturn(toolDeployment);

        ResponseEntity<String> response = ltiConfigurationController.createDeployment(newDeployment, UriComponentsBuilder.newInstance());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(saved, toolDeployment.getPlatformDeployment());
    }

    @Test
    void updateDeploymentNotFoundTest() {
        PlatformDeployment update = PlatformDeployment.builder().build();
        when(platformDeploymentRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = ltiConfigurationController.updateDeployment(1L, update);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateDeploymentTest() {
        PlatformDeployment existing = PlatformDeployment.builder().keyId(1L).toolDeployments(new HashSet<>()).build();
        PlatformDeployment update = PlatformDeployment.builder()
            .oAuth2TokenUrl("url")
            .clientId("client")
            .iss("iss")
            .oidcEndpoint("oidc")
            .jwksEndpoint("jwks")
            .enableAutomaticDeployments(true)
            .toolDeployments(Collections.emptySet())
            .build();
        when(platformDeploymentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(platformDeploymentRepository.saveAndFlush(existing)).thenReturn(existing);

        ResponseEntity<?> response = ltiConfigurationController.updateDeployment(1L, update);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("client", existing.getClientId());
        assertEquals("iss", existing.getIss());
        assertEquals(Boolean.TRUE, existing.getEnableAutomaticDeployments());
    }

    @Test
    void updateDeploymentAddsMissingToolDeploymentTest() {
        Set<ToolDeployment> existingToolDeployments = new HashSet<>();
        PlatformDeployment existing = PlatformDeployment.builder().keyId(1L).toolDeployments(existingToolDeployments).build();
        ToolDeployment newToolDeployment = ToolDeployment.builder().ltiDeploymentId("dep-99").build();
        PlatformDeployment update = PlatformDeployment.builder().iss("iss").clientId("client").toolDeployments(Set.of(newToolDeployment)).build();

        when(platformDeploymentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(toolDeploymentRepository.save(newToolDeployment)).thenReturn(newToolDeployment);
        when(platformDeploymentRepository.saveAndFlush(existing)).thenReturn(existing);

        ResponseEntity<?> response = ltiConfigurationController.updateDeployment(1L, update);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(existingToolDeployments.contains(newToolDeployment));
    }

    @Test
    void updateDeploymentNullToolDeploymentsTest() {
        PlatformDeployment existing = PlatformDeployment.builder().keyId(1L).toolDeployments(new HashSet<>()).build();
        PlatformDeployment update = PlatformDeployment.builder().iss("iss").clientId("client").build();

        when(platformDeploymentRepository.findById(1L)).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = ltiConfigurationController.updateDeployment(1L, update);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
