package edu.iu.terracotta.service.app;

import java.io.IOException;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;

public interface AdminService {

    void resyncTargetUris(long platformDeploymentId, String tokenOverride) throws CanvasApiException, DataServiceException, ConnectionException, IOException;
    boolean isTerracottaAdmin(String userKey);

}
