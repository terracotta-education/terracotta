package edu.iu.terracotta.service.app.async;

import java.io.IOException;
import java.util.UUID;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.exceptions.DataServiceException;

public interface ParticipantAsyncService {

    void updateParticipantData(SecuredInfo securedInfo) throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException;
    void prepareParticipationAsync(long experimentId, SecuredInfo securedInfo, UUID batchId);

}
