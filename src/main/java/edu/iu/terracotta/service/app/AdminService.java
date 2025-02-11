package edu.iu.terracotta.service.app;

import java.io.IOException;

import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;

public interface AdminService {

    void resyncTargetUris(long platformDeploymentId, String tokenOverride) throws DataServiceException, ConnectionException, IOException, ApiException;
    boolean isTerracottaAdmin(String userKey);

}
