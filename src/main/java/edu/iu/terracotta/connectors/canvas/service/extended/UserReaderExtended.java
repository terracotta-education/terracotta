package edu.iu.terracotta.connectors.canvas.service.extended;

import java.io.IOException;
import java.util.List;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.UserExtended;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.requestOptions.GetUsersInCourseOptions;

public interface UserReaderExtended extends CanvasReader<UserExtended, UserReaderExtended> {

    List<UserExtended> getUsersInCourse(GetUsersInCourseOptions getUsersInCourseOptions) throws IOException;

}
