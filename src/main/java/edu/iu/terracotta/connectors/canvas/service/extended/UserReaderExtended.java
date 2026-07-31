package edu.iu.terracotta.connectors.canvas.service.extended;

import java.io.IOException;
import java.util.UUID;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.UserExtended;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.ksu.canvas.exception.InvalidOauthTokenException;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.requestOptions.GetUsersInCourseOptions;

public interface UserReaderExtended extends CanvasReader<UserExtended, UserReaderExtended> {

    void getUsersInCourse(GetUsersInCourseOptions getUsersInCourseOptions, UUID batchId, Long contextId) throws IOException, InvalidOauthTokenException, TerracottaConnectorException;

}
