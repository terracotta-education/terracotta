package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Serving the public key of the tool.
 */
@RestController
@Scope("session")
@RequestMapping("/jwks")
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LooseCoupling"})
public class JwkController {

    @Autowired private LtiDataService ltiDataService;

    @GetMapping(value = "/jwk", produces = MediaType.APPLICATION_JSON_VALUE)
    public String jwk(HttpServletRequest req, Model model) throws GeneralSecurityException {
        RSAPublicKey toolPublicKey = OAuthUtils.loadPublicKey(ltiDataService.getOwnPublicKey());

        return new StringBuilder()
            .append("{\n")
                .append("\"keys\":[\n")
                    .append("{\n")
                        .append("\"kty\":\"RSA\",\n")
                        .append("\"use\":\"sig\",\n")
                        .append("\"alg\":\"RS256\",\n")
                        .append("\"kid\":\"OWNKEY\",\n")
                        .append("\"n\":\"")
                            .append(toBase64UrlUnsigned(toolPublicKey.getModulus()))
                            .append("\",\n")
                        .append("\"e\":\"")
                            .append(toBase64UrlUnsigned(toolPublicKey.getPublicExponent()))
                            .append("\"\n")
                    .append("}\n")
                .append("]\n")
            .append("}\n")
            .toString();
    }

    // BigInteger -> unsigned big-endian bytes -> base64url without padding
    private static String toBase64UrlUnsigned(BigInteger v) {
        byte[] bytes = v.toByteArray();
        // BigInteger.toByteArray() may include a leading 0x00 to force positive sign.
        if (bytes.length > 1 && bytes[0] == 0x00) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            bytes = tmp;
        }

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
