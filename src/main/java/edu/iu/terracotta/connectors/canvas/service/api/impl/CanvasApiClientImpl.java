package edu.iu.terracotta.connectors.canvas.service.api.impl;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.AssignmentExtended;
import edu.iu.terracotta.connectors.canvas.dao.model.extended.CourseExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.AssignmentReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.AssignmentWriterExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.CourseReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.CourseWriterExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.SubmissionReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.impl.CanvasApiFactoryExtended;
import edu.iu.terracotta.connectors.canvas.service.lms.impl.CanvasLmsOAuthServiceImpl;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiTokenEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsEntity;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Submission;
import edu.ksu.canvas.exception.CanvasException;
import edu.ksu.canvas.exception.ObjectNotFoundException;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.interfaces.CanvasWriter;
import edu.ksu.canvas.model.assignment.Assignment.ExternalToolTagAttribute;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSingleAssignmentOptions;
import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;
import edu.ksu.canvas.requestOptions.ListCourseAssignmentsOptions;
import edu.ksu.canvas.requestOptions.ListUserCoursesOptions;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@TerracottaConnector(LmsConnector.CANVAS)
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LambdaCanBeMethodReference", "PMD.UnusedPrivateMethod"})
public class CanvasApiClientImpl implements ApiClient {

    @Autowired private CanvasLmsOAuthServiceImpl canvasLmsOAuthService;

    @Value("${app.token.logging.enabled:true}")
    private boolean tokenLoggingEnabled;

    @Override
    public AssignmentExtended createLmsAssignment(LtiUserEntity apiUser, Assignment assignment, String canvasCourseId) throws ApiException {
        try {
            AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

            ExternalToolTagAttribute canvasExternalToolTagAttributes = assignmentExtended.getAssignment().new ExternalToolTagAttribute();
            canvasExternalToolTagAttributes.setUrl(
                String.format(
                    "%s/lti3?experiment=%s&assignment=%s",
                    assignment.getExposure().getExperiment().getPlatformDeployment().getLocalUrl(),
                    assignment.getExposure().getExperiment().getExperimentId(),
                    assignment.getAssignmentId()
                )
            );

            assignmentExtended.getAssignment().setExternalToolTagAttributes(canvasExternalToolTagAttributes);
            assignmentExtended.getAssignment().setName(assignment.getTitle());
            assignmentExtended.getAssignment().setDescription(null);
            assignmentExtended.getAssignment().setPublished(false);
            assignmentExtended.getAssignment().setGradingType("percent");
            assignmentExtended.getAssignment().setPointsPossible(100.0);
            assignmentExtended.getAssignment().setSubmissionTypes(Collections.singletonList("external_tool"));

            return getWriter(apiUser, AssignmentWriterExtended.class)
                .createAssignment(canvasCourseId, assignmentExtended.getAssignment())
                .orElseThrow(() -> new ApiException(String.format("Failed to create Assignment in Canvas course by ID [%s]", canvasCourseId)));
        } catch (IOException e) {
            throw new ApiException(String.format("Failed to create Assignment in Canvas course by ID [%s]", canvasCourseId), e);
        }
    }

    @Override
    public AssignmentExtended restoreAssignment(Assignment assignment) throws ApiException, IOException {
        // create the new Assignment in Canvas
        String canvasCourseId = StringUtils.substringBetween(
            assignment.getExposure().getExperiment().getLtiContextEntity().getContext_memberships_url(),
            "courses/",
            "/names"
        );

        return createLmsAssignment(assignment.getExposure().getExperiment().getCreatedBy(), assignment, canvasCourseId);
    }

    @Override
    public List<LmsAssignment> listAssignments(LtiUserEntity apiUser, String canvasCourseId) throws ApiException {
        try {
            return castList(
                getReader(apiUser,AssignmentReaderExtended.class)
                    .listCourseAssignments(new ListCourseAssignmentsOptions(canvasCourseId))
            );
        } catch (IOException e) {
            throw new ApiException(String.format("Failed to get the list of assignments Canvas course by ID [%s]", canvasCourseId), e);
        }
    }

    @Override
    public List<LmsAssignment> listAssignments(PlatformDeployment platformDeployment, String canvasCourseId, String tokenOverride) throws ApiException {
        try {
            return castList(
                getReader(platformDeployment.getBaseUrl(), AssignmentReaderExtended.class, tokenOverride)
                    .listCourseAssignments(new ListCourseAssignmentsOptions(canvasCourseId))
            );
        } catch (IOException | CanvasException ex) {
            throw new ApiException("Failed to get the list of assignments Canvas course [" + canvasCourseId + "]", ex);
        }
    }

