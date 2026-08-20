package edu.iu.terracotta.service.app.async;

import java.io.IOException;
import java.util.UUID;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.exceptions.DataServiceException;

public interface ParticipantAsyncService {

    void updateParticipantData(SecuredInfo securedInfo) throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException;
    void prepareParticipationAsync(long experimentId, SecuredInfo securedInfo, UUID batchId);

    /**
     * Best-effort, fire-and-forget: refreshes the participant roster (see
     * ParticipantService.refreshParticipantsIfStale) for every experiment in this LTI context,
     * so an instructor's launch keeps the roster reasonably fresh without waiting for someone to
     * open the manual-participation-selection screen. Never blocks or fails the launch itself -
     * each experiment's refresh failure is caught and logged independently.
     */
    void refreshParticipantsForContext(LtiContextEntity ltiContextEntity);

}
