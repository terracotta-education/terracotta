package edu.iu.terracotta.model.lti.dto;

import jakarta.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginInitiationDTO {

    private String iss;
    private String loginHint;
    private String targetLinkUri;
    private String ltiMessageHint;
    private String clientId;
    private String deploymentId;

    public LoginInitiationDTO(HttpServletRequest req) {
        this(
            req.getParameter("iss"),
            req.getParameter("login_hint"),
            req.getParameter("target_link_uri"),
            req.getParameter("lti_message_hint"),
            req.getParameter("client_id"),
            req.getParameter("lti_deployment_id")
        );
    }

}
