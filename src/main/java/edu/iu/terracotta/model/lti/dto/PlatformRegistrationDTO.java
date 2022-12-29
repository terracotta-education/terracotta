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
package edu.iu.terracotta.model.lti.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

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
