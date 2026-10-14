package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthServiceManager;

public class LmsAuthorizationControllerTest extends BaseTest {

    @Mock private LmsOAuthServiceManager lmsOAuthServiceManager;

    private LmsAuthorizationController lmsAuthorizationController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // constructed manually for the same reason as LmsOAuthControllerTest: @InjectMocks could
        // wire the inherited canvasApiJwtService mock as the ApiJwtService
        lmsAuthorizationController = new LmsAuthorizationController(apiJwtService, lmsOAuthServiceManager, ltiUserRepository);

        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(securedInfo.getUserId()).thenReturn("user1");
        when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId("user1", 1L)).thenReturn(ltiUserEntity);
        when(ltiUserEntity.getPlatformDeployment()).thenReturn(platformDeployment);
        doReturn(lmsOAuthService).when(lmsOAuthServiceManager).getLmsOAuthService(1L);
        when(lmsOAuthService.isConfigured(platformDeployment)).thenReturn(true);
    }

    @Test
    void authorizedWhenTheUserHasAToken() throws Exception {
        when(lmsOAuthService.isAccessTokenAvailable(ltiUserEntity)).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> response = lmsAuthorizationController.getAuthorization(httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("authorized", true), response.getBody());
    }

    @Test
    void notAuthorizedWithoutAToken() throws Exception {
        when(lmsOAuthService.isAccessTokenAvailable(ltiUserEntity)).thenReturn(false);

        ResponseEntity<Map<String, Boolean>> response = lmsAuthorizationController.getAuthorization(httpServletRequest);

        assertEquals(Map.of("authorized", false), response.getBody());
    }

    @Test
    void authorizedWhenLmsOAuthIsNotConfigured() throws Exception {
        when(lmsOAuthService.isConfigured(platformDeployment)).thenReturn(false);

        ResponseEntity<Map<String, Boolean>> response = lmsAuthorizationController.getAuthorization(httpServletRequest);

        assertEquals(Map.of("authorized", true), response.getBody());
        verify(lmsOAuthService, never()).isAccessTokenAvailable(any(LtiUserEntity.class));
    }

    @Test
    void notAuthorizedWhenTheUserIsUnknown() throws Exception {
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId("user1", 1L)).thenReturn(null);

        ResponseEntity<Map<String, Boolean>> response = lmsAuthorizationController.getAuthorization(httpServletRequest);

        assertEquals(Map.of("authorized", false), response.getBody());
    }

    @Test
    void unauthorizedForLearners() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Map<String, Boolean>> response = lmsAuthorizationController.getAuthorization(httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verify(lmsOAuthService, never()).isAccessTokenAvailable(any(LtiUserEntity.class));
    }

}
