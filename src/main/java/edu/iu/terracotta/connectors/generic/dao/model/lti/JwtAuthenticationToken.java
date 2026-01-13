package edu.iu.terracotta.connectors.generic.dao.model.lti;

import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

@Getter
@Setter
@SuppressWarnings({"PMD.LooseCoupling"})
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private String token;
    private String principal;
    private Claims claims;

    public JwtAuthenticationToken(String token) {
        super(List.of());
        this.token = token;
        setAuthenticated(false);
    }

    public JwtAuthenticationToken(String token, String principal, Collection<? extends GrantedAuthority> authorities, Claims claims) {
        super(authorities);
        this.token = token;
        this.principal = principal;
        this.claims = claims;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.token;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

}
