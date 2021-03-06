package edu.iu.terracotta.service.canvas;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.ksu.canvas.model.Progress;
import edu.ksu.canvas.model.assignment.Submission;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface CanvasAPIClient {

    Optional<AssignmentExtended> createCanvasAssignment(AssignmentExtended canvasAssignment, String canvasCourseId, PlatformDeployment platformDeployment ) throws CanvasApiException;

    List<AssignmentExtended> listAssignments(String canvasCourseId, PlatformDeployment platformDeployment) throws CanvasApiException;

    List<Submission> listSubmissions(Integer assignmentId, String canvasCourseId, PlatformDeployment platformDeployment) throws CanvasApiException, IOException;

    List<Submission> listSubmissionsForGivenUser(Integer assignmentId, String canvasCourseId, String canvasUserId, PlatformDeployment platformDeployment) throws CanvasApiException, IOException;

    Optional<AssignmentExtended> checkAssignmentExists(Integer assignmentId, String canvasCourseId, PlatformDeployment platformDeployment) throws CanvasApiException;

    Optional<AssignmentExtended> listAssignment(String canvasCourseId, int assignmentId, PlatformDeployment platformDeployment) throws CanvasApiException;

    Optional<AssignmentExtended> editAssignment(AssignmentExtended assignmentExtended, String canvasCourseId, PlatformDeployment platformDeployment) throws CanvasApiException;

    Optional<AssignmentExtended> deleteAssignment(AssignmentExtended assignmentExtended, String canvasCourseId, PlatformDeployment platformDeployment) throws CanvasApiException;

    Optional<Progress> postSubmission(edu.iu.terracotta.model.app.Submission submission, Float maxTerracottaScore) throws CanvasApiException, IOException;

    /**
     * Post a 1.0 point score for the consent assignment back to Canvas.
     * @param participant
     * @return
     * @throws CanvasApiException
     * @throws IOException
     */
    Optional<Progress> postConsentSubmission(Participant participant) throws CanvasApiException, IOException;
}
