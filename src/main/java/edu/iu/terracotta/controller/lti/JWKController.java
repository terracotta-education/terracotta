package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serving the public key of the tool.
 */
@RestController
@Scope("session")
@RequestMapping("/jwks")
public class JwkController {

    @Autowired private LtiDataService ltiDataService;

    @GetMapping(value = "/jwk", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<Map<String, Object>>> jwk(HttpServletRequest req, Model model) throws GeneralSecurityException {
        RSAPublicKey toolPublicKey = OAuthUtils.loadPublicKey(ltiDataService.getOwnPublicKey());

        Map<String, Object> values = new HashMap<>();
        values.put("kty", toolPublicKey.getAlgorithm()); // getAlgorithm() returns kty not algorithm
        values.put("kid", TextConstants.DEFAULT_KID);
        values.put("n", Base64.getUrlEncoder().encodeToString(toolPublicKey.getModulus().toByteArray()));
        values.put("e", Base64.getUrlEncoder().encodeToString(toolPublicKey.getPublicExponent().toByteArray()));
        values.put("alg", "RS256");
        values.put("use", "sig");

        return Collections.singletonMap("keys", Collections.singletonList(values));
    }

}
