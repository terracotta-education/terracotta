package edu.iu.terracotta.connectors.oneedtech.service.lms.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.dao.exceptions.FeatureNotFoundException;

/**
 * Unlike the Canvas/Brightspace equivalents, {@link OneEdTechLmsOAuthServiceImpl} does not yet
 * implement OAuth token exchange or refresh: every method besides {@code createRestTemplate}
 * is a no-op stub. These tests pin down that current (intentional-looking, but effectively
 * unimplemented) behavior so a future implementation change is a deliberate, visible diff.
 */
public class OneEdTechLmsOAuthServiceImplTest {

    private OneEdTechLmsOAuthServiceImpl oneEdTechLmsOAuthService;

    private PlatformDeployment platformDeployment;
    private LtiUserEntity user;

    @BeforeEach
    public void beforeEach() {
        oneEdTechLmsOAuthService = new OneEdTechLmsOAuthServiceImpl();

        platformDeployment = PlatformDeployment.builder()
            .keyId(1L)
            .localUrl("https://terracotta.example.com")
            .build();

        user = LtiUserEntity.builder()
            .userId(1L)
            .platformDeployment(platformDeployment)
            .build();
    }

    @Test
    public void testIsConfiguredReturnsFalse() {
        assertFalse(oneEdTechLmsOAuthService.isConfigured(platformDeployment));
    }

    @Test
    public void testIsConfiguredReturnsFalseForNullPlatformDeployment() {
        assertFalse(oneEdTechLmsOAuthService.isConfigured(null));
    }

    @Test
    public void testGetAuthorizationRequestURIReturnsNull() throws LmsOAuthException, FeatureNotFoundException {
        assertNull(oneEdTechLmsOAuthService.getAuthorizationRequestURI(platformDeployment, "state"));
    }

    @Test
    public void testFetchAndSaveAccessTokenReturnsNull() throws LmsOAuthException, FeatureNotFoundException {
        assertNull(oneEdTechLmsOAuthService.fetchAndSaveAccessToken(user, "code"));
    }

    @Test
    public void testGetAccessTokenReturnsNull() throws LmsOAuthException {
        assertNull(oneEdTechLmsOAuthService.getAccessToken(user));
    }

    @Test
    public void testIsAccessTokenAvailableReturnsFalse() {
        assertFalse(oneEdTechLmsOAuthService.isAccessTokenAvailable(user));
    }

    @Test
    public void testCreateRestTemplateReturnsBufferingRestTemplate() {
        RestTemplate restTemplate = oneEdTechLmsOAuthService.createRestTemplate();

        assertNotNull(restTemplate);
        assertNotNull(restTemplate.getRequestFactory());
        assertInstanceOf(BufferingClientHttpRequestFactory.class, restTemplate.getRequestFactory());
    }

    @Test
    public void testCreateRestTemplateReturnsSharedInstance() {
        RestTemplate first = oneEdTechLmsOAuthService.createRestTemplate();
        RestTemplate second = oneEdTechLmsOAuthService.createRestTemplate();

        assertSame(first, second);
    }

}
