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

    LTIToken getToken(String scope, PlatformDeployment platformDeployment) throws ConnectionException;

    LineItems getLineItems(LTIToken ltiToken, LtiContextEntity context) throws ConnectionException;

    boolean deleteLineItem(LTIToken ltiToken, LtiContextEntity context, String id) throws ConnectionException;

    LineItem putLineItem(LTIToken ltiToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException;

    LineItem getLineItem(LTIToken ltiToken, LtiContextEntity context, String id) throws ConnectionException;

    LineItems postLineItems(LTIToken ltiToken, LtiContextEntity context, LineItems lineItems) throws ConnectionException;

    Results getResults(LTIToken ltiTokenResults, LtiContextEntity context, String lineItemId) throws ConnectionException;

    void postScore(LTIToken ltiTokenScores, LTIToken ltiTokenResults, LtiContextEntity context, String lineItemId, Score score) throws ConnectionException;

}
