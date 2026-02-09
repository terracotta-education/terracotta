package edu.iu.terracotta.connectors.generic.service.lti.advantage;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.DeepLinkJwtDto;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.utils.lti.Lti3Request;

@TerracottaConnector(LmsConnector.GENERIC)
public interface AdvantageDeepLinkService {

    LtiDeepLink findByUuid(UUID uuid) throws TerracottaConnectorException;
    void delete(LtiDeepLink ltiDeepLink);
    LtiDeepLink generateLtiDeepLink(Lti3Request lti3Request, HttpServletRequest request, String state) throws TerracottaConnectorException, GeneralSecurityException, IOException;
    DeepLinkJwtDto generateDeepLinkJwt(List<String> deepLinkRequestIds, Jws<Claims> idToken, String returnUrl) throws GeneralSecurityException, IOException, TerracottaConnectorException;

}
