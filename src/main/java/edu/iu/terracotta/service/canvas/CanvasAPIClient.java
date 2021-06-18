package edu.iu.terracotta.service.canvas;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.ksu.canvas.model.assignment.Submission;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface CanvasAPIClient {

    Optional<AssignmentExtended> createCanvasAssignment(AssignmentExtended canvasAssignment, String contextMembershipUrl, PlatformDeployment platformDeployment ) throws CanvasApiException;

    List<AssignmentExtended> listAssignments(String contextMembershipUrl, PlatformDeployment platformDeployment) throws CanvasApiException;

    List<Submission> listSubmissions(Integer assignmentId, String contextMembershipUrl, PlatformDeployment platformDeployment) throws CanvasApiException, IOException;


}
