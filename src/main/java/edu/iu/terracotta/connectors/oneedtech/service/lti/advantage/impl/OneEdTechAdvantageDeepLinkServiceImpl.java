package edu.iu.terracotta.connectors.oneedtech.service.lti.advantage.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.DeepLinkJwtDto;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageDeepLinkService;
import edu.iu.terracotta.utils.lti.Lti3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;

@Service
@TerracottaConnector(LmsConnector.ONE_ED_TECH)
public class OneEdTechAdvantageDeepLinkServiceImpl implements AdvantageDeepLinkService {

    @Override
    public DeepLinkJwtDto generateDeepLinkJwt(List<String> deepLinkRequestIds, Jws<Claims> idToken, String returnUrl) throws java.security.GeneralSecurityException, java.io.IOException {
        throw new UnsupportedOperationException("Unimplemented method 'generateDeepLinkJwt'");
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
