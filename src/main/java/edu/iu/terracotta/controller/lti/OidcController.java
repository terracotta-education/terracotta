package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.LoginInitiationDto;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.dao.entity.LtiNonce;
import edu.iu.terracotta.dao.repository.LtiNonceRepository;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.lti.LtiOidcUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This LTI controller should be protected by OAuth 1.0a (on the /oauth path)
 * This will handle LTI 1 and 2 (many of the paths ONLY make sense for LTI2 though)
 * Sample Key "key" and secret "secret"
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/oidc/login_initiations")
@SuppressWarnings({"PMD.LooseCoupling"})
public class OidcController {

    //Constants defined in the LTI standard
    private static final String NONE = "none";
    private static final String FORM_POST = "form_post";
    private static final String ID_TOKEN = "id_token";
    private static final String OPEN_ID = "openid";
    private static final String CLIENT_ID = "client_id";
    private static final String DEPLOYMENT_ID = "lti_deployment_id";

    private final PlatformDeploymentRepository platformDeploymentRepository;
    private final LtiDataService ltiDataService;
    private final LtiNonceRepository ltiNonceRepository;

    @Value("${app.lti.data.verbose.logging.enabled:false}")
    private boolean ltiDataVerboseLoggingEnabled;

    /**
     * This will receive the request to start the OIDC process.
     * We receive some parameters (iss, login_hint, target_link_uri, lti_message_hint, and optionally, the deployment_id and the client_id)
     */
    @PostMapping
    public String loginInitiations(HttpServletRequest req, Model model) {
        // We need to receive the parameters and search for the deployment of the tool that matches with what we receive.
        LoginInitiationDto loginInitiationDTO = new LoginInitiationDto(req);
        List<PlatformDeployment> platformDeploymentListEntityList;
        // Getting the client_id (that is optional) and can come in the form or in the URL.
        String clientIdValue;

        // If we already have it in the loginInitiationDTO
        if (loginInitiationDTO.getClientId() != null) {
            clientIdValue = loginInitiationDTO.getClientId();
        } else {  // We try to get it from the URL query parameters.
            clientIdValue = req.getParameter(CLIENT_ID);
        }

        // Getting the deployment_id (that is optional) and can come in the form or in the URL.
        String deploymentIdValue;

        // If we already have it in the loginInitiationDTO
        if (loginInitiationDTO.getDeploymentId() != null) {
            deploymentIdValue = loginInitiationDTO.getDeploymentId();
        } else {  // We try to get it from the URL query getDeploymentId.
            deploymentIdValue = req.getParameter(DEPLOYMENT_ID);
        }

        // We search for the platformDeployment.
        // We will try all the options here (from more detailed to less), and we will deal with the error if there are more than one result.
        if (clientIdValue != null && deploymentIdValue != null) {
            // search for platformDeployment by iss, clientId and deploymentIdValue
            platformDeploymentListEntityList = platformDeploymentRepository.findByIssAndClientIdAndToolDeployments_LtiDeploymentId(loginInitiationDTO.getIss(), clientIdValue, deploymentIdValue);
            if (platformDeploymentListEntityList.isEmpty()) {
                // if missing, check if we can automatically create a ToolDeployment
                ToolDeployment toolDeployment = this.ltiDataService.findOrCreateToolDeployment(loginInitiationDTO.getIss(), clientIdValue, deploymentIdValue);
                if (toolDeployment != null) {
                    platformDeploymentListEntityList = Collections.singletonList(toolDeployment.getPlatformDeployment());
                }
            }
        } else if (clientIdValue != null) {
            platformDeploymentListEntityList = platformDeploymentRepository.findByIssAndClientId(loginInitiationDTO.getIss(), clientIdValue);
        } else if (deploymentIdValue != null) {
            platformDeploymentListEntityList = platformDeploymentRepository.findByIssAndToolDeployments_LtiDeploymentId(loginInitiationDTO.getIss(), deploymentIdValue);
        } else {
            platformDeploymentListEntityList = platformDeploymentRepository.findByIss(loginInitiationDTO.getIss());
        }

        // We deal with some possible errors
        if (platformDeploymentListEntityList.isEmpty()) {  //If we don't have configuration
            model.addAttribute(TextConstants.ERROR, "Not found any existing tool deployment with iss: " + loginInitiationDTO.getIss() +
                    " clientId: " + clientIdValue + " deploymentId: " + deploymentIdValue);

            return TextConstants.LTI3ERROR;
        }

        if (platformDeploymentListEntityList.size() > 1) {   // If we have more than one match.
            model.addAttribute(TextConstants.ERROR, "We have more than one tool deployment with iss: " + loginInitiationDTO.getIss() +
                    " clientId: " + clientIdValue + " deploymentId: " + deploymentIdValue);

            return TextConstants.LTI3ERROR;
        }

        // If we have arrived here, it means that we have only one result (as expected)
        PlatformDeployment lti3KeyEntity = platformDeploymentListEntityList.get(0);

        if (clientIdValue == null) {
            clientIdValue = lti3KeyEntity.getClientId();
        }

        try {
            // We are going to create the OIDC request,
            Map<String, String> parameters = generateAuthRequestPayload(lti3KeyEntity, loginInitiationDTO, clientIdValue, deploymentIdValue);

            // We add that information so the thymeleaf template can display it (and prepare the links)
            //model.addAllAttributes(parameters);
            // These 3 are to display what we received from the platform.
            if (ltiDataService.getDemoMode()) {
                model.addAllAttributes(parameters);
                model.addAttribute("initiation_dto", loginInitiationDTO);
                model.addAttribute("client_id_received", clientIdValue);
                model.addAttribute("deployment_id_received", deploymentIdValue);
            }

            // The state is a self-contained signed JWT (see LtiOidcUtils.generateState), so its own
            // signature is enough to validate it later - no need to track issued states ourselves.
            // The nonce isn't self-verifying, so it's persisted here and consumed exactly once
            // (Lti3Request.checkNonce) instead of being tracked in a session.
            ltiNonceRepository.save(LtiNonce.builder().nonce(parameters.get("nonce")).build());

            // Once we have the data ready, we redirect straight to the platform's OIDC
            // authorization endpoint - the launch doesn't depend on cookies, so there's no need
            // to route through a storage-access check first.
            if (!ltiDataService.getDemoMode()) {
                return "redirect:" + parameters.get("oicdEndpointComplete");
            }

            return "oidcRedirect";
        } catch (Exception ex) {
            log.error("Failed creating OIDC request", ex);
            model.addAttribute(TextConstants.ERROR, ex.getMessage());

            return TextConstants.LTI3ERROR;
        }
    }

