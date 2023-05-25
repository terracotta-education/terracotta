package edu.iu.terracotta.service.canvas;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.canvas.CourseExtended;
import edu.ksu.canvas.model.assignment.Submission;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface CanvasAPIClient {

    Optional<AssignmentExtended> createCanvasAssignment(LtiUserEntity apiUser, AssignmentExtended canvasAssignment, String canvasCourseId) throws CanvasApiException;

    List<AssignmentExtended> listAssignments(LtiUserEntity apiUser, String canvasCourseId) throws CanvasApiException;

    List<AssignmentExtended> listAssignments(String baseUrl, String canvasCourseId, String tokenOverride) throws CanvasApiException;

    List<Submission> listSubmissions(LtiUserEntity apiUser, Integer assignmentId, String canvasCourseId) throws CanvasApiException, IOException;

    Optional<AssignmentExtended> checkAssignmentExists(LtiUserEntity apiUser, Integer assignmentId, String canvasCourseId) throws CanvasApiException;

    Optional<AssignmentExtended> listAssignment(LtiUserEntity apiUser, String canvasCourseId, int assignmentId) throws CanvasApiException;

    Optional<AssignmentExtended> editAssignment(LtiUserEntity apiUser, AssignmentExtended assignmentExtended, String canvasCourseId) throws CanvasApiException;

    Optional<AssignmentExtended> editAssignment(String baseUrl, AssignmentExtended assignmentExtended, String canvasCourseId, String tokenOverride) throws CanvasApiException;

    Optional<AssignmentExtended> deleteAssignment(LtiUserEntity apiUser, AssignmentExtended assignmentExtended, String canvasCourseId) throws CanvasApiException;

    List<CourseExtended> listCoursesForUser(String baseUrl, String canvasUserId, String tokenOverride) throws CanvasApiException;

    Optional<CourseExtended> editCourse(String baseUrl, CourseExtended courseExtended, String canvasCourseId, String tokenOverride) throws CanvasApiException;

}
