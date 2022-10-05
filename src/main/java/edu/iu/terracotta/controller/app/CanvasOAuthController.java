package edu.iu.terracotta.controller.app;

import java.text.MessageFormat;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import edu.iu.terracotta.model.canvas.CanvasAPIToken;
import edu.iu.terracotta.service.lti.LTIDataService;

@Controller
@RequestMapping("/canvas/oauth2")
public class CanvasOAuthController {

    private static final String SESSION_CANVAS_OAUTH2_STATE = "canvas_oauth2_state";

    private static final Logger log = LoggerFactory.getLogger(CanvasOAuthController.class);

    @Autowired
    LTIDataService ltiDataService;

    @GetMapping("/auth")
    public String redirectToCanvasOAuthLogin(HttpServletRequest req, Model model) {

        // TODO: maybe need a redirect_uri that we redirect to after obtaining token?
        String clientId = "202570000000000113";
        model.addAttribute("client_id", clientId);
        model.addAttribute("response_type", "code");
        model.addAttribute("redirect_uri", getRedirectURI());
        // TODO: Is there a better way to generate the state value?
        UUID randomUUID = UUID.randomUUID();
        HttpSession session = req.getSession();
        session.setAttribute(SESSION_CANVAS_OAUTH2_STATE, randomUUID.toString());
        model.addAttribute("state", randomUUID.toString());
        // TODO: specify all of the necessary scopes
        // TODO: get auth endpoint from database config
        return "redirect:https://terracotta.instructure.com/login/oauth2/auth";
    }

    private String getRedirectURI() {
        // TODO: use MvcUriComponentsBuilder
        // https://docs.spring.io/spring-framework/docs/5.2.22.RELEASE/spring-framework-reference/web.html#mvc-links-to-controllers
        return ltiDataService.getLocalUrl() + "/canvas/oauth2/oauth_response";
    }

    @GetMapping(value = "/oauth_response")
    public String handleOauthResponse(HttpServletRequest req) {

        String code = req.getParameter("code");
        log.debug("/oauth_response: code={}", code);

        // Verify that state parameter matches session state
        String state = req.getParameter("state");
        log.debug("/oauth_response: state={}", state);
        String sessionState = (String) req.getSession().getAttribute(SESSION_CANVAS_OAUTH2_STATE);
        if (sessionState == null) {
            throw new IllegalStateException("Canvas OAuth2 request doesn't contains the expected state");
        }
        if (!sessionState.equals(state)) {
            throw new IllegalStateException("Canvas OAuth2 request doesn't contains the expected state");
        }

        CanvasAPIToken canvasAPIToken = getToken(code);
        return canvasAPIToken.getAccessToken();
    }

    private CanvasAPIToken getToken(String code) {

        // TODO: retrieve these from database
        String clientId = "202570000000000113";
        String clientSecret = "orqsVzvJzC8voMswY3nMtpA1yt6pNYJFoGUBJO69gnTLGD1TN9ldB5o4Eb5Bvjhh";
        String canvasOauth2TokenUrl = "https://terracotta.instructure.com/login/oauth2/token";

        // Create x-www-form-urlencoded POST request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "authorization_code");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("redirect_uri", getRedirectURI());
        map.add("code", code);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        RestTemplate restTemplate = createRestTemplate();
        ResponseEntity<CanvasAPIToken> response = restTemplate.postForEntity(canvasOauth2TokenUrl, request,
                CanvasAPIToken.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        throw new RuntimeException(MessageFormat.format("Could not retrieve token from {0}", canvasOauth2TokenUrl));
    }

    private RestTemplate createRestTemplate() {
        return new RestTemplate(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    }
}
