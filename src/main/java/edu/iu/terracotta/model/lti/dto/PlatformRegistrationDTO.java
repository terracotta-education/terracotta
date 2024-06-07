package edu.iu.terracotta.model.lti.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlatformRegistrationDTO {

    private String issuer;
    private String authorization_endpoint;
    private String token_endpoint;
    private List<String> token_endpoint_auth_methods_supported;
    private String jwks_uri;
    private String registration_endpoint;
    private List<String> scopes_supported;
    private List<String> response_types_supported;
    private List<String> subject_types_supported;
    private List<String> id_token_signing_alg_values_supported;
    private List<String> claims_supported;
    private String authorization_server;

    @JsonProperty("https://purl.imsglobal.org/spec/lti-platform-configuration")
    private PlatformConfigurationDTO platformConfiguration;

}
