package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import edu.iu.terracotta.connectors.brightspace.dao.model.extended.AssignmentExtended;
import edu.iu.terracotta.connectors.brightspace.io.model.Assignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;

import java.io.IOException;
import java.util.Optional;

public interface AssignmentWriterService extends BrightspaceWriterService<AssignmentExtended, AssignmentWriterService> {

    Optional<AssignmentExtended> createAssignment(String orgUnitId, Assignment assignment) throws IOException;
    Optional<AssignmentExtended> editAssignment(String orgUnitId, LmsAssignment lmsAssignment) throws IOException;
    Optional<AssignmentExtended> deleteAssignment(String orgUnitId, LmsAssignment lmsAssignment) throws IOException;

}
