package edu.iu.terracotta.service.canvas;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.ksu.canvas.model.assignment.Assignment;

import java.util.Optional;

public interface CanvasAPIClient {

    Optional<Assignment> createCanvasAssignment(Assignment canvasAssignment, String courseId, PlatformDeployment platformDeployment ) throws CanvasApiException;


}
