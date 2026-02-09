package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.List;

import edu.iu.terracotta.connectors.brightspace.dao.model.extended.CourseExtended;

public interface CourseReaderService extends BrightspaceReaderService<CourseExtended, CourseReaderService> {

    List<CourseExtended> listCoursesForUser(long userId) throws IOException;

}
