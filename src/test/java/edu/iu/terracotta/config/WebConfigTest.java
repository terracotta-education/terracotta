package edu.iu.terracotta.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

/**
 * Note on {@link #testLogFilter()}: {@code AbstractRequestLoggingFilter}'s getters
 * ({@code isIncludeQueryString()}, {@code isIncludePayload()}, {@code getMaxPayloadLength()},
 * {@code isIncludeHeaders()}, {@code isIncludeClientInfo()}) are all declared {@code protected}
 * (confirmed via the local spring-web 7.0.8 jar), so this test's own package
 * ({@code edu.iu.terracotta.config}, not a subclass of {@code CommonsRequestLoggingFilter}) has no
 * public way to assert the individual flag values set by {@link WebConfig#logFilter()}. See the
 * final report for this gap.
 */
@ExtendWith(MockitoExtension.class)
public class WebConfigTest {

    @Mock private ResourceHandlerRegistry registry;
    @Mock private ResourceHandlerRegistration registration;

    private WebConfig webConfig;

    @BeforeEach
    void beforeEach() {
        webConfig = new WebConfig();
    }

    @Test
    void testAddResourceHandlers() {
        when(
            registry.addResourceHandler(
                "/webjars/**",
                "/img/**",
                "/css/**",
                "/js/**",
                "/app/**"
            )
        ).thenReturn(registration);

        webConfig.addResourceHandlers(registry);

        verify(registry).addResourceHandler(
            "/webjars/**",
            "/img/**",
            "/css/**",
            "/js/**",
            "/app/**"
        );
        verify(registration).addResourceLocations(
            "classpath:/META-INF/resources/webjars/",
            "classpath:/static/img/",
            "classpath:/static/css/",
            "classpath:/static/js/",
            "classpath:/static/app/"
        );
    }

    @Test
    void testLogFilter() {
        CommonsRequestLoggingFilter filter = webConfig.logFilter();

        assertNotNull(filter);
        // see class javadoc: the individual include*/maxPayloadLength getters are protected on
        // AbstractRequestLoggingFilter, so they cannot be asserted from this test without reflection.
    }

}
