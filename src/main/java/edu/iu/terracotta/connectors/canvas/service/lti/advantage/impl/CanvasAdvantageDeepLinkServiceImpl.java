package edu.iu.terracotta.connectors.canvas.service.lti.advantage.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterables;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.DeepLinkJwtDto;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageDeepLinkService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.lti.Lti3Request;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.LooseCoupling"})
@TerracottaConnector(LmsConnector.CANVAS)
public class CanvasAdvantageDeepLinkServiceImpl implements AdvantageDeepLinkService {

    private final PlatformDeploymentRepository platformDeploymentRepository;
    private final LtiDataService ltiDataService;

    @Override
    public DeepLinkJwtDto generateDeepLinkJwt(List<String> deepLinkRequestIds, Jws<Claims> idToken, String returnUrl) throws GeneralSecurityException, IOException, TerracottaConnectorException {
        Date date = new Date();
        String lmsIssuer = idToken.getPayload().getIssuer();
        String terracottaClientId = Iterables.getOnlyElement(idToken.getPayload().getAudience());
        List<PlatformDeployment> deployments = platformDeploymentRepository.findByIssAndClientId(lmsIssuer, terracottaClientId);

        if (deployments.isEmpty()) {
            throw new TerracottaConnectorException(
                String.format("No PlatformDeployment found for issuer [%s] and clientId [%s]", lmsIssuer, terracottaClientId)
            );
        }

        PlatformDeployment platformDeployment = deployments.get(0);

        Map<?, ?> deepLinkSettings = idToken.getPayload().get(LtiStrings.DEEP_LINKING_SETTINGS, Map.class);
        Object deepLinkData = deepLinkSettings != null ? deepLinkSettings.get(LtiStrings.DEEP_LINK_DATA) : null;

        String jwt = Jwts.builder()
            .header()
                .add(
                    LtiStrings.KID,
                    TextConstants.DEFAULT_KID
                )
                .add(
                    LtiStrings.TYP,
                    LtiStrings.JWT
                )
                .add(
                    LtiStrings.ALG,
                    LtiStrings.RS256
                )
            .and()
            .issuer(terracottaClientId)
            .expiration(
                DateUtils.addSeconds(
                    date,
                    3600
                )
            )
            .issuedAt(date)
            // Brightspace requires a scalar string aud, not an array; single-element audience() serializes as string in JJWT 0.12+
            .audience().add(lmsIssuer).and()
            .claim(
                LtiStrings.LTI_NONCE,
                idToken
                    .getPayload()
                    .get(
                        LtiStrings.LTI_NONCE,
                        String.class
                    )
            )
            .claim(
                LtiStrings.LTI_AZP,
                lmsIssuer
            )
            .claim(
                LtiStrings.LTI_DEPLOYMENT_ID,
                idToken
                    .getPayload()
                    .get(
                        LtiStrings.LTI_DEPLOYMENT_ID,
                        String.class
                    )
            )
            .claim(
                LtiStrings.LTI_MESSAGE_TYPE,
                LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE
            )
            .claim(
                LtiStrings.LTI_VERSION,
                LtiStrings.LTI_VERSION_3
            )
            .claim(
                LtiStrings.LTI_DATA,
                deepLinkData)
            .claim(
                LtiStrings.LTI_CONTENT_ITEMS,
                List.of(
                    createTerracottaDeepLink(
                        platformDeployment.getLocalUrl()
                    )
                )
            )
            .signWith(OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey()), SIG.RS256)
            .compact();

        return DeepLinkJwtDto.builder()
            .jwt(jwt)
            .returnUrl(returnUrl)
            .build();
    }

    private Map<String, Object> createTerracottaDeepLink(String localUrl) {
        Map<String, Object> deepLink = new HashMap<>();
        deepLink.put(LtiStrings.DEEP_LINK_TYPE, LtiStrings.DEEP_LINK_LTIRESOURCELINK);
        deepLink.put(LtiStrings.DEEP_LINK_TITLE, "Terracotta");
        deepLink.put(LtiStrings.DEEP_LINK_URL, String.format("%s/lti3", localUrl));

        Map<String, String> custom = new HashMap<>();
        custom.put("canvas_login_id", "$User.username");
        custom.put("canvas_user_name", "$User.username");
        deepLink.put("custom", custom);

        return deepLink;
    }

    @Override
    public LtiDeepLink generateLtiDeepLink(Lti3Request lti3Request, HttpServletRequest request, String state) throws TerracottaConnectorException, GeneralSecurityException, IOException {
        throw new UnsupportedOperationException("Unimplemented method 'generateLtiDeepLink'");
    }

    @Override
    public LtiDeepLink findByUuid(UUID uuid) throws TerracottaConnectorException {
        throw new UnsupportedOperationException("Unimplemented method 'findByUuid'");
    }

    @Override
    public void delete(LtiDeepLink ltiDeepLink) {
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

}
