package edu.iu.terracotta.connectors.generic.service.lti.advantage.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterables;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.DeepLinkJwtDto;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiDeepLinkRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiJwtService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageDeepLinkService;
import edu.iu.terracotta.utils.lti.Lti3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;

@Primary
@Service
@SuppressWarnings({"PMD.LooseCoupling"})
public class AdvantageDeepLinkServiceImpl implements AdvantageDeepLinkService {

    @Autowired private LtiDeepLinkRepository ltiDeepLinkRepository;
    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private ConnectorService<AdvantageDeepLinkService> connectorService;
    @Autowired private LtiJwtService ltijwtService;

    private AdvantageDeepLinkService instance(PlatformDeployment platformDeployment) throws TerracottaConnectorException {
        return connectorService.instance(platformDeployment, AdvantageDeepLinkService.class);
    }

    @Override
    public DeepLinkJwtDto generateDeepLinkJwt(List<String> deepLinkRequestIds, Jws<Claims> idToken, String returnUrl) throws GeneralSecurityException, IOException, TerracottaConnectorException {
        return instance(
            platformDeploymentRepository.findByIssAndClientId(
                idToken.getPayload().getIssuer(),
                Iterables.getOnlyElement(idToken.getPayload().getAudience())
            )
            .get(0)
        )
        .generateDeepLinkJwt(
            deepLinkRequestIds,
            idToken,
            returnUrl
        );
    }

    @Override
    public LtiDeepLink generateLtiDeepLink(Lti3Request lti3Request, HttpServletRequest request, String state) throws TerracottaConnectorException, GeneralSecurityException, IOException {
        Jws<Claims> stateClaims = ltijwtService.validateState(state);

        return ltiDeepLinkRepository.save(
            LtiDeepLink.builder()
                .idToken(request.getParameter("id_token"))
                .nonce(stateClaims.getPayload().getId())
                .returnUrl(lti3Request.getDeepLinkReturnUrl())
                .state(state)
                .token(ltijwtService.generateTokenRequestJWT(lti3Request.getKey()))
                .build()
        );
    }

    @Override
    public LtiDeepLink findByUuid(UUID uuid) throws TerracottaConnectorException {
        return ltiDeepLinkRepository.findByUuid(uuid)
            .orElseThrow(() -> new TerracottaConnectorException(String.format("DeepLink not found with UUID: [%s]", uuid)));
    }

    @Override
    public void delete(LtiDeepLink ltiDeepLink) {
        ltiDeepLinkRepository.delete(ltiDeepLink);
    }

}
