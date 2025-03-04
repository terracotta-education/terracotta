package edu.iu.terracotta.security.app;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.api.ApiTokenService;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * LTI3 Redirect calls will be filtered on this class. We will check if the JWT is valid and then extract all the needed data.
 */
@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ApiOAuthProviderProcessingFilter extends GenericFilterBean {

    private static final String JWT_REQUEST_HEADER_NAME = "Authorization";
    private static final String JWT_BEARER_TYPE = "Bearer";
    private static final String QUERY_PARAM_NAME = "token";

    private final boolean allowQueryParam;

    private ApiJwtService apiJwtService;
    private ApiTokenService apiDataService;

    public ApiOAuthProviderProcessingFilter(ApiJwtService apiJwtService, ApiTokenService apiDataService) {
        this(apiJwtService, apiDataService, false);
    }

    /**
     * We need to load the data service to find the iss configurations and extract the keys.
     */
    public ApiOAuthProviderProcessingFilter(ApiJwtService apiJwtService, ApiTokenService apiDataService, boolean allowQueryParam) {
        super();
        this.allowQueryParam = allowQueryParam;

        if (apiJwtService == null) {
            throw new AssertionError();
        }

        this.apiJwtService = apiJwtService;

        if (apiDataService == null) {
            throw new AssertionError();
        }

        this.apiDataService = apiDataService;
    }

    /**
     * We filter all the API queries received on this endpoint.
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest)) {
            throw new IllegalStateException("API requests MUST be an HttpServletRequest (cannot only be a ServletRequest)");
        }

        try {
            String token = extractJwtStringValue((HttpServletRequest) servletRequest);

            if (token == null) {
                throw new AuthenticationCredentialsNotFoundException("Missing JWT token");
            }

            // Second, as the state is something that we have created, it should be in our list of states.

            if (StringUtils.hasText(token)) {
                Jws<Claims> tokenClaims = apiJwtService.validateToken(token);
                if (tokenClaims != null) {
                    if (!"TERRACOTTA".equals(tokenClaims.getPayload().getIssuer())) {
                        throw new IllegalStateException("API token is invalid");
                    }

                    // TODO add here any other checks we want to perform.

                    if ((Boolean) tokenClaims.getPayload().get("oneUse")) {
                        boolean exists = apiDataService.findAndDeleteOneUseToken(token);

                        if (!exists) {
                            throw new IllegalStateException("OneUse token does not exist or has been already used");
                        }
                    }
                }
            }

            filterChain.doFilter(servletRequest, servletResponse);
            this.resetAuthenticationAfterRequest();
        } catch (ExpiredJwtException e) {
            log.warn("Security exception for user {} - {}", e.getClaims().getSubject(), e.getMessage());
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (AuthenticationCredentialsNotFoundException | IllegalStateException e) {
            log.warn("Error handling JWT token: {}", e.getMessage());
        }
    }

    private void resetAuthenticationAfterRequest() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private String extractJwtStringValue(HttpServletRequest request) {
        String rawHeaderValue = StringUtils.trimAllWhitespace(request.getHeader(JWT_REQUEST_HEADER_NAME));

        if (rawHeaderValue == null && allowQueryParam) {
            return StringUtils.trimAllWhitespace(request.getParameter(QUERY_PARAM_NAME));
        }

        if (rawHeaderValue == null) {
          return null;
        }

        // very similar to BearerTokenExtractor.java in Spring spring-security-oauth2

        if (isBearerToken(rawHeaderValue)) {
            return rawHeaderValue.substring(JWT_BEARER_TYPE.length()).trim();
        }

        return null;
    }

    private boolean isBearerToken(String rawHeaderValue) {
        return rawHeaderValue.toLowerCase(Locale.US).startsWith(JWT_BEARER_TYPE.toLowerCase(Locale.US));
    }

}
