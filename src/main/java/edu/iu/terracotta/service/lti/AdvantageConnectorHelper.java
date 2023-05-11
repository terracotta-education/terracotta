package edu.iu.terracotta.service.lti;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ags.LineItem;
import edu.iu.terracotta.model.ags.LineItems;
import edu.iu.terracotta.model.oauth2.LTIToken;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings({"rawtypes"})
public interface AdvantageConnectorHelper {

    HttpEntity createRequestEntity(String apiKey);

    HttpEntity createTokenizedRequestEntity(LTIToken ltiToken);

    HttpEntity<LineItem> createTokenizedRequestEntity(LTIToken ltiToken, LineItem lineItem);

    HttpEntity<LineItems> createTokenizedRequestEntity(LTIToken ltiToken, LineItems lineItems);

    HttpEntity<String> createTokenizedRequestEntity(LTIToken ltiToken, String score);

    LTIToken getToken(PlatformDeployment platformDeployment, String scope) throws ConnectionException;

    RestTemplate createRestTemplate();

    String nextPage(HttpHeaders headers);

}
