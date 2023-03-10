/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.lti.dto.PlatformRegistrationDTO;
import edu.iu.terracotta.model.lti.dto.ToolConfigurationDTO;
import edu.iu.terracotta.model.lti.dto.ToolMessagesSupportedDTO;
import edu.iu.terracotta.model.lti.dto.ToolRegistrationDTO;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.lti.RegistrationService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This LTI controller should be protected by OAuth 1.0a (on the /oauth path)
 * This will handle LTI 1 and 2 (many of the paths ONLY make sense for LTI2 though)
 * Sample Key "key" and secret "secret"
 */
@Slf4j
@Controller
@Scope("session")
@RequestMapping("/registration")
public class RegistrationController {

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private APIJWTService apijwtService;

    @Autowired
    private RegistrationService registrationService;

    @Value("${application.name}")
    private String clientName;

    @Value("${application.description}")
    private String description;

    /**
     * This will receive the request to start the dynamic registration process and prepare the answer.
     * We receive some parameters (issuer, authorization_endpoint, registration_endpoint,
     * jwks_uri, token_endpoint, token_endpoint_auth_methods_supported,
     * token_endpoint_auth_signing_alg_values_supported,
     * scopes_supported, response_types_supported, subject_types_supported,
     * id_token_signing_alg_values_supported, claims_supported, authorization_server (optional) and
     * https://purl.imsglobal.org/spec/lti-platform-configuration --> product_family_code,version, messages_supported --> (type,placements (optional)), variables(optional))
     * @param req
     * @param model
     * @return
     */
    @GetMapping("/")
    public String registration(@RequestParam("openid_configuration") String openidConfiguration, @RequestParam(LtiStrings.REGISTRATION_TOKEN) String registrationToken, HttpServletRequest req, Model model) {
        // We need to call the configuration endpoint recevied in the registration inititaion message and
        // call it to get all the information about the platform
        HttpSession session = req.getSession();
        model.addAttribute("openid_configuration", openidConfiguration);
        session.setAttribute(LtiStrings.REGISTRATION_TOKEN, registrationToken);
        model.addAttribute(LtiStrings.REGISTRATION_TOKEN, registrationToken);

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        Optional<PlatformDeployment> platformDeployment = allRepositories.platformDeploymentRepository.findById(securedInfo.getPlatformDeploymentId());
        model.addAttribute("own_redirect_post_endpoint", platformDeployment.get().getLocalUrl() + "/registration/");

        try {
            // We are going to create the call the openidconfiguration endpoint,
            RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));

            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.

