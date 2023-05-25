package edu.iu.terracotta.service.canvas;

import java.io.IOException;
import java.util.Optional;

import edu.iu.terracotta.model.canvas.CourseExtended;
import edu.ksu.canvas.interfaces.CanvasWriter;

public interface CourseWriterExtended extends CanvasWriter<CourseExtended, CourseWriterExtended> {

    Optional<CourseExtended> editCourse(String canvasCourseId, CourseExtended courseExtended) throws IOException;

}
