package edu.iu.terracotta.service.canvas;


import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.model.assignment.Submission;
import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;

import java.io.IOException;
import java.util.List;

public interface SubmissionReaderExtended extends CanvasReader<Submission, SubmissionReaderExtended> {


    List<Submission> listSubmissionsForMultipleAssignments(GetSubmissionsOptions options) throws IOException;


}
