package edu.iu.terracotta.connectors.generic.service.lti.advantage;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings({"rawtypes"})
public interface AdvantageConnectorHelper {

    HttpEntity createRequestEntity(String apiKey);
    HttpEntity createTokenizedRequestEntity(LtiToken ltiToken);
    HttpEntity createTokenizedRequestEntityWithAcceptAndContentType(LtiToken ltiToken, String accept, String contentType);
    HttpEntity createTokenizedRequestEntityWithAccept(LtiToken ltiToken, String accept);
    HttpEntity<LineItem> createTokenizedRequestEntity(LtiToken ltiToken, LineItem lineItem);
    HttpEntity<LineItem> createTokenizedRequestEntityWithAcceptAndContentType(LtiToken ltiToken, LineItem lineItem, String accept, String contentType);
    HttpEntity<LineItems> createTokenizedRequestEntity(LtiToken ltiToken, LineItems lineItems);
    HttpEntity<String> createTokenizedRequestEntity(LtiToken ltiToken, String score);
    LtiToken getToken(PlatformDeployment platformDeployment, String scope) throws ConnectionException;
    RestTemplate createRestTemplate();
    String nextPage(HttpHeaders headers);

}
