package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;

import java.io.IOException;
import java.util.Map;

public interface ExportService {

    Map<String, String> getFiles(long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, IOException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException;

}
