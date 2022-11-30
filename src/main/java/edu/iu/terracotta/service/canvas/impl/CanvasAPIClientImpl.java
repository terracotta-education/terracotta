package edu.iu.terracotta.service.canvas.impl;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.LMSOAuthException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.canvas.CanvasAPITokenEntity;
import edu.iu.terracotta.service.canvas.AssignmentReaderExtended;
import edu.iu.terracotta.service.canvas.AssignmentWriterExtended;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.ksu.canvas.exception.CanvasException;
import edu.ksu.canvas.exception.ObjectNotFoundException;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.interfaces.CanvasWriter;
import edu.ksu.canvas.interfaces.SubmissionReader;
import edu.ksu.canvas.model.assignment.Submission;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSingleAssignmentOptions;
import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;
import edu.ksu.canvas.requestOptions.ListCourseAssignmentsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;


@Service
public class CanvasAPIClientImpl implements CanvasAPIClient {

    private static final Logger logger = LoggerFactory.getLogger(CanvasAPIClientImpl.class);

    private static final String SCOPE_ASSIGNMENT_CREATE = "url:POST|/api/v1/courses/:course_id/assignments";
    private static final String SCOPE_ASSIGNMENTS_LIST = "url:GET|/api/v1/courses/:course_id/assignments";
    private static final String SCOPE_ASSIGNMENT_GET = "url:GET|/api/v1/courses/:course_id/assignments/:id";
    private static final String SCOPE_ASSIGNMENT_EDIT = "url:PUT|/api/v1/courses/:course_id/assignments/:id";
    private static final String SCOPE_ASSIGNMENT_DELETE = "url:DELETE|/api/v1/courses/:course_id/assignments/:id";
    private static final String SCOPE_SUBMISSIONS_FOR_ASSIGNMENT_LIST = "url:GET|/api/v1/courses/:course_id/assignments/:assignment_id/submissions";

    /**
     * All scopes that are required by the methods in this class.
     */
    public static final List<String> SCOPES_REQUIRED = Arrays.asList(
            SCOPE_ASSIGNMENT_CREATE,
            SCOPE_ASSIGNMENT_DELETE,
            SCOPE_ASSIGNMENT_EDIT,
            SCOPE_ASSIGNMENT_GET,
            SCOPE_ASSIGNMENTS_LIST,
            SCOPE_SUBMISSIONS_FOR_ASSIGNMENT_LIST);

    @Autowired
    CanvasOAuthServiceImpl canvasOAuthService;

    /**
     * required scopes:
     * <ul>
     * <li>{@value #SCOPE_ASSIGNMENT_CREATE}
     * </ul>
     */
    @Override
    public Optional<AssignmentExtended> createCanvasAssignment(LtiUserEntity apiUser,
            AssignmentExtended canvasAssignment, String canvasCourseId) throws CanvasApiException {
        //https://github.com/kstateome/canvas-api/tree/master

        try {
            AssignmentWriterExtended assignmentWriter = getWriter(apiUser, AssignmentWriterExtended.class);
            return assignmentWriter.createAssignment(canvasCourseId, canvasAssignment);
        } catch (IOException | CanvasException ex) {
            throw new CanvasApiException(
                    "Failed to create Assignment in Canvas course by ID [" + canvasCourseId + "]", ex);
        }
    }

    /**
     * required scopes:
     * <ul>
     * <li>{@value #SCOPE_ASSIGNMENTS_LIST}
     * </ul>
     */
    @Override
    public List<AssignmentExtended> listAssignments(LtiUserEntity apiUser, String canvasCourseId)
            throws CanvasApiException {
        try {
            AssignmentReaderExtended assignmentReader = getReader(apiUser, AssignmentReaderExtended.class);
            ListCourseAssignmentsOptions listCourseAssignmentsOptions = new ListCourseAssignmentsOptions(canvasCourseId);
            return assignmentReader.listCourseAssignments(listCourseAssignmentsOptions);
        } catch (IOException | CanvasException ex) {
            throw new CanvasApiException(
                    "Failed to get the list of assignments Canvas course [" + canvasCourseId + "]", ex);
        }
    }

