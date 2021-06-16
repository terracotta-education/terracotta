package edu.iu.terracotta.service.canvas;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.canvas.AssignmentExtended;

import java.util.Optional;

public interface CanvasAPIClient {

    Optional<AssignmentExtended> createCanvasAssignment(AssignmentExtended canvasAssignment, String contextMembershipUrl, PlatformDeployment platformDeployment ) throws CanvasApiException;


}
