package edu.iu.terracotta.connectors.canvas.service.extended;


import edu.iu.terracotta.connectors.canvas.dao.model.extended.SubmissionExtended;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;

import java.io.IOException;
import java.util.List;

public interface SubmissionReaderExtended extends CanvasReader<SubmissionExtended, SubmissionReaderExtended> {

    List<SubmissionExtended> listSubmissionsForMultipleAssignments(GetSubmissionsOptions options) throws IOException;
    List<SubmissionExtended> getCourseSubmissions(GetSubmissionsOptions options) throws IOException;

}
