package edu.iu.terracotta.service.canvas;

import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.ksu.canvas.interfaces.CanvasWriter;

import java.io.IOException;
import java.util.Optional;

public interface AssignmentWriterExtended extends CanvasWriter<AssignmentExtended, AssignmentWriterExtended> {

    Optional<AssignmentExtended> createAssignment(String var1, AssignmentExtended var2) throws IOException;
    Optional<AssignmentExtended> deleteAssignment(String var1, Long var2) throws IOException;
    Optional<AssignmentExtended> editAssignment(String var1, AssignmentExtended var2) throws IOException;

}
