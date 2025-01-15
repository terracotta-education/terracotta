package edu.iu.terracotta.service.app.integrations;

import java.util.Optional;

import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.exceptions.DataServiceException;

public interface IntegrationScoreService {

    void score(String launchToken, String score, Optional<String> previewTokenClient)
        throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException;
    Optional<String> getPreviewTokenClient(String launchToken);

}
