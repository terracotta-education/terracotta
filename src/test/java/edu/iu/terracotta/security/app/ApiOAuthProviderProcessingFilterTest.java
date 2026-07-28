package edu.iu.terracotta.security.app;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.service.api.ApiTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiOAuthProviderProcessingFilterTest extends BaseTest {

    @Mock private ApiTokenService apiTokenService;
    @Mock private HttpServletRequest httpRequest;
    @Mock private HttpServletResponse httpResponse;
    @Mock private FilterChain filterChain;

    @SuppressWarnings("unchecked")
    private final Jws<Claims> jws = mock(Jws.class);
    private final Claims claims = mock(Claims.class);

    private ApiOAuthProviderProcessingFilter filter;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        when(jws.getPayload()).thenReturn(claims);
    }

    @Test
    void testConstructorNullApiJwtServiceThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new ApiOAuthProviderProcessingFilter(null, apiTokenService));
    }

    @Test
    void testConstructorNullApiTokenServiceThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new ApiOAuthProviderProcessingFilter(apiJwtService, null));
    }

    @Test
    void testDoFilterNonHttpServletRequestThrowsIllegalStateException() {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService);
        ServletRequest nonHttpRequest = mock(ServletRequest.class);

        assertThrows(IllegalStateException.class, () -> filter.doFilter(nonHttpRequest, httpResponse, filterChain));
    }

    @Test
    void testDoFilterMissingTokenNeverInvokesFilterChain() throws Exception {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService, false);
        when(httpRequest.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(httpResponse, never()).setStatus(anyInt());
    }

    @Test
    void testDoFilterAllowsQueryParamTokenWhenEnabled() throws Exception {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService, true);
        when(httpRequest.getHeader("Authorization")).thenReturn(null);
        when(httpRequest.getParameter("token")).thenReturn("query-token");
        when(apiJwtService.validateToken("query-token")).thenReturn(jws);
        when(claims.getIssuer()).thenReturn("TERRACOTTA");
        when(claims.get("oneUse")).thenReturn(false);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain).doFilter(httpRequest, httpResponse);
    }

    @Test
    void testDoFilterValidBearerTokenProceeds() throws Exception {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService);
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer abc123");
        when(apiJwtService.validateToken("abc123")).thenReturn(jws);
        when(claims.getIssuer()).thenReturn("TERRACOTTA");
        when(claims.get("oneUse")).thenReturn(false);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain).doFilter(httpRequest, httpResponse);
    }

    @Test
    void testDoFilterBearerTokenCaseInsensitive() throws Exception {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService);
        when(httpRequest.getHeader("Authorization")).thenReturn("bearer abc123");
        when(apiJwtService.validateToken("abc123")).thenReturn(jws);
        when(claims.getIssuer()).thenReturn("TERRACOTTA");
        when(claims.get("oneUse")).thenReturn(false);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain).doFilter(httpRequest, httpResponse);
    }

    @Test
    void testDoFilterNonBearerAuthorizationHeaderNeverInvokesFilterChain() throws Exception {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService);
        when(httpRequest.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testDoFilterInvalidIssuerNeverInvokesFilterChain() throws Exception {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService);
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer abc123");
        when(apiJwtService.validateToken("abc123")).thenReturn(jws);
        when(claims.getIssuer()).thenReturn("SOME_OTHER_ISSUER");

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testDoFilterOneUseTokenExistsProceeds() throws Exception {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService);
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer abc123");
        when(apiJwtService.validateToken("abc123")).thenReturn(jws);
        when(claims.getIssuer()).thenReturn("TERRACOTTA");
        when(claims.get("oneUse")).thenReturn(true);
        when(apiTokenService.findAndDeleteOneUseToken("abc123")).thenReturn(true);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain).doFilter(httpRequest, httpResponse);
    }

    @Test
    void testDoFilterOneUseTokenAlreadyUsedNeverInvokesFilterChain() throws Exception {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService);
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer abc123");
        when(apiJwtService.validateToken("abc123")).thenReturn(jws);
        when(claims.getIssuer()).thenReturn("TERRACOTTA");
        when(claims.get("oneUse")).thenReturn(true);
        when(apiTokenService.findAndDeleteOneUseToken("abc123")).thenReturn(false);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
    }

    // documents current behavior: validateToken(...) can return null for an expired token
    // (ApiJwtServiceImpl.validateToken catches ExpiredJwtException internally and returns null
    // rather than rethrowing) - this filter already null-checks before dereferencing, so the
    // request silently proceeds to the filter chain without issuer/oneUse validation, rather than
    // being rejected with a 401 like the ExpiredJwtException catch block below is designed to do.
    @Test
    void testDoFilterValidateTokenReturnsNullStillProceeds() throws Exception {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService);
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer abc123");
        when(apiJwtService.validateToken("abc123")).thenReturn(null);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain).doFilter(httpRequest, httpResponse);
    }

    @Test
    void testDoFilterExpiredJwtExceptionSetsUnauthorizedStatus() throws Exception {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService);
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer abc123");
        when(claims.getSubject()).thenReturn("user123");
        when(apiJwtService.validateToken("abc123")).thenThrow(new ExpiredJwtException(null, claims, "expired"));

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(httpResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testDoFilterSecurityExceptionSetsUnauthorizedStatus() throws Exception {
        filter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiTokenService);
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer abc123");
        when(apiJwtService.validateToken("abc123")).thenThrow(new SecurityException("bad signature"));

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(httpResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

}
