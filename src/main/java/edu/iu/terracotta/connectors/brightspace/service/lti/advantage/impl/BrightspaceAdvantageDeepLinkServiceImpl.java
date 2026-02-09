package edu.iu.terracotta.connectors.brightspace.service.lti.advantage.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
@SuppressWarnings({"PMD.LooseCoupling"})
@TerracottaConnector(LmsConnector.BRIGHTSPACE)
public class BrightspaceAdvantageDeepLinkServiceImpl implements AdvantageDeepLinkService {

    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private LtiDataService ltiDataService;

    @Override
    public DeepLinkJwtDto generateDeepLinkJwt(List<String> deepLinkRequestIds, Jws<Claims> idToken, String returnUrl) throws GeneralSecurityException, IOException, TerracottaConnectorException {
        Date date = new Date();
        String lmsIssuer = idToken.getPayload().getIssuer();
        String terracottaClientId = Iterables.getOnlyElement(idToken.getPayload().getAudience());
        PlatformDeployment platformDeployment = platformDeploymentRepository.findByIssAndClientId(lmsIssuer, terracottaClientId).get(0);

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
            .claim(
                LtiStrings.AUD,
                lmsIssuer
            ) // Brightspace requires a string aud
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
                idToken
                    .getPayload()
                    .get(
                        LtiStrings.DEEP_LINKING_SETTINGS,
                        Map.class
                    )
                    .get(LtiStrings.DEEP_LINK_DATA))
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
        custom.put("brightspace_login_id", "$User.username");
        custom.put("brightspace_user_name", "$User.username");
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
