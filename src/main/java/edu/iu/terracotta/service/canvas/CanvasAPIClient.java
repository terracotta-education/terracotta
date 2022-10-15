package edu.iu.terracotta.service.canvas;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.ksu.canvas.model.Progress;
import edu.ksu.canvas.model.assignment.Submission;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface CanvasAPIClient {

    Optional<AssignmentExtended> createCanvasAssignment(LtiUserEntity apiUser, AssignmentExtended canvasAssignment,
            String canvasCourseId) throws CanvasApiException;

    List<AssignmentExtended> listAssignments(LtiUserEntity apiUser, String canvasCourseId) throws CanvasApiException;

    List<Submission> listSubmissions(LtiUserEntity apiUser, Integer assignmentId, String canvasCourseId)
                    throws CanvasApiException, IOException;

    List<Submission> listSubmissionsForGivenUser(LtiUserEntity apiUser, Integer assignmentId, String canvasCourseId,
                    String canvasUserId) throws CanvasApiException, IOException;

    Optional<AssignmentExtended> checkAssignmentExists(LtiUserEntity apiUser, Integer assignmentId,
                    String canvasCourseId) throws CanvasApiException;

    Optional<AssignmentExtended> listAssignment(LtiUserEntity apiUser, String canvasCourseId, int assignmentId)
                    throws CanvasApiException;

    Optional<AssignmentExtended> editAssignment(LtiUserEntity apiUser, AssignmentExtended assignmentExtended,
                    String canvasCourseId) throws CanvasApiException;

    Optional<AssignmentExtended> deleteAssignment(LtiUserEntity apiUser, AssignmentExtended assignmentExtended,
                    String canvasCourseId) throws CanvasApiException;

    Optional<Progress> postSubmission(LtiUserEntity apiUser, edu.iu.terracotta.model.app.Submission submission,
            Float maxTerracottaScore) throws CanvasApiException, IOException;

    /**
     * Post a 1.0 point score for the consent assignment back to Canvas.
     * @param participant
     * @return
     * @throws CanvasApiException
     * @throws IOException
     */
    Optional<Progress> postConsentSubmission(LtiUserEntity apiUser, Participant participant)
            throws CanvasApiException, IOException;
}
