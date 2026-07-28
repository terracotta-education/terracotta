package edu.iu.terracotta.security.lti;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Lti3OAuthProviderProcessingFilterTest extends BaseTest {

    @Mock private HttpServletRequest httpRequest;
    @Mock private HttpServletResponse httpResponse;
    @Mock private FilterChain filterChain;

    @SuppressWarnings("unchecked")
    private final Jws<Claims> stateJws = mock(Jws.class);
    private final Claims stateClaims = mock(Claims.class);

    private Lti3OAuthProviderProcessingFilter filter;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(stateJws.getPayload()).thenReturn(stateClaims);
        when(stateClaims.getAudience()).thenReturn(java.util.Set.of("client-id"));

        filter = new Lti3OAuthProviderProcessingFilter(ltiDataService, ltiJwtService);
    }

    @Test
    void testConstructorNullLtiDataServiceThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new Lti3OAuthProviderProcessingFilter(null, ltiJwtService));
    }

    @Test
    void testConstructorNullLtiJwtServiceThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new Lti3OAuthProviderProcessingFilter(ltiDataService, null));
    }

    @Test
    void testDoFilterNonHttpServletRequestThrowsIllegalStateException() {
        ServletRequest nonHttpRequest = mock(ServletRequest.class);

        assertThrows(IllegalStateException.class, () -> filter.doFilter(nonHttpRequest, httpResponse, filterChain));
    }

    @Test
    void testDoFilterMissingStateParameterReturnsEarly() throws Exception {
        when(httpRequest.getParameter("state")).thenReturn(null);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testDoFilterBlankStateParameterReturnsEarly() throws Exception {
        when(httpRequest.getParameter("state")).thenReturn("   ");

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testDoFilterBlankIdTokenStillProceedsAndResetsAuthentication() throws Exception {
        when(httpRequest.getParameter("state")).thenReturn("state123");
        when(ltiJwtService.validateState("state123")).thenReturn(stateJws);
        when(httpRequest.getParameter("id_token")).thenReturn(null);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain).doFilter(httpRequest, httpResponse);
    }

    @Test
    void testDoFilterValidateJWTReturnsNullSkipsLti3RequestButStillProceeds() throws Exception {
        when(httpRequest.getParameter("state")).thenReturn("state123");
        when(ltiJwtService.validateState("state123")).thenReturn(stateJws);
        when(httpRequest.getParameter("id_token")).thenReturn("id-token-value");
        when(ltiJwtService.validateJWT("id-token-value", "client-id")).thenReturn(null);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain).doFilter(httpRequest, httpResponse);
        verify(httpRequest, never()).setAttribute(org.mockito.ArgumentMatchers.eq("LTI3"), any());
    }

    @Test
    void testDoFilterNeverAddsAnyCookie() throws Exception {
        when(httpRequest.getParameter("state")).thenReturn("state123");
        when(ltiJwtService.validateState("state123")).thenReturn(stateJws);
        when(httpRequest.getParameter("id_token")).thenReturn(null);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(httpResponse, never()).addCookie(any(Cookie.class));
    }

    @Test
    void testDoFilterExpiredJwtExceptionSetsUnauthorizedStatus() throws Exception {
        when(httpRequest.getParameter("state")).thenReturn("state123");
        when(stateClaims.getSubject()).thenReturn("user123");
        when(ltiJwtService.validateState("state123")).thenThrow(new ExpiredJwtException(null, stateClaims, "expired"));

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(httpResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testDoFilterSecurityExceptionSetsUnauthorizedStatus() throws Exception {
        when(httpRequest.getParameter("state")).thenReturn("state123");
        when(ltiJwtService.validateState("state123")).thenReturn(stateJws);
        when(httpRequest.getParameter("id_token")).thenReturn("id-token-value");
        when(ltiJwtService.validateJWT("id-token-value", "client-id")).thenThrow(new SecurityException("bad signature"));

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(httpResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

}
