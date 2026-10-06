package edu.iu.terracotta.security.app.roles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.api.ApiTokenService;
import edu.iu.terracotta.controller.app.ConsentFileController;
import edu.iu.terracotta.dao.model.dto.FileInfoDto;
import edu.iu.terracotta.security.app.ApiOAuthProviderProcessingFilter;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.FileStorageService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

/**
 * The whole path a role takes: ApiOAuthProviderProcessingFilter grants roles from the request's
 * API token, then method security enforces a real controller's role annotation through its Spring
 * proxy. The controller tests call handler methods directly, which bypasses the proxy, so this is
 * where the annotations are actually exercised.
 */
class ControllerRoleEnforcementTest {

    private static final String TOKEN = "api-token";

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    static class LearnerEndpoint {

        @LearnerOrHigher
        public String call() {
            return "ok";
        }

    }

    private final ApiJwtService apiJwtService = mock(ApiJwtService.class);
    private final ExperimentService experimentService = mock(ExperimentService.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final SecuredInfo securedInfo = SecuredInfo.builder().userId("user-1").build();

    private AnnotationConfigApplicationContext context;
    private ApiOAuthProviderProcessingFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() throws Exception {
        context = new AnnotationConfigApplicationContext();
        context.register(MethodSecurityConfig.class);
        context.registerBean(ConsentFileController.class, () -> new ConsentFileController(apiJwtService, experimentService, fileStorageService));
        context.registerBean(LearnerEndpoint.class);
        context.refresh();

        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, mock(ApiTokenService.class));

        Jws<Claims> jws = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(jws.getPayload()).thenReturn(claims);
        when(claims.getIssuer()).thenReturn("TERRACOTTA");
        when(apiJwtService.validateToken(TOKEN)).thenReturn(jws);
        when(apiJwtService.extractValues(jws)).thenReturn(securedInfo);

        when(apiJwtService.extractValues(any(), eq(false))).thenReturn(securedInfo);
        when(experimentService.getExperimentIdByUuid(any())).thenReturn(1L);
        when(fileStorageService.uploadConsentFile(anyLong(), anyString(), any(), any())).thenReturn(FileInfoDto.builder().build());
    }

    @AfterEach
    void afterEach() {
        context.close();
        SecurityContextHolder.clearContext();
    }

    private void grant(boolean admin, boolean instructor, boolean learner) {
        when(apiJwtService.isAdmin(securedInfo)).thenReturn(admin);
        when(apiJwtService.isInstructor(securedInfo)).thenReturn(instructor);
        when(apiJwtService.isLearner(securedInfo)).thenReturn(learner);
    }

    /**
     * Runs the call inside the filter, as a real request would, and returns what it returned or
     * the exception it threw.
     */
    private Object throughFilter(ThrowingCall call) throws Exception {
        AtomicReference<Object> outcome = new AtomicReference<>();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            try {
                outcome.set(call.call());
            } catch (Exception e) {
                outcome.set(e);
            }
        });

        return outcome.get();
    }

    @FunctionalInterface
    interface ThrowingCall {
        Object call() throws Exception;
    }

    private Object postConsent() throws Exception {
        ConsentFileController controller = context.getBean(ConsentFileController.class);
        MockMultipartFile pdf = new MockMultipartFile("consent", "consent.pdf", "application/pdf", new byte[] { 1 });

        return throughFilter(() -> controller.postConsent(pdf, UUID.randomUUID(), "Consent", new MockHttpServletRequest()));
    }

    @Test
    void instructorOrHigherRefusesALearner() throws Exception {
        grant(false, false, true);

        assertInstanceOf(AccessDeniedException.class, postConsent());
    }

    @Test
    void instructorOrHigherRefusesATokenWithNoRole() throws Exception {
        grant(false, false, false);

        assertInstanceOf(AccessDeniedException.class, postConsent());
    }

    @Test
    void instructorOrHigherAllowsAnInstructor() throws Exception {
        grant(false, true, false);

        assertEquals(HttpStatus.OK, ((ResponseEntity<?>) postConsent()).getStatusCode());
    }

    @Test
    void instructorOrHigherAllowsAnAdmin() throws Exception {
        grant(true, false, false);

        assertEquals(HttpStatus.OK, ((ResponseEntity<?>) postConsent()).getStatusCode());
    }

    @Test
    void learnerOrHigherAllowsALearnerAnInstructorAndAnAdmin() throws Exception {
        LearnerEndpoint endpoint = context.getBean(LearnerEndpoint.class);

        grant(false, false, true);
        assertEquals("ok", throughFilter(endpoint::call));

        grant(false, true, false);
        assertEquals("ok", throughFilter(endpoint::call));

        grant(true, false, false);
        assertEquals("ok", throughFilter(endpoint::call));
    }

    @Test
    void learnerOrHigherRefusesATokenWithNoRole() throws Exception {
        grant(false, false, false);

        assertInstanceOf(AccessDeniedException.class, throughFilter(context.getBean(LearnerEndpoint.class)::call));
    }

    @Test
    void theFilterClearsTheRolesAfterTheRequest() throws Exception {
        grant(false, true, false);

        postConsent();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

}