    /**
     * This generates a map with all the information that we need to send to the OIDC Authorization endpoint in the Platform.
     * In this case, we will put this in the model to be used by the thymeleaf template.
     */
    private Map<String, String> generateAuthRequestPayload(PlatformDeployment platformDeployment, LoginInitiationDto loginInitiationDto, String clientIdValue, String deploymentIdValue) throws GeneralSecurityException, IOException {
        Map<String, String> authRequestMap = new HashMap<>();
        authRequestMap.put(CLIENT_ID, platformDeployment.getClientId()); //As it came from the Platform (if it came... if not we should have it configured)
        authRequestMap.put("login_hint", loginInitiationDto.getLoginHint()); //As it came from the Platform
        authRequestMap.put("lti_message_hint", loginInitiationDto.getLtiMessageHint()); //As it came from the Platform
        String nonce = UUID.randomUUID().toString(); // We generate a nonce to allow this auth request to be used only one time.
        authRequestMap.put("nonce", nonce);  //The nonce
        authRequestMap.put("prompt", NONE);  //Always this value, as specified in the standard.
        authRequestMap.put("redirect_uri", String.format("%s/lti3", platformDeployment.getLocalUrl()));
        authRequestMap.put("response_mode", FORM_POST); //Always this value, as specified in the standard.
        authRequestMap.put("response_type", ID_TOKEN); //Always this value, as specified in the standard.
        authRequestMap.put("scope", OPEN_ID);  //Always this value, as specified in the standard.
        // The state is something that we can create and add anything we want on it.
        // On this case, we have decided to create a JWT token with some information that we will use as additional security. But it is not mandatory.
        String state = LtiOidcUtils.generateState(ltiDataService, platformDeployment, authRequestMap, loginInitiationDto, clientIdValue, deploymentIdValue, ltiDataVerboseLoggingEnabled);
        authRequestMap.put("state", state); //The state we use later to retrieve some useful information about the OICD request.
        authRequestMap.put("oicdEndpoint", platformDeployment.getOidcEndpoint());  //We need this in the Thymeleaf template in case we decide to use the POST method. It is the endpoint where the LMS receives the OICD requests
        authRequestMap.put("oicdEndpointComplete", generateCompleteUrl(authRequestMap));  //This generates the URL to use in case we decide to use the GET method

        return authRequestMap;
    }

    /**
     * This generates the GET URL with all the query string parameters.
     */
    private String generateCompleteUrl(Map<String, String> model) throws UnsupportedEncodingException {
        StringBuilder getUrl = new StringBuilder();

        getUrl.append(model.get("oicdEndpoint"));
        getUrl = addParameter(getUrl, "client_id", model.get(CLIENT_ID), true);
        getUrl = addParameter(getUrl, "login_hint", model.get("login_hint"), false);
        getUrl = addParameter(getUrl, "lti_message_hint", model.get("lti_message_hint"), false);
        getUrl = addParameter(getUrl, "nonce", model.get("nonce"), false);
        getUrl = addParameter(getUrl, "prompt", model.get("prompt"), false);
        getUrl = addParameter(getUrl, "redirect_uri", model.get("redirect_uri"), false);
        getUrl = addParameter(getUrl, "response_mode", model.get("response_mode"), false);
        getUrl = addParameter(getUrl, "response_type", model.get("response_type"), false);
        getUrl = addParameter(getUrl, "scope", model.get("scope"), false);
        getUrl = addParameter(getUrl, "state", model.get("state"), false);

        return getUrl.toString();
    }

    private StringBuilder addParameter(StringBuilder url, String parameter, String value, boolean first) throws UnsupportedEncodingException {
        if (value != null) {
            if (first) {
                url.append("?").append(parameter).append("=");
            } else {
                url.append("&").append(parameter).append("=");
            }

            url.append(URLEncoder.encode(value, String.valueOf(StandardCharsets.UTF_8)));
        }

        return url;
    }

}
