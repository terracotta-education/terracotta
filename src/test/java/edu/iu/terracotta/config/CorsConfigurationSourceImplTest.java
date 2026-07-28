package edu.iu.terracotta.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;

import jakarta.servlet.http.HttpServletRequest;

/**
 * {@link CorsConfigurationSourceImpl#corsConfiguration} is built once, in the field initializer,
 * via the inner {@code DefaultCorsConfiguration} class. {@code applyPermitDefaultValues()} seeds
 * {@code allowedMethods} with GET/HEAD/POST (Spring's {@code DEFAULT_PERMIT_METHODS}) and
 * {@code allowedHeaders} with the wildcard {@code "*"}; the constructor then clears the headers
 * and re-adds the five explicit ones, and appends PUT/OPTIONS/DELETE/PATCH on top of the
 * permit-default methods (verified against the {@code CorsConfiguration} source in the local
 * spring-web 7.0.8 sources jar).
 */
@ExtendWith(MockitoExtension.class)
public class CorsConfigurationSourceImplTest {

    @Mock private HttpServletRequest request;

    private CorsConfigurationSourceImpl corsConfigurationSource;

    @BeforeEach
    void beforeEach() {
        corsConfigurationSource = new CorsConfigurationSourceImpl();
    }

    @Test
    void testGetCorsConfigurationNotNull() {
        assertNotNull(corsConfigurationSource.getCorsConfiguration(request));
    }

    @Test
    void testGetCorsConfigurationIgnoresNullRequest() {
        // the request parameter is unused by the implementation; a fixed configuration is
        // always returned regardless of what (if anything) is passed in.
        assertNotNull(corsConfigurationSource.getCorsConfiguration(null));
    }

    @Test
    void testGetCorsConfigurationReturnsSameInstance() {
        CorsConfiguration first = corsConfigurationSource.getCorsConfiguration(request);
        CorsConfiguration second = corsConfigurationSource.getCorsConfiguration(null);

        assertSame(first, second);
    }

    @Test
    void testAllowedHeaders() {
        CorsConfiguration corsConfiguration = corsConfigurationSource.getCorsConfiguration(request);

        assertEquals(
            List.of("Origin", "X-Requested-With", "Content-Type", "Accept", "Authorization"),
            corsConfiguration.getAllowedHeaders()
        );
    }

    @Test
    void testAllowedMethods() {
        CorsConfiguration corsConfiguration = corsConfigurationSource.getCorsConfiguration(request);

        // GET, HEAD, POST come from applyPermitDefaultValues(); PUT, OPTIONS, DELETE, PATCH are
        // appended on top by the DefaultCorsConfiguration constructor.
        assertEquals(
            List.of(
                HttpMethod.GET.name(),
                HttpMethod.HEAD.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.OPTIONS.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.PATCH.name()
            ),
            corsConfiguration.getAllowedMethods()
        );
    }

    @Test
    void testAllowedOriginsPermitDefault() {
        CorsConfiguration corsConfiguration = corsConfigurationSource.getCorsConfiguration(request);

        // applyPermitDefaultValues() allows all origins ("*") and is never overridden afterward.
        assertEquals(List.of("*"), corsConfiguration.getAllowedOrigins());
    }

    @Test
    void testMaxAgePermitDefault() {
        CorsConfiguration corsConfiguration = corsConfigurationSource.getCorsConfiguration(request);

        // applyPermitDefaultValues() sets max age to 1800 seconds and is never overridden afterward.
        assertEquals(1800L, corsConfiguration.getMaxAge());
    }

}
