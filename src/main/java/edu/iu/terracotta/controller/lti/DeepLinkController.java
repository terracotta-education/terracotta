package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lti.LtiJwtService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageDeepLinkService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@Scope("session")
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LooseCoupling"})
public class DeepLinkController {

    @Autowired AdvantageDeepLinkService deepLinkService;
    @Autowired LtiJwtService ltiJwtService;

    @GetMapping({"/deeplink/toJwt/{id}"})
    public ResponseEntity<Object> deepLinksToJwt(@PathVariable UUID id, HttpServletRequest req) throws GeneralSecurityException, IOException, TerracottaConnectorException {
        try {
            LtiDeepLink ltiDeepLink = deepLinkService.findByUuid(id);
            Jws<Claims> stateClaims = ltiJwtService.validateState(ltiDeepLink.getState());
            Jws<Claims> idToken = ltiJwtService.validateJWT(ltiDeepLink.getIdToken(), stateClaims.getPayload().get("clientId", String.class));

            try {
                deepLinkService.delete(ltiDeepLink);
            } catch (Exception e) {
                // error occurred deleting the DeepLink, but we can still return the JWT, so log the error and continue
                log.warn(
                    "Error deleting DeepLink with UUID: [{}]",
                    id,
                    e
                );
            }

            return ResponseEntity.ok(
                deepLinkService.generateDeepLinkJwt(
                    List.of("terracotta"),
                    idToken,
                    ltiDeepLink.getReturnUrl()
                )
            );
        } catch (TerracottaConnectorException | GeneralSecurityException | IOException e) {
            log.error(
                "Error generating DeepLink JWT with UUID: [{}]",
                id,
                e
            );

            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error(
                "Unexpected error generating DeepLink JWT with UUID: [{}]",
                id,
                e
            );

            return ResponseEntity.internalServerError().build();
        }
    }

}