    /**
     * required scopes:
     * <ul>
     * <li>{@value #SCOPE_ASSIGNMENT_GET}
     * </ul>
     */
    @Override
    public Optional<AssignmentExtended> listAssignment(LtiUserEntity apiUser, String canvasCourseId, int assignmentId)
            throws CanvasApiException {
        try {
            AssignmentReaderExtended assignmentReader = getReader(apiUser, AssignmentReaderExtended.class);
            GetSingleAssignmentOptions assignmentsOptions = new GetSingleAssignmentOptions(canvasCourseId, assignmentId);
            return assignmentReader.getSingleAssignment(assignmentsOptions);
        } catch (ObjectNotFoundException ex) {
            return Optional.empty();
        } catch (IOException | CanvasException ex) {
            throw new CanvasApiException(
                    "Failed to get the assignments with id [" + assignmentId + "] from canvas course [" + canvasCourseId
                            + "]",
                    ex);
        }
    }

    /**
     * required scopes:
     * <ul>
     * <li>{@value #SCOPE_ASSIGNMENT_EDIT}
     * </ul>
     */
    @Override
    public Optional<AssignmentExtended> editAssignment(LtiUserEntity apiUser, AssignmentExtended assignmentExtended,
            String canvasCourseId) throws CanvasApiException {
        try {
            AssignmentWriterExtended assignmentWriter = getWriter(apiUser, AssignmentWriterExtended.class);
            return assignmentWriter.editAssignment(canvasCourseId, assignmentExtended);
        } catch (IOException | CanvasException ex) {
            throw new CanvasApiException(
                    "Failed to edit the assignments with id [" + assignmentExtended.getId() + "] from canvas course [" + canvasCourseId + "]", ex);
        }
    }

    /**
     * required scopes:
     * <ul>
     * <li>{@value #SCOPE_ASSIGNMENT_DELETE}
     * </ul>
     */
    @Override
    public Optional<AssignmentExtended> deleteAssignment(LtiUserEntity apiUser, AssignmentExtended assignmentExtended,
            String canvasCourseId) throws CanvasApiException {
        try {
            AssignmentWriterExtended assignmentWriter = getWriter(apiUser, AssignmentWriterExtended.class);
            return assignmentWriter.deleteAssignment(canvasCourseId, assignmentExtended.getId());
        } catch (IOException | CanvasException ex) {
            throw new CanvasApiException(
                    "Failed to edit the assignments with id [" + assignmentExtended.getId() + "] from canvas course [" + canvasCourseId + "]", ex);
        }
    }


    /**
     * required scopes:
     * <ul>
     * <li>{@value #SCOPE_SUBMISSIONS_FOR_ASSIGNMENT_LIST}
     * </ul>
     */
    @Override
    public List<Submission> listSubmissions(LtiUserEntity apiUser, Integer assignmentId, String canvasCourseId)
            throws CanvasApiException, IOException {
        SubmissionReader submissionReader = getReader(apiUser, SubmissionReader.class);
        GetSubmissionsOptions submissionsOptions = new GetSubmissionsOptions(canvasCourseId, assignmentId);
        submissionsOptions.includes(Collections.singletonList(GetSubmissionsOptions.Include.USER));
        return submissionReader.getCourseSubmissions(submissionsOptions);
    }


    // @Override
    // public List<Submission> listSubmissionsForGivenUser(LtiUserEntity apiUser, Integer assignmentId,
    //         String canvasCourseId, String canvasUserId) throws CanvasApiException, IOException {
    //     PlatformDeployment platformDeployment = apiUser.getPlatformDeployment();
    //     String canvasBaseUrl = platformDeployment.getBaseUrl();
    //     String accessToken = getAccessToken(apiUser);
    //     OauthToken oauthToken = new NonRefreshableOauthToken(accessToken);
    //     CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
    //     SubmissionImpl submissionReader = apiFactory.getReader(SubmissionImpl.class, oauthToken);
    //     GetSubmissionsOptions submissionsOptions = new GetSubmissionsOptions(canvasCourseId, assignmentId);
    //     submissionsOptions.includes(Collections.singletonList(GetSubmissionsOptions.Include.USER));
    //     return submissionReader.getCourseSubmissions(submissionsOptions);
    // }

