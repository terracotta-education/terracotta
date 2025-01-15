package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.PlatformRegistrationDto;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.ToolConfigurationDto;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.ToolMessagesSupportedDto;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.ToolRegistrationDto;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.lti.RegistrationService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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

    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private ApiJwtService apijwtService;
    @Autowired private RegistrationService registrationService;

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
     * @throws TerracottaConnectorException
     * @throws NumberFormatException
     */
    @GetMapping("/")
    public String registration(@RequestParam("openid_configuration") String openidConfiguration, @RequestParam(LtiStrings.REGISTRATION_TOKEN) String registrationToken, HttpServletRequest req, Model model) throws NumberFormatException, TerracottaConnectorException {
        // We need to call the configuration endpoint recevied in the registration inititaion message and
        // call it to get all the information about the platform
        HttpSession session = req.getSession();
        model.addAttribute("openid_configuration", openidConfiguration);
        session.setAttribute(LtiStrings.REGISTRATION_TOKEN, registrationToken);
        model.addAttribute(LtiStrings.REGISTRATION_TOKEN, registrationToken);

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(securedInfo.getPlatformDeploymentId());
        model.addAttribute("own_redirect_post_endpoint", platformDeployment.get().getLocalUrl() + "/registration/");

        try {
            // We are going to create the call the openidconfiguration endpoint,
            RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));

            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.

            ResponseEntity<PlatformRegistrationDto> platformConfiguration = restTemplate.exchange(openidConfiguration, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), PlatformRegistrationDto.class);
            PlatformRegistrationDto platformRegistrationDto = null;

            if (platformConfiguration != null) {
                HttpStatusCode status = platformConfiguration.getStatusCode();

                if (status.is2xxSuccessful()) {
                    platformRegistrationDto = platformConfiguration.getBody();
                } else {
                    String exceptionMsg = "Can't get the platform configuration";
                    log.error(exceptionMsg);
                    throw new ConnectionException(exceptionMsg);
                }
            } else {
                log.warn("Problem getting the membership");
            }

            model.addAttribute(LtiStrings.PLATFORM_CONFIGURATION, platformRegistrationDto);
            session.setAttribute(LtiStrings.PLATFORM_CONFIGURATION, platformRegistrationDto);
            ToolRegistrationDto toolRegistrationDto = generateToolConfiguration(req);
            // We add that information so the thymeleaf template can display it (and prepare the links)
            model.addAttribute(LtiStrings.TOOL_CONFIGURATION, toolRegistrationDto);
            session.setAttribute(LtiStrings.TOOL_CONFIGURATION, toolRegistrationDto);

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
        PlatformRegistrationDto platformRegistrationDto = (PlatformRegistrationDto) session.getAttribute(LtiStrings.PLATFORM_CONFIGURATION);
        ToolRegistrationDto toolRegistrationDto = (ToolRegistrationDto) session.getAttribute(LtiStrings.TOOL_CONFIGURATION);
        String answer = "Error during the registration";

        try {
            answer = registrationService.callDynamicRegistration(token, toolRegistrationDto, platformRegistrationDto.getRegistration_endpoint());
        } catch (ConnectionException e) {
            e.printStackTrace();
        }

        model.addAttribute("registration_confirmation", answer);

        try {
            model.addAttribute("issuer", java.net.URLDecoder.decode(platformRegistrationDto.getIssuer(), StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            log.error("Error decoding the issuer as URL", e);
        }

        return "registrationConfirmation";
    }

    /**
     * This generates a JsonNode with all the information that we need to send to the Registration Authorization endpoint in the Platform.
     * In this case, we will put this in the model to be used by the thymeleaf template.
     *
     * @param platformRegistrationDto
     * @return
          * @throws TerracottaConnectorException
          * @throws NumberFormatException
          */
         private ToolRegistrationDto generateToolConfiguration(HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(securedInfo.getPlatformDeploymentId());
        ToolRegistrationDto toolRegistrationDto = new ToolRegistrationDto();
        toolRegistrationDto.setApplication_type("web");
        List<String> grantTypes = new ArrayList<>();
        grantTypes.add("implict");
        grantTypes.add("client_credentials");
        toolRegistrationDto.setGrant_types(grantTypes);
        toolRegistrationDto.setResponse_types(Collections.singletonList("id_token"));
        toolRegistrationDto.setRedirect_uris(Collections.singletonList(platformDeployment.get().getLocalUrl() + TextConstants.LTI3_SUFFIX));
        toolRegistrationDto.setInitiate_login_uri(platformDeployment.get().getLocalUrl() + "/oidc/login_initiations");
        toolRegistrationDto.setClient_name(clientName);
        toolRegistrationDto.setJwks_uri(platformDeployment.get().getLocalUrl() + "/jwks/jwk");
        //OPTIONAL -->setLogo_uri
        toolRegistrationDto.setToken_endpoint_auth_method("private_key_jwt");
        //OPTIONAL -->setContacts
        //OPTIONAL -->setClient_uri
        //OPTIONAL -->setTos_uri
        //OPTIONAL -->setPolicy_uri
        ToolConfigurationDto toolConfigurationDto = new ToolConfigurationDto();
        toolConfigurationDto.setDomain(platformDeployment.get().getLocalUrl().substring(platformDeployment.get().getLocalUrl().indexOf("//") + 2));
        //OPTIONAL -->setSecondary_domains --> Collections.singletonList
        //OPTIONAL -->setDeployment_id
        toolConfigurationDto.setTarget_link_uri(platformDeployment.get().getLocalUrl() + TextConstants.LTI3_SUFFIX);
        //OPTIONAL -->setCustom_parameters --> Map
        toolConfigurationDto.setDescription(description);
        List<ToolMessagesSupportedDto> messages = new ArrayList<>();
        ToolMessagesSupportedDto message1 = new ToolMessagesSupportedDto();
        message1.setType("LtiDeepLinkingRequest");
        message1.setTarget_link_uri(platformDeployment.get().getLocalUrl() + TextConstants.LTI3_SUFFIX);
        //OPTIONAL: --> message1 --> setLabel
        //OPTIONAL: --> message1 --> setIcon_uri
        //OPTIONAL: --> message1 --> setCustom_parameters
        messages.add(message1);
        ToolMessagesSupportedDto message2 = new ToolMessagesSupportedDto();
        message2.setType("LtiResourceLinkRequest");
        message2.setTarget_link_uri(platformDeployment.get().getLocalUrl() + TextConstants.LTI3_SUFFIX);
        messages.add(message2);
        toolConfigurationDto.setMessages_supported(messages);
        //TODO, fill this correctly based on the claims received.
        List<String> claims = new ArrayList<>();
        claims.add("iss");
        claims.add("aud");
        toolConfigurationDto.setClaims(claims);
        toolRegistrationDto.setToolConfiguration(toolConfigurationDto);
        //TODO, fill this correctly based on the scopes received.
        List<String> scopes = new ArrayList<>();
        scopes.add("openid");
        scopes.add("https://purl.imsglobal.org/spec/lti-ags/scope/lineitem");
        scopes.add("https://purl.imsglobal.org/spec/lti-ags/scope/result.readonly");
        scopes.add("https://purl.imsglobal.org/spec/lti-ags/scope/score");
        scopes.add("https://purl.imsglobal.org/spec/lti-reg/scope/registration");
        scopes.add("https://purl.imsglobal.org/spec/lti-nrps/scope/contextmembership.readonly");
        toolRegistrationDto.setScope(scopes);

        return toolRegistrationDto;
    }

}
