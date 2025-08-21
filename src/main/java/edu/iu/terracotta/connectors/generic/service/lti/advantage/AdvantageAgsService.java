package edu.iu.terracotta.connectors.generic.service.lti.advantage;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Results;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

@TerracottaConnector(LmsConnector.GENERIC)
public interface AdvantageAgsService {

    LtiToken getToken(LtiAgsScope scope, PlatformDeployment platformDeployment) throws ConnectionException;
    LineItem getLineItem(LtiToken ltiToken, LtiContextEntity context, String id) throws ConnectionException;
    LineItem postLineItem(LtiToken ltiToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException, TerracottaConnectorException;
    LineItem putLineItem(LtiToken ltiToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException;
    boolean deleteLineItem(LtiToken ltiToken, LtiContextEntity context, String id) throws ConnectionException, TerracottaConnectorException;
    LineItems getLineItems(LtiToken ltiToken, LtiContextEntity context) throws ConnectionException, TerracottaConnectorException;
    LineItems postLineItems(LtiToken ltiToken, LtiContextEntity context, LineItems lineItems) throws ConnectionException;
    Results getResults(LtiToken ltiTokenResults, LtiContextEntity context, String lineItemId) throws ConnectionException;
    void postScore(LtiToken ltiTokenScores, LtiToken ltiTokenResults, LtiContextEntity context, String lineItemId, Score score) throws ConnectionException, TerracottaConnectorException;

}