    // @Override
    // public Optional<Progress> postSubmission(LtiUserEntity apiUser, edu.iu.terracotta.model.app.Submission submission,
    //         Float maxTerracottaScore)
    //         throws CanvasApiException, IOException {

    //     String canvasCourseId = getCanvasCourseId(submission.getParticipant().getLtiMembershipEntity().getContext().getContext_memberships_url());
    //     int assignmentId = Integer.parseInt(submission.getAssessment().getTreatment().getAssignment().getLmsAssignmentId());
    //     String canvasUserId = submission.getParticipant().getLtiUserEntity().getLmsUserId();
    //     Optional<AssignmentExtended> assignmentExtended = listAssignment(apiUser, canvasCourseId, assignmentId);
    //     if (!assignmentExtended.isPresent()) {
    //         throw new CanvasApiException(
    //                 "Failed to get the assignments with id [" + assignmentId + "] from canvas course [" + canvasCourseId + "]");
    //     }
    //     Double maxCanvasScore = assignmentExtended.get().getPointsPossible();

    //     Double grade = Double.valueOf("0");
    //     if (submission.getTotalAlteredGrade() != null) {
    //         grade = Double.parseDouble(submission.getTotalAlteredGrade().toString());
    //     } else if (submission.getAlteredCalculatedGrade() != null) {
    //         grade = Double.parseDouble(submission.getAlteredCalculatedGrade().toString());
    //     } else {
    //         grade = Double.parseDouble(submission.getCalculatedGrade().toString());
    //     }
    //     grade = grade * maxCanvasScore / Double.parseDouble(maxTerracottaScore.toString());

    //     return postGrade(apiUser, canvasCourseId, assignmentId, canvasUserId, grade);
    // }

    // @Override
    // public Optional<Progress> postConsentSubmission(LtiUserEntity apiUser, Participant participant)
    //         throws CanvasApiException, IOException {
    //     String canvasCourseId = getCanvasCourseId(participant.getLtiMembershipEntity().getContext().getContext_memberships_url());
    //     int assignmentId = Integer.parseInt(participant.getExperiment().getConsentDocument().getLmsAssignmentId());
    //     String canvasUserId = participant.getLtiUserEntity().getLmsUserId();
    //     Optional<AssignmentExtended> assignmentExtended = listAssignment(apiUser, canvasCourseId, assignmentId);
    //     if (!assignmentExtended.isPresent()) {
    //         throw new CanvasApiException(
    //                 "Failed to get the assignments with id [" + assignmentId + "] from canvas course [" + canvasCourseId + "]");
    //     }

    //     Double grade = Double.valueOf("1.0");

    //     return postGrade(apiUser, canvasCourseId, assignmentId, canvasUserId, grade);
    // }

    // private Optional<Progress> postGrade(LtiUserEntity apiUser, String canvasCourseId, int assignmentId,
    //         String canvasUserId, Double grade) throws IOException, CanvasApiException {
    //     PlatformDeployment platformDeployment = apiUser.getPlatformDeployment();
    //     String canvasBaseUrl = platformDeployment.getBaseUrl();
    //     String accessToken = getAccessToken(apiUser);
    //     OauthToken oauthToken = new NonRefreshableOauthToken(accessToken);
    //     CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
    //     SubmissionWriter submissionWriter = apiFactory.getWriter(SubmissionWriter.class, oauthToken);
    //     MultipleSubmissionsOptions multipleSubmissionsOptions = new MultipleSubmissionsOptions(canvasCourseId, assignmentId, new HashMap<>());
    //     MultipleSubmissionsOptions.StudentSubmissionOption submissionsOptions =
    //             multipleSubmissionsOptions.createStudentSubmissionOption(
    //                     null,
    //                     grade.toString(),
    //                     false,
    //                     false,
    //                     null,
    //                     null
    //             );
    //     HashMap<String, MultipleSubmissionsOptions.StudentSubmissionOption> submissionOptionHashMap = new HashMap<>();
    //     submissionOptionHashMap.put(canvasUserId, submissionsOptions);
    //     multipleSubmissionsOptions.setStudentSubmissionOptionMap(submissionOptionHashMap);
    //     submissionWriter.gradeMultipleSubmissionsByCourse(multipleSubmissionsOptions);
    //     return submissionWriter.gradeMultipleSubmissionsByCourse(multipleSubmissionsOptions);
    // }

