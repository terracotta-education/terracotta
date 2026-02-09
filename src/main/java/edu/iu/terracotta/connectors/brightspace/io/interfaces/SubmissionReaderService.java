package edu.iu.terracotta.connectors.brightspace.io.interfaces;


import edu.iu.terracotta.connectors.brightspace.dao.model.extended.SubmissionExtended;

import java.io.IOException;
import java.util.List;

public interface SubmissionReaderService extends BrightspaceReaderService<SubmissionExtended, SubmissionReaderService> {

    List<SubmissionExtended> listSubmissionsForMultipleAssignments(String orgUnitId, List<Long> assignmentIds) throws IOException;
    List<SubmissionExtended> getCourseSubmissions(String orgUnitId) throws IOException;

}
