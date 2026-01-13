package edu.iu.terracotta.connectors.generic.dao.model.lti.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolRegistrationDto {

    private String application_type;
    private List<String> grant_types;
    private List<String> response_types;
    private List<String> redirect_uris;
    private String initiate_login_uri;
    private String client_name;
    private String jwks_uri;
    private String logo_uri;
    private String token_endpoint_auth_method;
    private List<String> contacts;
    private String client_uri;
    private String tos_uri;
    private String policy_uri;
    private List<String> scope;

    @JsonProperty("https://purl.imsglobal.org/spec/lti-tool-configuration")
    private ToolConfigurationDto toolConfiguration;

}
