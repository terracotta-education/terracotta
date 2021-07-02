package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;

public interface ScheduledService {
    void hello();
    void restoreDeletedAssignments() throws DataServiceException, CanvasApiException, ConnectionException;
}
