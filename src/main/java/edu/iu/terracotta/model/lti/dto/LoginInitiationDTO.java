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

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginInitiationDTO {

    private String iss;
    private String loginHint;
    private String targetLinkUri;
    private String ltiMessageHint;
    private String clientId;
    private String deploymentId;


    public LoginInitiationDTO(String iss, String loginHint, String targetLinkUri, String ltiMessageHint, String clientId, String deploymentId) {
        this.iss = iss;
        this.loginHint = loginHint;
        this.targetLinkUri = targetLinkUri;
        this.ltiMessageHint = ltiMessageHint;
        this.clientId = clientId;
        this.deploymentId = deploymentId;
    }

    public LoginInitiationDTO(HttpServletRequest req) {
        this(req.getParameter("iss"),
                req.getParameter("login_hint"),
                req.getParameter("target_link_uri"),
                req.getParameter("lti_message_hint"),
                req.getParameter("client_id"),
                req.getParameter("lti_deployment_id")
        );
    }

}
