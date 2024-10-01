package edu.iu.terracotta.service.app.integrations;

import java.util.Optional;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenNotFoundException;

public interface IntegrationScoreService {

    void score(String launchToken, String score, Optional<String> previewTokenClient) throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException;
    Optional<String> getPreviewTokenClient(String launchToken);

}