    @Override
    public Optional<LmsAssignment> listAssignment(LtiUserEntity apiUser, String canvasCourseId, String canvasAssignmentId) throws ApiException {
        try {
            return castOptional(
                getReader(apiUser, AssignmentReaderExtended.class)
                    .getSingleAssignment(new GetSingleAssignmentOptions(canvasCourseId, Long.parseLong(canvasAssignmentId)))
            );
        } catch (ObjectNotFoundException ex) {
            return Optional.empty();
        } catch (IOException | CanvasException ex) {
            throw new ApiException("Failed to get the assignments with id [" + canvasAssignmentId + "] from canvas course [" + canvasCourseId + "]", ex);
        }
    }

    @Override
    public Optional<LmsAssignment> checkAssignmentExists(LtiUserEntity apiUser, String canvasAssignmentId, String canvasCourseId) throws ApiException {
        try {
            return castOptional(
                getReader(apiUser, AssignmentReaderExtended.class)
                    .getSingleAssignment(new GetSingleAssignmentOptions(canvasCourseId, Long.parseLong(canvasAssignmentId)))
            );
        } catch (ObjectNotFoundException e) {
            return Optional.empty();
        } catch (IOException | CanvasException ex) {
            throw new ApiException("Failed to get the Assignment in Canvas course by ID [" + canvasCourseId + "]", ex);
        }
    }

    @Override
    public Optional<LmsAssignment> editAssignment(LtiUserEntity apiUser, LmsAssignment lmsAssignment, String canvasCourseId) throws ApiException {
        try {
            return castOptional(
                getWriter(apiUser, AssignmentWriterExtended.class)
                    .editAssignment(canvasCourseId, AssignmentExtended.of(lmsAssignment).getAssignment())
            );
        } catch (IOException | CanvasException ex) {
            throw new ApiException("Failed to edit the assignments with id [" + lmsAssignment.getId() + "] from canvas course [" + canvasCourseId + "]", ex);
        }
    }

    @Override
    public Optional<LmsAssignment> editAssignment(PlatformDeployment platformDeployment, LmsAssignment lmsAssignment, String canvasCourseId, String tokenOverride) throws ApiException {
        try {
            return castOptional(
                getWriter(platformDeployment.getBaseUrl(), AssignmentWriterExtended.class, tokenOverride)
                    .editAssignment(canvasCourseId, AssignmentExtended.of(lmsAssignment).getAssignment())
            );
        } catch (IOException | CanvasException e) {
            throw new ApiException(String.format("Failed to edit the assignments with id [%s] from canvas course [%]", lmsAssignment.getId(), canvasCourseId), e);
        }
    }

    @Override
    public void editAssignmentNameInLms(Assignment assignment, String lmsCourseId, String newName, LtiUserEntity instructorUser) throws ApiException, IOException {
        Optional<LmsAssignment> lmsAssignment = listAssignment(instructorUser, lmsCourseId, assignment.getLmsAssignmentId());

        if (lmsAssignment.isEmpty()) {
            log.warn("The assignment [{}] (canvas id: [{}]) was already deleted", assignment.getTitle(), assignment.getLmsAssignmentId());
            return;
        }

        lmsAssignment.get().setName(newName);
        editAssignment(instructorUser, lmsAssignment.get(), lmsCourseId);
    }

    @Override
    public void deleteAssignmentInLms(Assignment assignment, String lmsCourseId, LtiUserEntity instructorUser) throws ApiException, IOException {
        Optional<LmsAssignment> lmsAssignment = listAssignment(instructorUser, lmsCourseId, assignment.getLmsAssignmentId());

        if (lmsAssignment.isEmpty()) {
            log.warn("The assignment [{}] (lms id: [{}]) was already deleted", assignment.getTitle(), assignment.getLmsAssignmentId());
            return;
        }

        deleteAssignmentInLms(lmsAssignment.get(), lmsCourseId, instructorUser);
    }

    @Override
    public void deleteAssignmentInLms(LmsAssignment lmsAssignment, String lmsCourseId, LtiUserEntity instructorUser) throws ApiException, IOException {
        try {
            getWriter(instructorUser, AssignmentWriterExtended.class).deleteAssignment(lmsCourseId, Long.parseLong(lmsAssignment.getId()));
        } catch (IOException | CanvasException e) {
            throw new ApiException(String.format("Failed to delete the LMS assignment with id [%s] from canvas course [%s]", lmsAssignment.getId(), lmsCourseId), e);
        }
    }

