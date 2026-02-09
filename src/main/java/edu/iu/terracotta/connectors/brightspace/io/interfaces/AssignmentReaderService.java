package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import edu.iu.terracotta.connectors.brightspace.dao.model.extended.AssignmentExtended;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface AssignmentReaderService extends BrightspaceReaderService<AssignmentExtended, AssignmentReaderService> {

    Optional<AssignmentExtended> getSingleAssignment(String orgUnitId, long assignmentId) throws IOException;
    List<AssignmentExtended> listCourseAssignments(String orgUnitId) throws IOException;
    List<AssignmentExtended> listUserAssignments(String orgUnitId, long userId) throws IOException;

}
