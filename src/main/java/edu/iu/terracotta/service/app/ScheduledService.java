package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;

import java.io.IOException;

public interface ScheduledService {
    void restoreDeletedAssignments() throws DataServiceException, CanvasApiException, ConnectionException, IOException, AssignmentNotCreatedException;
}
