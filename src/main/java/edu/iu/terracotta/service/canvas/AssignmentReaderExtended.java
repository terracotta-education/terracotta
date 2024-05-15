package edu.iu.terracotta.service.canvas;

import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.requestOptions.GetSingleAssignmentOptions;
import edu.ksu.canvas.requestOptions.ListCourseAssignmentsOptions;
import edu.ksu.canvas.requestOptions.ListUserAssignmentOptions;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface AssignmentReaderExtended extends CanvasReader<AssignmentExtended, AssignmentReaderExtended> {

    Optional<AssignmentExtended> getSingleAssignment(GetSingleAssignmentOptions var1) throws IOException;
    List<AssignmentExtended> listCourseAssignments(ListCourseAssignmentsOptions var1) throws IOException;
    List<AssignmentExtended> listUserAssignments(ListUserAssignmentOptions var1) throws IOException;

}
