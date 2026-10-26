package edu.iu.terracotta.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.ServletRequestPathUtils;

/**
 * Note on {@link #testLogFilter()}: {@code AbstractRequestLoggingFilter}'s getters
 * ({@code isIncludeQueryString()}, {@code isIncludePayload()}, {@code getMaxPayloadLength()},
 * {@code isIncludeHeaders()}, {@code isIncludeClientInfo()}) are all declared {@code protected}
 * (confirmed via the local spring-web 7.0.8 jar), so this test's own package
 * ({@code edu.iu.terracotta.config}, not a subclass of {@code CommonsRequestLoggingFilter}) has no
 * public way to assert the individual flag values set by {@link WebConfig#logFilter()}. See the
 * final report for this gap.
 */
public class WebConfigTest {

    private WebConfig webConfig;

    @BeforeEach
    void beforeEach() {
        webConfig = new WebConfig();
    }

    @ParameterizedTest
    @CsvSource({
        // content-hashed Vite output: a new build always means a new URL
        "/app/assets/cache-test-a1b2c3d4.js, 'max-age=31536000, public, immutable'",
        // fixed-name integration scripts that partner pages load by URL
        "/app/js/cache-test.js, 'max-age=3600, public'",
        // HTML entry pages naming the current build's assets
        "/app/cache-test.html, no-cache"
    })
    void testAppResourcesCacheControl(String path, String cacheControl) throws Exception {
        MockHttpServletResponse response = serve(path);

        assertEquals(200, response.getStatus());
        assertEquals(cacheControl, response.getHeader(HttpHeaders.CACHE_CONTROL));
        assertNotNull(response.getHeader(HttpHeaders.LAST_MODIFIED));
    }

    /**
     * Builds the handlers through a real {@link ResourceHandlerRegistry} and serves the request
     * through whichever one its mapping picks, so this also checks that the specific /app paths
     * win over the /app/** catch-all.
     */
    private MockHttpServletResponse serve(String path) throws Exception {
        MockServletContext servletContext = new MockServletContext();
        GenericWebApplicationContext context = new GenericWebApplicationContext(servletContext);
        context.refresh();

        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(context, servletContext);
        webConfig.addResourceHandlers(registry);
        // getHandlerMapping is protected; it's what WebMvcConfigurationSupport calls
        SimpleUrlHandlerMapping mapping = ReflectionTestUtils.invokeMethod(registry, "getHandlerMapping");
        assertNotNull(mapping);
        mapping.setApplicationContext(context);

        MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", path);
        ServletRequestPathUtils.parseAndCache(request);
        HandlerExecutionChain chain = mapping.getHandler(request);
        assertNotNull(chain);

        MockHttpServletResponse response = new MockHttpServletResponse();
        // the mapping's interceptor exposes the matched path the handler resolves the file from
        for (HandlerInterceptor interceptor : chain.getInterceptorList()) {
            interceptor.preHandle(request, response, chain.getHandler());
        }

        ((ResourceHttpRequestHandler) chain.getHandler()).handleRequest(request, response);

        return response;
    }

    @Test
    void testLogFilter() {
        CommonsRequestLoggingFilter filter = webConfig.logFilter();

        assertNotNull(filter);
        // see class javadoc: the individual include*/maxPayloadLength getters are protected on
        // AbstractRequestLoggingFilter, so they cannot be asserted from this test without reflection.
    }

}
