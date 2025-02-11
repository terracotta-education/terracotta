package edu.iu.terracotta.security.app;

import com.google.common.collect.ImmutableSet;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import edu.iu.terracotta.connectors.generic.dao.model.lti.JwtAuthenticationToken;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    @Autowired private ApiJwtService jwtService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        JwtAuthenticationToken jwtAuthentication = (JwtAuthenticationToken) authentication;
        String jwtValue = jwtAuthentication.getToken();
        try {
            Jws<Claims> jwtClaims = jwtService.validateToken(jwtValue);
            return new JwtAuthenticationToken(
                    jwtValue, jwtClaims.getPayload().getSubject(), extractGrantedAuthorities(jwtClaims.getPayload()), jwtClaims.getPayload());
        } catch (JwtException e) {
            throw new BadCredentialsException("Failed to authenticate JWT", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractGrantedAuthorities(Claims jwtClaims) {
        List<String> authorityStrings = jwtClaims.get("roles", List.class);
        if (CollectionUtils.isEmpty(authorityStrings)) {
            return ImmutableSet.of();
        }
        return authorityStrings.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
