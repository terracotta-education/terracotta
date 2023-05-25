package edu.iu.terracotta.service.canvas;

import java.io.IOException;
import java.util.List;

import edu.iu.terracotta.model.canvas.CourseExtended;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.requestOptions.ListUserCoursesOptions;

public interface CourseReaderExtended extends CanvasReader<CourseExtended, CourseReaderExtended> {

    List<CourseExtended> listCoursesForUser(ListUserCoursesOptions options) throws IOException;

}