            ResponseEntity<PlatformRegistrationDTO> platformConfiguration = restTemplate.exchange(openidConfiguration, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), PlatformRegistrationDTO.class);
            PlatformRegistrationDTO platformRegistrationDTO = null;

            if (platformConfiguration != null) {
                HttpStatus status = platformConfiguration.getStatusCode();

                if (status.is2xxSuccessful()) {
                    platformRegistrationDTO = platformConfiguration.getBody();
                } else {
                    String exceptionMsg = "Can't get the platform configuration";
                    log.error(exceptionMsg);
                    throw new ConnectionException(exceptionMsg);
                }
            } else {
                log.warn("Problem getting the membership");
            }

            model.addAttribute(LtiStrings.PLATFORM_CONFIGURATION, platformRegistrationDTO);
            session.setAttribute(LtiStrings.PLATFORM_CONFIGURATION, platformRegistrationDTO);
            ToolRegistrationDTO toolRegistrationDTO = generateToolConfiguration(req);
            // We add that information so the thymeleaf template can display it (and prepare the links)
            model.addAttribute(LtiStrings.TOOL_CONFIGURATION, toolRegistrationDTO);
            session.setAttribute(LtiStrings.TOOL_CONFIGURATION, toolRegistrationDTO);

            // Once all is added to the session, and we have the data ready for the html template, we redirect
            return "registrationRedirect";
        } catch (Exception ex) {
            model.addAttribute("Error", ex.getMessage());
            return "registrationError";
        }
    }

    @PostMapping("/")
    public String registrationPOST(HttpServletRequest req, Model model) {
        HttpSession session = req.getSession();
        String token = (String) session.getAttribute(LtiStrings.REGISTRATION_TOKEN);
        PlatformRegistrationDTO platformRegistrationDTO = (PlatformRegistrationDTO) session.getAttribute(LtiStrings.PLATFORM_CONFIGURATION);
        ToolRegistrationDTO toolRegistrationDTO = (ToolRegistrationDTO) session.getAttribute(LtiStrings.TOOL_CONFIGURATION);
        String answer = "Error during the registration";

        try {
            answer = registrationService.callDynamicRegistration(token, toolRegistrationDTO, platformRegistrationDTO.getRegistration_endpoint());
        } catch (ConnectionException e) {
            e.printStackTrace();
        }

        model.addAttribute("registration_confirmation", answer);

        try {
            model.addAttribute("issuer", java.net.URLDecoder.decode(platformRegistrationDTO.getIssuer(), StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            log.error("Error decoding the issuer as URL", e);
        }

        return "registrationConfirmation";
    }

    /**
     * This generates a JsonNode with all the information that we need to send to the Registration Authorization endpoint in the Platform.
     * In this case, we will put this in the model to be used by the thymeleaf template.
     *
     * @param platformRegistrationDTO
     * @return
     */
    private ToolRegistrationDTO generateToolConfiguration(HttpServletRequest req) {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        Optional<PlatformDeployment> platformDeployment = allRepositories.platformDeploymentRepository.findById(securedInfo.getPlatformDeploymentId());
        ToolRegistrationDTO toolRegistrationDTO = new ToolRegistrationDTO();
        toolRegistrationDTO.setApplication_type("web");
        List<String> grantTypes = new ArrayList<>();
        grantTypes.add("implict");
        grantTypes.add("client_credentials");
        toolRegistrationDTO.setGrant_types(grantTypes);
        toolRegistrationDTO.setResponse_types(Collections.singletonList("id_token"));
        toolRegistrationDTO.setRedirect_uris(Collections.singletonList(platformDeployment.get().getLocalUrl() + TextConstants.LTI3_SUFFIX));
        toolRegistrationDTO.setInitiate_login_uri(platformDeployment.get().getLocalUrl() + "/oidc/login_initiations");
        toolRegistrationDTO.setClient_name(clientName);
        toolRegistrationDTO.setJwks_uri(platformDeployment.get().getLocalUrl() + "/jwks/jwk");
        //OPTIONAL -->setLogo_uri
        toolRegistrationDTO.setToken_endpoint_auth_method("private_key_jwt");
        //OPTIONAL -->setContacts
        //OPTIONAL -->setClient_uri
        //OPTIONAL -->setTos_uri
        //OPTIONAL -->setPolicy_uri
        ToolConfigurationDTO toolConfigurationDTO = new ToolConfigurationDTO();
        toolConfigurationDTO.setDomain(platformDeployment.get().getLocalUrl().substring(platformDeployment.get().getLocalUrl().indexOf("//") + 2));
        //OPTIONAL -->setSecondary_domains --> Collections.singletonList
        //OPTIONAL -->setDeployment_id
        toolConfigurationDTO.setTarget_link_uri(platformDeployment.get().getLocalUrl() + TextConstants.LTI3_SUFFIX);
        //OPTIONAL -->setCustom_parameters --> Map
        toolConfigurationDTO.setDescription(description);
        List<ToolMessagesSupportedDTO> messages = new ArrayList<>();
        ToolMessagesSupportedDTO message1 = new ToolMessagesSupportedDTO();
        message1.setType("LtiDeepLinkingRequest");
        message1.setTarget_link_uri(platformDeployment.get().getLocalUrl() + TextConstants.LTI3_SUFFIX);
        //OPTIONAL: --> message1 --> setLabel
        //OPTIONAL: --> message1 --> setIcon_uri
        //OPTIONAL: --> message1 --> setCustom_parameters
        messages.add(message1);
        ToolMessagesSupportedDTO message2 = new ToolMessagesSupportedDTO();
        message2.setType("LtiResourceLinkRequest");
        message2.setTarget_link_uri(platformDeployment.get().getLocalUrl() + TextConstants.LTI3_SUFFIX);
        messages.add(message2);
        toolConfigurationDTO.setMessages_supported(messages);
        //TODO, fill this correctly based on the claims received.
        List<String> claims = new ArrayList<>();
        claims.add("iss");
        claims.add("aud");
        toolConfigurationDTO.setClaims(claims);
        toolRegistrationDTO.setToolConfiguration(toolConfigurationDTO);
        //TODO, fill this correctly based on the scopes received.
        List<String> scopes = new ArrayList<>();
        scopes.add("openid");
        scopes.add("https://purl.imsglobal.org/spec/lti-ags/scope/lineitem");
        scopes.add("https://purl.imsglobal.org/spec/lti-ags/scope/result.readonly");
        scopes.add("https://purl.imsglobal.org/spec/lti-ags/scope/score");
        scopes.add("https://purl.imsglobal.org/spec/lti-reg/scope/registration");
        scopes.add("https://purl.imsglobal.org/spec/lti-nrps/scope/contextmembership.readonly");
        toolRegistrationDTO.setScope(scopes);

        return toolRegistrationDTO;
    }

}
