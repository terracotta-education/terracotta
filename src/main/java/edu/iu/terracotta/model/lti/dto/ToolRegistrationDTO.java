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

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ToolRegistrationDTO {

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
    private ToolConfigurationDTO toolConfiguration;

}
