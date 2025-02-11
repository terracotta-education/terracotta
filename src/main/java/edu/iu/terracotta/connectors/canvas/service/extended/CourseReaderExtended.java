package edu.iu.terracotta.connectors.canvas.service.extended;

import java.io.IOException;
import java.util.List;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.CourseExtended;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.requestOptions.ListUserCoursesOptions;

public interface CourseReaderExtended extends CanvasReader<CourseExtended, CourseReaderExtended> {

    List<CourseExtended> listCoursesForUser(ListUserCoursesOptions options) throws IOException;

}
