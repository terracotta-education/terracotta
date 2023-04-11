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

import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
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
public class JWKController {

    @Autowired
    private LTIDataService ltiDataService;

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
