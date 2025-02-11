package edu.iu.terracotta.connectors.canvas.service.extended;

import java.io.IOException;
import java.util.Optional;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.CourseExtended;
import edu.ksu.canvas.interfaces.CanvasWriter;

public interface CourseWriterExtended extends CanvasWriter<CourseExtended, CourseWriterExtended> {

    Optional<CourseExtended> editCourse(String courseId, CourseExtended courseExtended) throws IOException;

}
