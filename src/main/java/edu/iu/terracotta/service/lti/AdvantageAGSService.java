package edu.iu.terracotta.service.lti;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ags.LineItem;
import edu.iu.terracotta.model.ags.LineItems;
import edu.iu.terracotta.model.ags.Results;
import edu.iu.terracotta.model.ags.Score;
import edu.iu.terracotta.model.oauth2.LTIToken;

public interface AdvantageAGSService {
    //Asking for a token with the right scope.
    LTIToken getToken(String scope, PlatformDeployment platformDeployment) throws ConnectionException;

    //Calling the AGS service and getting a paginated result of lineitems.
    LineItems getLineItems(LTIToken LTIToken, LtiContextEntity context) throws ConnectionException;

    boolean deleteLineItem(LTIToken LTIToken, LtiContextEntity context, String id) throws ConnectionException;

    LineItem putLineItem(LTIToken LTIToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException;

    LineItem getLineItem(LTIToken LTIToken, LtiContextEntity context, String id) throws ConnectionException;

    LineItems postLineItems(LTIToken LTIToken, LtiContextEntity context, LineItems lineItems) throws ConnectionException;

    Results getResults(LTIToken LTITokenResults, LtiContextEntity context, String lineItemId) throws ConnectionException;

    Results postScore(LTIToken LTITokenScores, LTIToken LTITokenResults,LtiContextEntity context, String lineItemId, Score score) throws ConnectionException;
}
