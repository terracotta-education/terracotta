package edu.iu.terracotta.service.app.integrations;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.integrations.IntegrationToken;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

public interface IntegrationTokenService {

    void create(Submission submission, SecuredInfo securedInfo) throws IntegrationTokenNotFoundException;
    IntegrationToken findByToken(String token) throws IntegrationTokenNotFoundException;
    IntegrationToken redeemToken(String token) throws DataServiceException, IntegrationTokenNotFoundException, IntegrationTokenInvalidException, IntegrationTokenAlreadyRedeemedException, IntegrationTokenExpiredException;

}