    @Override
    public AssignmentExtended uploadConsentFile(Experiment experiment, ConsentDocument consentDocument, LtiUserEntity instructorUser) throws ApiException, IOException {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();
        ExternalToolTagAttribute canvasExternalToolTagAttributes = assignmentExtended.getAssignment().new ExternalToolTagAttribute();
        canvasExternalToolTagAttributes.setUrl(
            ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(String.format("/lti3?consent=true&experiment=%s", experiment.getExperimentId()))
                .build()
                .toUriString());
        assignmentExtended.getAssignment().setExternalToolTagAttributes(canvasExternalToolTagAttributes);
        assignmentExtended.getAssignment().setName(consentDocument.getTitle());
        assignmentExtended.getAssignment().setDescription("You are being asked to participate in a research study. " +
                "Please read the statement below, and then select your response. " +
                "Your teacher will be able to see whether you submitted a response, but will not be able to see your selection.");
        assignmentExtended.getAssignment().setPublished(false);
        assignmentExtended.getAssignment().setGradingType("points");
        assignmentExtended.getAssignment().setPointsPossible(1.0);
        assignmentExtended.getAssignment().setSubmissionTypes(Collections.singletonList("external_tool"));

        try {
            String canvasCourseId = StringUtils.substringBetween(experiment.getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");

            return getWriter(instructorUser, AssignmentWriterExtended.class)
                .createAssignment(canvasCourseId, assignmentExtended.getAssignment())
                .orElseThrow(() -> new ApiException(String.format("Failed to create Assignment in Canvas course by ID [%s]", canvasCourseId)));
        } catch (ApiException e) {
            log.error("Creating the consent document with tile: [{}] failed", consentDocument.getTitle(), e);
            throw new ApiException("Error 137: The assignment was not created.", e);
        }
    }

    @Override
    public void resyncAssignmentTargetUrisInLms(PlatformDeployment platformDeployment, LtiUserEntity ltiUserEntity, long canvasCourseId, String tokenOverride, List<String> assignmentIds, List<String> consentAssignmentIds, List<String> allAssignmentIds)
        throws ApiException, TerracottaConnectorException {
        List<AssignmentExtended> assignmentExtendeds;

        try {
            // retrieve assignments for this course in the LMS
            assignmentExtendeds = listAssignmentExtendeds(ltiUserEntity, Long.toString(canvasCourseId), tokenOverride);
        } catch (ApiException e) {
            log.info("An error occurred updating assignments for Canvas course ID: [{}] for deployment ID: [{}]. Error: [{}]", canvasCourseId, platformDeployment.getKeyId(), e.getMessage());
            return;
        }

        if (CollectionUtils.isEmpty(assignmentExtendeds)) {
            log.info("No assignments exist in Canvas for course ID: [{}]", canvasCourseId);
            //return;
        }

        List<String> assignmentsUpdatedTargetLink = assignmentExtendeds.stream()
            .filter(assignmentExtended -> allAssignmentIds.contains(assignmentExtended.getId()))
            .filter(
                assignmentExtended -> {
                    String[] baseUrl = StringUtils.splitByWholeSeparator(assignmentExtended.getAssignment().getExternalToolTagAttributes().getUrl(), "/lti3");

                    return ArrayUtils.isNotEmpty(baseUrl) || !StringUtils.equalsIgnoreCase(baseUrl[0], platformDeployment.getLocalUrl());
                }
            )
            .map(
                assignmentToUpdate -> {
                    String updatedTargetLinkUri = StringUtils.replaceOnce(
                        assignmentToUpdate.getAssignment().getExternalToolTagAttributes().getUrl(),
                        StringUtils.splitByWholeSeparator(assignmentToUpdate.getAssignment().getExternalToolTagAttributes().getUrl(), "/lti3")[0],
                        platformDeployment.getLocalUrl()
                    );

                    log.info("Updating assignment ID: [{}] LTI Target Link URI in Canvas: from [{}] to [{}]",
                        assignmentToUpdate.getId(),
                        assignmentToUpdate.getAssignment().getExternalToolTagAttributes().getUrl(),
                        updatedTargetLinkUri
                    );

                    try {
                        assignmentToUpdate.getAssignment().getExternalToolTagAttributes().setUrl(updatedTargetLinkUri);
                        editAssignment(platformDeployment, assignmentToUpdate, assignmentToUpdate.getAssignment().getCourseId(), tokenOverride);
                    } catch (ApiException e) {
                        log.error("Error updating LTI Target Link URIs in the LMS. Assignment ID: [{}]. Error: [{}]", assignmentToUpdate.getId(), e.getMessage());
                    }

                    return assignmentToUpdate.getId();
                }
            )
            .toList();

        log.info("Updating Assignment Target Link URIs for the LMS course ID: [{}] for deployment ID: [{}] in the LMS COMPLETE. Updated: {}",
            canvasCourseId,
            platformDeployment.getKeyId(),
            CollectionUtils.isNotEmpty(assignmentsUpdatedTargetLink) ?
                assignmentsUpdatedTargetLink.stream()
                    .collect(Collectors.joining(", ")) :
                "N/A"
        );
    }

    @Override
    public List<LmsCourse> listCoursesForUser(PlatformDeployment platformDeployment, String canvasUserId, String tokenOverride) throws ApiException {
        try {
            return castList(
                getReader(platformDeployment.getBaseUrl(), CourseReaderExtended.class, tokenOverride)
                    .listCoursesForUser(new ListUserCoursesOptions(canvasUserId))
            );
        } catch (IOException | CanvasException e) {
            throw new ApiException(String.format("Failed to get the courses from canvas for user ID [%s]", canvasUserId), e);
        }
    }

    @Override
    public Optional<LmsCourse> editCourse(PlatformDeployment platformDeployment, LmsCourse lmsCourse, String canvasCourseId, String tokenOverride) throws ApiException {
        try {
            return castOptional(
                getWriter(platformDeployment.getBaseUrl(), CourseWriterExtended.class, tokenOverride)
                    .editCourse(canvasCourseId, CourseExtended.of(lmsCourse))
            );
        } catch (IOException | CanvasException e) {
            throw new ApiException(String.format("Failed to edit the course with ID [%s] in Canvas", canvasCourseId), e);
        }
    }

    @Override
    public List<LmsSubmission> listSubmissions(LtiUserEntity apiUser, String canvasAssignmentId, String canvasCourseId) throws ApiException, IOException {
        GetSubmissionsOptions submissionsOptions = new GetSubmissionsOptions(canvasCourseId, Long.parseLong(canvasAssignmentId));
        submissionsOptions.includes(Collections.singletonList(GetSubmissionsOptions.Include.USER));

        return castList(
            getReader(apiUser, SubmissionReaderExtended.class)
                .getCourseSubmissions(submissionsOptions)
        );
    }

    @Override
    public void addLmsExtensions(Score score, Submission submission, boolean studentSubmission) throws ApiException, IOException {
        Map<String, Object> submissionData = new HashMap<>();
        // See
        // https://canvas.instructure.com/doc/api/score.html#method.lti/ims/scores.create
        // for more information about these extension fields

        // Only treat a score as a new submission when it comes from a student and NOT
        // when graded by an instructor
        submissionData.put("new_submission", studentSubmission);

        // Include date originally submitted so that late grading doesn't result in late
        // submissions
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // ISO8601 format
        String dateSubmittedFormatted = dt.format(submission.getDateSubmitted());
        submissionData.put("submitted_at", dateSubmittedFormatted);

        score.setLmsSubmissionExtension(submissionData);
    }

    private List<AssignmentExtended> listAssignmentExtendeds(LtiUserEntity apiUser, String canvasCourseId, String tokenOverride) throws ApiException {
        try {
            return
                getReader(apiUser,AssignmentReaderExtended.class, tokenOverride)
                    .listCourseAssignments(new ListCourseAssignmentsOptions(canvasCourseId));
        } catch (IOException e) {
            throw new ApiException(String.format("Failed to get the list of assignments Canvas course by ID [%s]", canvasCourseId), e);
        }
    }

    private <T extends CanvasWriter<?, T>> T getWriter(LtiUserEntity apiUser, Class<T> clazz) throws ApiException {
        return getWriterInternal(apiUser, clazz, getOauthToken(apiUser));
    }

    private <T extends CanvasWriter<?, T>> T getWriter(String baseUrl, Class<T> clazz, String tokenOverride) throws ApiException {
        return getWriterInternal(baseUrl, clazz, getOauthToken(null, tokenOverride));
    }

    private <T extends CanvasReader<?, T>> T getReader(LtiUserEntity apiUser, Class<T> clazz) throws ApiException {
        return getReaderInternal(apiUser, clazz, getOauthToken(apiUser));
    }

    private <T extends CanvasReader<?, T>> T getReader(LtiUserEntity apiUser, Class<T> clazz, String tokenOverride) throws ApiException {
        return getReaderInternal(apiUser, clazz, getOauthToken(apiUser, tokenOverride));
    }

    private <T extends CanvasReader<?, T>> T getReader(String baseUrl, Class<T> clazz, String tokenOverride) throws ApiException {
        return getReaderInternal(baseUrl, clazz, getOauthToken(null, tokenOverride));
    }

    public <T extends CanvasReader<?, T>> T getReaderInternal(LtiUserEntity apiUser, Class<T> clazz, OauthToken oauthToken) {
        return getApiFactory(apiUser).getReader(clazz, oauthToken);
    }

    public <T extends CanvasReader<?, T>> T getReaderInternal(String baseUrl, Class<T> clazz, OauthToken oauthToken) {
        return getApiFactory(baseUrl).getReader(clazz, oauthToken);
    }

    public <T extends CanvasWriter<?, T>> T getWriterInternal(LtiUserEntity apiUser, Class<T> clazz, OauthToken oauthToken) {
        return getApiFactory(apiUser).getWriter(clazz, oauthToken);
    }

    public <T extends CanvasWriter<?, T>> T getWriterInternal(String baseUrl, Class<T> clazz, OauthToken oauthToken) {
        return getApiFactory(baseUrl).getWriter(clazz, oauthToken);
    }

    private CanvasApiFactoryExtended getApiFactory(LtiUserEntity apiUser) {
        return new CanvasApiFactoryExtended(apiUser.getPlatformDeployment().getBaseUrl());
    }

    private CanvasApiFactoryExtended getApiFactory(String baseUrl) {
        return new CanvasApiFactoryExtended(baseUrl);
    }

    private OauthToken getOauthToken(LtiUserEntity apiUser) throws ApiException {
        return new NonRefreshableOauthToken(getAccessToken(apiUser, null));
    }

    private OauthToken getOauthToken(LtiUserEntity apiUser, String tokenOverride) throws ApiException {
        return new NonRefreshableOauthToken(getAccessToken(apiUser, tokenOverride));
    }

    private String getAccessToken(LtiUserEntity apiUser, String tokenOverride) throws ApiException {
        if (tokenOverride != null) {
            if (tokenLoggingEnabled) {
                log.debug("Using API token override: [{}]", tokenOverride);
            }

            return tokenOverride;
        }

        String accessToken = null;

        if (canvasLmsOAuthService.isConfigured(apiUser.getPlatformDeployment())) {
            try {
                ApiTokenEntity canvasApiTokenEntity = canvasLmsOAuthService.getAccessToken(apiUser);

                if (tokenLoggingEnabled) {
                    log.debug("Using access token for user {}", apiUser.getUserKey());
                }

                accessToken = canvasApiTokenEntity.getAccessToken();
            } catch (LmsOAuthException e) {
                throw new ApiException(String.format("Could not get a Canvas API token for user [%s]", apiUser.getUserKey()), e);
            }
        } else if (apiUser.getPlatformDeployment().getApiToken() != null) {
            if (tokenLoggingEnabled) {
                log.debug("Using admin api token configured for platform deployment {}", apiUser.getPlatformDeployment().getKeyId());
            }

            accessToken = apiUser.getPlatformDeployment().getApiToken();
        } else {
            throw new ApiException(
                String.format(
                    "Could not get a Canvas API token for platform deployment [%s] and user [%s]",
                    apiUser.getPlatformDeployment().getKeyId(),
                    apiUser.getUserKey()
                )
            );
        }

        return accessToken;
    }

    private <T> Optional<T> castOptional(Optional<? extends LmsEntity<T>> extended) {
        return Optional.of(extended.get().from());
    }

    private <T> List<T> castList(List<? extends LmsEntity<T>> extendeds) {
        return extendeds.stream()
            .map(extended -> extended.from())
            .toList();
    }

    /*@Override
    public List<Submission> listSubmissionsForGivenUser(LtiUserEntity apiUser, Integer assignmentId,
            String canvasCourseId, String canvasUserId) throws ApiException, IOException {
        PlatformDeployment platformDeployment = apiUser.getPlatformDeployment();
        String canvasBaseUrl = platformDeployment.getBaseUrl();
        String accessToken = getAccessToken(apiUser);
        OauthToken oauthToken = new NonRefreshableOauthToken(accessToken);
        CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
        SubmissionImpl submissionReader = apiFactory.getReader(SubmissionImpl.class, oauthToken);
        GetSubmissionsOptions submissionsOptions = new GetSubmissionsOptions(canvasCourseId, assignmentId);
        submissionsOptions.includes(Collections.singletonList(GetSubmissionsOptions.Include.USER));
        return submissionReader.getCourseSubmissions(submissionsOptions);
    }

    @Override
    public Optional<Progress> postSubmission(LtiUserEntity apiUser, edu.iu.terracotta.model.app.Submission submission,
            Float maxTerracottaScore)
            throws ApiException, IOException {

        String canvasCourseId = getCanvasCourseId(submission.getParticipant().getLtiMembershipEntity().getContext().getContext_memberships_url());
        int assignmentId = Integer.parseInt(submission.getAssessment().getTreatment().getAssignment().getLmsAssignmentId());
        String canvasUserId = submission.getParticipant().getLtiUserEntity().getLmsUserId();
        Optional<AssignmentExtended> assignmentExtended = listAssignment(apiUser, canvasCourseId, assignmentId);
        if (assignmentExtended.isEmpty()) {
            throw new ApiException(
                    "Failed to get the assignments with id [" + assignmentId + "] from canvas course [" + canvasCourseId + "]");
        }
        Double maxCanvasScore = assignmentExtended.get().getPointsPossible();

        Double grade = Double.valueOf("0");
        if (submission.getTotalAlteredGrade() != null) {
            grade = Double.parseDouble(submission.getTotalAlteredGrade().toString());
        } else if (submission.getAlteredCalculatedGrade() != null) {
            grade = Double.parseDouble(submission.getAlteredCalculatedGrade().toString());
        } else {
            grade = Double.parseDouble(submission.getCalculatedGrade().toString());
        }
        grade = grade * maxCanvasScore / Double.parseDouble(maxTerracottaScore.toString());

        return postGrade(apiUser, canvasCourseId, assignmentId, canvasUserId, grade);
    }

    @Override
    public Optional<Progress> postConsentSubmission(LtiUserEntity apiUser, Participant participant)
            throws ApiException, IOException {
        String canvasCourseId = getCanvasCourseId(participant.getLtiMembershipEntity().getContext().getContext_memberships_url());
        int assignmentId = Integer.parseInt(participant.getExperiment().getConsentDocument().getLmsAssignmentId());
        String canvasUserId = participant.getLtiUserEntity().getLmsUserId();
        Optional<AssignmentExtended> assignmentExtended = listAssignment(apiUser, canvasCourseId, assignmentId);
        if (assignmentExtended.isEmpty()) {
            throw new ApiException(
                    "Failed to get the assignments with id [" + assignmentId + "] from canvas course [" + canvasCourseId + "]");
        }

        Double grade = Double.valueOf("1.0");

        return postGrade(apiUser, canvasCourseId, assignmentId, canvasUserId, grade);
    }

    private Optional<Progress> postGrade(LtiUserEntity apiUser, String canvasCourseId, int assignmentId,
            String canvasUserId, Double grade) throws IOException, ApiException {
        PlatformDeployment platformDeployment = apiUser.getPlatformDeployment();
        String canvasBaseUrl = platformDeployment.getBaseUrl();
        String accessToken = getAccessToken(apiUser);
        OauthToken oauthToken = new NonRefreshableOauthToken(accessToken);
        CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
        SubmissionWriter submissionWriter = apiFactory.getWriter(SubmissionWriter.class, oauthToken);
        MultipleSubmissionsOptions multipleSubmissionsOptions = new MultipleSubmissionsOptions(canvasCourseId, assignmentId, new HashMap<>());
        MultipleSubmissionsOptions.StudentSubmissionOption submissionsOptions =
                multipleSubmissionsOptions.createStudentSubmissionOption(
                        null,
                        grade.toString(),
                        false,
                        false,
                        null,
                        null
                );
        HashMap<String, MultipleSubmissionsOptions.StudentSubmissionOption> submissionOptionHashMap = new HashMap<>();
        submissionOptionHashMap.put(canvasUserId, submissionsOptions);
        multipleSubmissionsOptions.setStudentSubmissionOptionMap(submissionOptionHashMap);
        submissionWriter.gradeMultipleSubmissionsByCourse(multipleSubmissionsOptions);
        return submissionWriter.gradeMultipleSubmissionsByCourse(multipleSubmissionsOptions);
    }*/

}
