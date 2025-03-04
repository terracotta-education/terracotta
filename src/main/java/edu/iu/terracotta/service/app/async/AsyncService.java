package edu.iu.terracotta.service.app.async;

import java.io.IOException;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.exceptions.DataServiceException;

public interface AsyncService {

    void checkAndRestoreAssignmentsInLmsByContext(SecuredInfo securedInfo) throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException;
    void handleObsoleteAssignmentsInLmsByContext(SecuredInfo securedInfo) throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException;
    void processAssignmentFileArchive(AssignmentFileArchive assignmentFileArchive) throws IOException;

}
