package edu.iu.terracotta.security.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.lti.JwtAuthenticationToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;

public class JwtAuthenticationProviderTest extends BaseTest {

    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @SuppressWarnings("unchecked")
    private final Jws<Claims> jws = mock(Jws.class);
    private final Claims claims = mock(Claims.class);

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        jwtAuthenticationProvider = new JwtAuthenticationProvider(apiJwtService);

        when(jws.getPayload()).thenReturn(claims);
    }

    @Test
    void testAuthenticateHappyPath() {
        JwtAuthenticationToken incoming = new JwtAuthenticationToken("jwt-value");
        when(apiJwtService.validateToken("jwt-value")).thenReturn(jws);
        when(claims.getSubject()).thenReturn("user123");
        when(claims.get("roles", List.class)).thenReturn(List.of("ADMIN", "USER"));

        JwtAuthenticationToken result = (JwtAuthenticationToken) jwtAuthenticationProvider.authenticate(incoming);

        assertEquals("jwt-value", result.getToken());
        assertEquals("user123", result.getPrincipal());
        assertEquals("jwt-value", result.getCredentials());
        assertEquals(claims, result.getClaims());
        assertTrue(result.isAuthenticated());

        Set<String> authorityNames = result.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
        assertEquals(Set.of("ADMIN", "USER"), authorityNames);
    }

    @Test
    void testAuthenticateNullRolesReturnsEmptyAuthorities() {
        JwtAuthenticationToken incoming = new JwtAuthenticationToken("jwt-value");
        when(apiJwtService.validateToken("jwt-value")).thenReturn(jws);
        when(claims.getSubject()).thenReturn("user123");
        when(claims.get("roles", List.class)).thenReturn(null);

        JwtAuthenticationToken result = (JwtAuthenticationToken) jwtAuthenticationProvider.authenticate(incoming);

        assertTrue(result.getAuthorities().isEmpty());
    }

    @Test
    void testAuthenticateEmptyRolesReturnsEmptyAuthorities() {
        JwtAuthenticationToken incoming = new JwtAuthenticationToken("jwt-value");
        when(apiJwtService.validateToken("jwt-value")).thenReturn(jws);
        when(claims.getSubject()).thenReturn("user123");
        when(claims.get("roles", List.class)).thenReturn(List.of());

        JwtAuthenticationToken result = (JwtAuthenticationToken) jwtAuthenticationProvider.authenticate(incoming);

        assertTrue(result.getAuthorities().isEmpty());
    }

    @Test
    void testAuthenticateThrowsBadCredentialsExceptionWhenValidateTokenThrows() {
        JwtAuthenticationToken incoming = new JwtAuthenticationToken("jwt-value");
        when(apiJwtService.validateToken("jwt-value")).thenThrow(new JwtException("bad signature"));

        BadCredentialsException exception = assertThrows(
            BadCredentialsException.class,
            () -> jwtAuthenticationProvider.authenticate(incoming)
        );

        assertEquals("Failed to authenticate JWT", exception.getMessage());
    }

    // BUG: ApiJwtServiceImpl.validateToken(...) catches ExpiredJwtException internally and returns
    // null instead of rethrowing (confirmed by reading the implementation), so an expired token
    // previously caused an uncaught NullPointerException here instead of a clean
    // BadCredentialsException - fixed by adding an explicit null check.
    @Test
    void testAuthenticateThrowsBadCredentialsExceptionWhenValidateTokenReturnsNull() {
        JwtAuthenticationToken incoming = new JwtAuthenticationToken("jwt-value");
        when(apiJwtService.validateToken("jwt-value")).thenReturn(null);

        assertThrows(BadCredentialsException.class, () -> jwtAuthenticationProvider.authenticate(incoming));
    }

    @Test
    void testSupportsJwtAuthenticationToken() {
        assertTrue(jwtAuthenticationProvider.supports(JwtAuthenticationToken.class));
    }

    @Test
    void testSupportsRejectsOtherAuthenticationTypes() {
        assertFalse(jwtAuthenticationProvider.supports(UsernamePasswordAuthenticationToken.class));
    }

}
