package edu.iu.terracotta.service.app.integrations;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.integrations.IntegrationToken;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.exceptions.DataServiceException;

public interface IntegrationTokenService {

    void create(Submission submission, SecuredInfo securedInfo) throws IntegrationTokenNotFoundException;
    IntegrationToken findByToken(String token) throws IntegrationTokenNotFoundException;
    IntegrationToken redeemToken(String token)
        throws DataServiceException, IntegrationTokenNotFoundException, IntegrationTokenInvalidException, IntegrationTokenAlreadyRedeemedException, IntegrationTokenExpiredException;

}
