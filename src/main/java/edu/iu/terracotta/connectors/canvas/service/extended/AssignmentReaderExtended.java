package edu.iu.terracotta.connectors.canvas.service.extended;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.AssignmentExtended;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.requestOptions.GetSingleAssignmentOptions;
import edu.ksu.canvas.requestOptions.ListCourseAssignmentsOptions;
import edu.ksu.canvas.requestOptions.ListUserAssignmentOptions;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface AssignmentReaderExtended extends CanvasReader<AssignmentExtended, AssignmentReaderExtended> {

    Optional<AssignmentExtended> getSingleAssignment(GetSingleAssignmentOptions options) throws IOException;
    List<AssignmentExtended> listCourseAssignments(ListCourseAssignmentsOptions options) throws IOException;
    List<AssignmentExtended> listUserAssignments(ListUserAssignmentOptions options) throws IOException;

}
