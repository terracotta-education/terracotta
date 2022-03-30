package edu.iu.terracotta.service.lti;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ags.LineItem;
import edu.iu.terracotta.model.ags.LineItems;
import edu.iu.terracotta.model.ags.Score;
import edu.iu.terracotta.model.oauth2.LTIToken;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

public interface AdvantageConnectorHelper {
    HttpEntity createRequestEntity(String apiKey);

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity createTokenizedRequestEntity(LTIToken LTIToken);

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity<LineItem> createTokenizedRequestEntity(LTIToken LTIToken, LineItem lineItem);

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity<LineItems> createTokenizedRequestEntity(LTIToken LTIToken, LineItems lineItems);

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity<String> createTokenizedRequestEntity(LTIToken LTIToken, String score);

    //Asking for a token. The scope will come in the scope parameter
    //The platformDeployment has the URL to ask for the token.
    LTIToken getToken(PlatformDeployment platformDeployment, String scope) throws ConnectionException;

    RestTemplate createRestTemplate();

    String nextPage(HttpHeaders headers);
}
