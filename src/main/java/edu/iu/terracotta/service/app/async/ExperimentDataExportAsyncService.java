package edu.iu.terracotta.service.app.async;

import java.io.IOException;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.export.data.ExperimentDataExport;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.export.data.ExperimentDataExportException;

public interface ExperimentDataExportAsyncService {

    void process(ExperimentDataExport experimentDataExport, SecuredInfo securedInfo) throws ExperimentDataExportException, NumberFormatException, IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, TerracottaConnectorException;

}
