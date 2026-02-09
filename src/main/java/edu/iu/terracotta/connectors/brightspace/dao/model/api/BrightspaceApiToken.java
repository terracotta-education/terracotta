package edu.iu.terracotta.connectors.brightspace.dao.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.iu.terracotta.connectors.generic.dao.model.lti.ApiToken;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrightspaceApiToken implements ApiToken {

    private BrightspaceApiUser user;

    @JsonProperty("access_token") private String accessToken;
    @JsonProperty("refresh_token") private String refreshToken;
    @JsonProperty("expires_in") private Integer expiresIn;

}
