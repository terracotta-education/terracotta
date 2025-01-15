package edu.iu.terracotta.connectors.canvas.service.extended;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.AssignmentExtended;
import edu.ksu.canvas.interfaces.CanvasWriter;
import edu.ksu.canvas.model.assignment.Assignment;

import java.io.IOException;
import java.util.Optional;

public interface AssignmentWriterExtended extends CanvasWriter<AssignmentExtended, AssignmentWriterExtended> {

    Optional<AssignmentExtended> createAssignment(String courseId, Assignment assignment) throws IOException;
    Optional<AssignmentExtended> deleteAssignment(String courseId, Long canvasAssignmentId) throws IOException;
    Optional<AssignmentExtended> editAssignment(String courseId, Assignment assignment) throws IOException;

}
