package edu.iu.terracotta.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.api.ApiTokenService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiJwtService;
import edu.iu.terracotta.security.app.ApiOAuthProviderProcessingFilter;
import edu.iu.terracotta.security.lti.Lti3OAuthProviderProcessingFilter;

/**
 * Unit tests for {@link WebSecurityConfig}.
 *
 * <p>{@code ApiJwtService}, {@code ApiTokenService}, {@code LtiDataService}, and
 * {@code LtiJwtService} are plain interfaces, so they are mocked directly here rather than pulling
 * in the shared {@code edu.iu.terracotta.base.BaseTest}/{@code BaseServiceTest} hierarchy. That
 * hierarchy documents (see the {@code @InjectMocks} pitfall comment in {@code BaseServiceTest}
 * around its {@code apiJwtService}/{@code canvasApiJwtService} mocks) that classes such as
 * {@code CanvasApiJwtServiceImpl} also implement {@code ApiJwtService}, so Mockito's
 * type-only constructor-injection matching for {@code @InjectMocks} can silently wire the wrong
 * mock. To avoid that ambiguity entirely, {@link WebSecurityConfig} is constructed manually here
 * via its real (Lombok {@code @RequiredArgsConstructor}-generated) constructor with locally
 * declared {@code @Mock} fields, instead of relying on {@code @InjectMocks}.</p>
 */
@ExtendWith(MockitoExtension.class)
class WebSecurityConfigTest {

    @Mock private ApiJwtService apiJwtService;
    @Mock private ApiTokenService apiTokenService;
    @Mock private LtiDataService ltiDataService;
    @Mock private LtiJwtService ltiJwtService;

    private WebSecurityConfig webSecurityConfig;

    @BeforeEach
    void beforeEach() {
        webSecurityConfig = new WebSecurityConfig(apiJwtService, apiTokenService, ltiDataService, ltiJwtService);
    }

    @Test
    void testInitCreatesProcessingFilters() {
        assertNull(ReflectionTestUtils.getField(webSecurityConfig, "apiOAuthProviderProcessingFilter"));
        assertNull(ReflectionTestUtils.getField(webSecurityConfig, "lti3OAuthProviderProcessingFilter"));

        webSecurityConfig.init();

        Object apiFilter = ReflectionTestUtils.getField(webSecurityConfig, "apiOAuthProviderProcessingFilter");
        Object lti3Filter = ReflectionTestUtils.getField(webSecurityConfig, "lti3OAuthProviderProcessingFilter");

        assertNotNull(apiFilter);
        assertNotNull(lti3Filter);
        assertInstanceOf(ApiOAuthProviderProcessingFilter.class, apiFilter);
        assertInstanceOf(Lti3OAuthProviderProcessingFilter.class, lti3Filter);
    }

    @Test
    void testConfigureSimpleAuthUsersDefaultPasswordGeneratesRandomPassword() throws Exception {
        ReflectionTestUtils.setField(webSecurityConfig, "adminUser", "admin");
        ReflectionTestUtils.setField(webSecurityConfig, "adminPassword", "admin");

        AuthenticationManagerBuilder auth = new AuthenticationManagerBuilder(ObjectPostProcessor.identity());

        // should complete without throwing, even though the "real" generated password is never
        // exposed back to the caller
        assertDoesNotThrow(() -> webSecurityConfig.configureSimpleAuthUsers(auth));

        AuthenticationManager authenticationManager = auth.build();

        // the literal default "admin"/"admin" credentials must NOT work, since a random UUID
        // password was generated instead of using the literal default password
        assertThrows(
            BadCredentialsException.class,
            () -> authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("admin", "admin"))
        );
    }

    @Test
    void testConfigureSimpleAuthUsersCustomPasswordAuthenticatesSuccessfully() throws Exception {
        ReflectionTestUtils.setField(webSecurityConfig, "adminUser", "admin");
        ReflectionTestUtils.setField(webSecurityConfig, "adminPassword", "customSecretPw");

        AuthenticationManagerBuilder auth = new AuthenticationManagerBuilder(ObjectPostProcessor.identity());

        webSecurityConfig.configureSimpleAuthUsers(auth);

        AuthenticationManager authenticationManager = auth.build();

        Authentication result = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken("admin", "customSecretPw")
        );

        assertTrue(result.isAuthenticated());
        assertTrue(result.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
        assertTrue(result.getAuthorities().stream().anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));

        assertThrows(
            BadCredentialsException.class,
            () -> authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("admin", "wrongPassword"))
        );
    }

    // filterChain2/filterChain4/filterChain5/filterChain6 (the @Bean SecurityFilterChain methods)
    // are intentionally not covered here: they are thin wiring over HttpSecurity's fluent DSL,
    // whose authorizeHttpRequests/httpBasic/csrf/headers methods take Customizer<...> callbacks
    // invoked against further chained builder objects (e.g.
    // AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry). A Mockito stub
    // for HttpSecurity never actually invokes those callbacks, so a mocked HttpSecurity can at best
    // assert "didn't throw" without exercising any of the real configuration logic, and a
    // sufficiently deep mock/answer graph to make that meaningful would be fragile and would
    // essentially just re-implement Spring Security's own DSL. Fully exercising these methods
    // would require a Spring context (@WebMvcTest/@SpringBootTest + MockMvc), which is heavier than
    // this class's other genuinely unit-testable logic (init()/configureSimpleAuthUsers()).

}
