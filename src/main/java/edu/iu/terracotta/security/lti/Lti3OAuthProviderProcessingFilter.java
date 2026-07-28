package edu.iu.terracotta.security.lti;

import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiJwtService;
import edu.iu.terracotta.exceptions.DataServiceException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import edu.iu.terracotta.utils.lti.Lti3Request;

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

/**
 * LTI3 Redirect calls will be filtered on this class. Check if the JWT is valid and then extract all the needed data.
 */
@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class Lti3OAuthProviderProcessingFilter extends GenericFilterBean {

    private LtiDataService ltiDataService;
    private LtiJwtService ltijwtService;

    /**
     * Load the data service to find the iss configurations and extract the keys.
     */
    public Lti3OAuthProviderProcessingFilter(LtiDataService ltiDataService, LtiJwtService ltijwtService) {
        super();

        if (ltiDataService == null) {
            throw new AssertionError();
        }

        this.ltiDataService = ltiDataService;

        if (ltijwtService == null) {
            throw new AssertionError();
        }

        this.ltijwtService = ltijwtService;
    }

    /**
     * Filter all the LTI3 queries received on this endpoint.
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException,
            ServletException {

        // We filter all the LTI queries (not the launch) with this filter.
        if (!(servletRequest instanceof HttpServletRequest)) {
            throw new IllegalStateException("LTI request MUST be an HttpServletRequest (cannot only be a ServletRequest)");
        }

        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

            // validate that the state is a good state.

            // ensure that the query has an state
            String state = httpServletRequest.getParameter("state");
            String link = httpServletRequest.getParameter("link");

            if (!StringUtils.hasText(state)) {
                log.error("LTI request doesn't contain the expected state");
                return;
            }

            // validate the state to be sure that is correct; its signature is the sole source of
            // truth for whether it is one we actually issued - no session-based tracking needed
            Jws<Claims> stateClaims = ltijwtService.validateState(state);

            /*
                Once we have the state validated we need the key to check the JWT signature from the id_token, and extract all the values in the Lti3Request object.
                Most of the platforms will provide a JWK repo URL and we will have it stored in configuration, where they store the public keys
                With that URL and the "kid" in the header of the jwt id_token, we can get the public key too.
                In our tool we have included a alternative mechanism for those platforms without JWK endpoint.
                The state provides us the way to find that key in our repo. This is not a requirement in LTI, it is just a way to do it that we've implemented, but each one can use the
                state in a different way.
            */

            String jwt = httpServletRequest.getParameter("id_token");

            if (StringUtils.hasText(jwt)) {
                // validate the JWT token
                Jws<Claims> jws = ltijwtService.validateJWT(jwt, stateClaims.getPayload().getAudience().toArray(new String[stateClaims.getPayload().getAudience().size()])[0]);
                if (jws != null) {
                    // Create and populate the Lti3Request object and add it to the httpServletRequest, so the redirect endpoint will have all that information ready and will be able to use it.
                    Lti3Request lti3Request = new Lti3Request(httpServletRequest, ltiDataService, true, link); // IllegalStateException if invalid
                    httpServletRequest.setAttribute("LTI3", true); // indicate this request is an LTI3 one
                    httpServletRequest.setAttribute("lti3_valid", lti3Request.isLoaded() && lti3Request.isComplete()); // is LTI3 request totally valid and complete
                    httpServletRequest.setAttribute("lti3_message_type", lti3Request.getLtiMessageType()); // is LTI3 request totally valid and complete
                    httpServletRequest.setAttribute(Lti3Request.class.getName(), lti3Request); // make the LTI3 data accessible later in the request if needed
                }
            }

            filterChain.doFilter(servletRequest, servletResponse);

            this.resetAuthenticationAfterRequest();
        } catch (ExpiredJwtException eje) {
            log.warn("Security exception for user {} - {}", eje.getClaims().getSubject(), eje.getMessage());
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.debug("Exception " + eje.getMessage(), eje);
        } catch (SecurityException ex) {
            log.warn("Invalid JWT signature: {0}", ex.getMessage());
            log.debug("Exception " + ex.getMessage(), ex);
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (DataServiceException e) {
            log.error("Error in the Data Service", e);
        }
    }

    private void resetAuthenticationAfterRequest() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

}
