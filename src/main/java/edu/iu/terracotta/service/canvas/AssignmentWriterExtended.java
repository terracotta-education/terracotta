package edu.iu.terracotta.service.canvas;

import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.ksu.canvas.interfaces.AssignmentWriter;
import edu.ksu.canvas.interfaces.CanvasWriter;
import edu.ksu.canvas.model.assignment.Assignment;

import java.io.IOException;
import java.util.Optional;

public interface AssignmentWriterExtended extends CanvasWriter<AssignmentExtended, AssignmentWriterExtended> {
    Optional<AssignmentExtended> createAssignment(String var1, AssignmentExtended var2) throws IOException;

    Optional<AssignmentExtended> deleteAssignment(String var1, Integer var2) throws IOException;

    Optional<AssignmentExtended> editAssignment(String var1, Assignment var2) throws IOException;
}