    /**
     * required scopes:
     * <ul>
     * <li>{@value #SCOPE_ASSIGNMENT_GET}
     * </ul>
     */
    @Override
    public Optional<AssignmentExtended> checkAssignmentExists(LtiUserEntity apiUser, Integer assignmentId,
            String canvasCourseId) throws CanvasApiException {
        try {
            logger.debug("Checking if assignment {} exists in Canvas course {}", assignmentId, canvasCourseId);
            AssignmentReaderExtended assignmentReader = getReader(apiUser, AssignmentReaderExtended.class);
            GetSingleAssignmentOptions getSingleAssignmentsOptions = new GetSingleAssignmentOptions(canvasCourseId, assignmentId);
            return assignmentReader.getSingleAssignment(getSingleAssignmentsOptions);
        } catch (edu.ksu.canvas.exception.ObjectNotFoundException e) {
            return Optional.empty();
        } catch (IOException | CanvasException ex) {
            throw new CanvasApiException(
                    "Failed to get the Assignment in Canvas course by ID [" + canvasCourseId + "]", ex);
        }
    }

    // private String getCanvasCourseId(String membershipUrl) {
    //     return StringUtils.substringBetween(membershipUrl, "/courses/", "/");
    // }

    private <T extends CanvasWriter<?, T>> T getWriter(LtiUserEntity apiUser, Class<T> clazz)
            throws CanvasApiException {

        OauthToken oauthToken = getOauthToken(apiUser);
        return getWriterInternal(apiUser, clazz, oauthToken);
    }

    private <T extends CanvasReader<?, T>> T getReader(LtiUserEntity apiUser, Class<T> clazz)
            throws CanvasApiException {

        OauthToken oauthToken = getOauthToken(apiUser);
        return getReaderInternal(apiUser, clazz, oauthToken);
    }

    <T extends CanvasReader<?, T>> T getReaderInternal(LtiUserEntity apiUser, Class<T> clazz,
            OauthToken oauthToken) {
        CanvasApiFactoryExtended apiFactory = getApiFactory(apiUser);
        return apiFactory.getReader(clazz, oauthToken);
    }

    <T extends CanvasWriter<?, T>> T getWriterInternal(LtiUserEntity apiUser, Class<T> clazz,
            OauthToken oauthToken) {
        CanvasApiFactoryExtended apiFactory = getApiFactory(apiUser);
        return apiFactory.getWriter(clazz, oauthToken);
    }

    private CanvasApiFactoryExtended getApiFactory(LtiUserEntity apiUser) {
        PlatformDeployment platformDeployment = apiUser.getPlatformDeployment();
        String canvasBaseUrl = platformDeployment.getBaseUrl();
        return new CanvasApiFactoryExtended(canvasBaseUrl);
    }

    private OauthToken getOauthToken(LtiUserEntity apiUser) throws CanvasApiException {
        String accessToken = getAccessToken(apiUser);
        return new NonRefreshableOauthToken(accessToken);
    }

    private String getAccessToken(LtiUserEntity apiUser)
            throws CanvasApiException {
        String accessToken = null;
        PlatformDeployment platformDeployment = apiUser.getPlatformDeployment();
        if (canvasOAuthService.isConfigured(platformDeployment)) {
            try {
                CanvasAPITokenEntity canvasAPIToken = canvasOAuthService.getAccessToken(apiUser);
                logger.debug("Using access token for user {}", apiUser.getUserKey());
                accessToken = canvasAPIToken.getAccessToken();
            } catch (LMSOAuthException e) {
                throw new CanvasApiException(
                        MessageFormat.format("Could not get a Canvas API token for user {0}", apiUser.getUserKey()), e);
            }
        } else if (platformDeployment.getApiToken() != null) {
            logger.debug("Using admin api token configured for platform deployment {}",
                    platformDeployment.getKeyId());
            accessToken = platformDeployment.getApiToken();
        } else {
            throw new CanvasApiException(MessageFormat.format(
                    "Could not get a Canvas API token for platform deployment {0} and user {1}",
                    platformDeployment.getKeyId(), apiUser.getUserKey()));
        }
        return accessToken;
    }

}
