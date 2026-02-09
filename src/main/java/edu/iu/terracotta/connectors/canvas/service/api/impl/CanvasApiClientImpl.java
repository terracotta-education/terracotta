package edu.iu.terracotta.connectors.canvas.service.api.impl;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.AssignmentExtended;
import edu.iu.terracotta.connectors.canvas.dao.model.extended.FolderExtended;
import edu.iu.terracotta.connectors.canvas.dao.model.extended.options.GetSubmissionsOptionsExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.AssignmentReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.AssignmentWriterExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.ConversationReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.ConversationWriterExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.CourseReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.FileReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.FolderReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.SubmissionReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.UserReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.impl.CanvasApiFactoryExtended;
import edu.iu.terracotta.connectors.canvas.service.lms.impl.CanvasLmsOAuthServiceImpl;
import edu.iu.terracotta.connectors.canvas.service.lms.impl.CanvasLmsUtilsImpl;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiTokenEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsConversation;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFile;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;
import edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsEntity;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsCreateConversationOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetSingleConversationOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetUsersInCourseOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Outcome;
import edu.iu.terracotta.dao.entity.Submission;
import edu.ksu.canvas.exception.CanvasException;
import edu.ksu.canvas.exception.ObjectNotFoundException;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.interfaces.CanvasWriter;
import edu.ksu.canvas.model.assignment.Assignment.ExternalToolTagAttribute;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.CreateConversationOptions;
import edu.ksu.canvas.requestOptions.GetSingleAssignmentOptions;
import edu.ksu.canvas.requestOptions.GetSingleConversationOptions;
import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;
import edu.ksu.canvas.requestOptions.GetUsersInCourseOptions;
import edu.ksu.canvas.requestOptions.ListCourseAssignmentsOptions;
import edu.ksu.canvas.requestOptions.ListUserCoursesOptions;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@TerracottaConnector(LmsConnector.CANVAS)
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LambdaCanBeMethodReference", "PMD.UnusedPrivateMethod", "PMD.LooseCoupling"})
public class CanvasApiClientImpl implements ApiClient {

    @Autowired private CanvasLmsOAuthServiceImpl canvasLmsOAuthService;
    @Autowired private CanvasLmsUtilsImpl canvasLmsUtils;

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
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to create Assignment in Canvas course by ID [%s]", canvasCourseId), e);
        }
    }

    @Override
    public AssignmentExtended restoreAssignment(Assignment assignment) throws ApiException, IOException, TerracottaConnectorException {
        // create the new Assignment in Canvas
        String canvasCourseId = canvasLmsUtils.parseCourseId(
            assignment.getExposure().getExperiment().getPlatformDeployment(),
            assignment.getExposure().getExperiment().getLtiContextEntity().getContext_memberships_url()
        );

        return createLmsAssignment(assignment.getExposure().getExperiment().getCreatedBy(), assignment, canvasCourseId);
    }

    @Override
    public List<LmsAssignment> listAssignments(LtiUserEntity apiUser, LtiContextEntity ltiContext) throws ApiException, TerracottaConnectorException {
        String canvasCourseId = canvasLmsUtils.parseCourseId(
            ltiContext.getToolDeployment().getPlatformDeployment(),
            ltiContext.getContext_memberships_url()
        );

        try {
            return castList(
                getReader(apiUser,AssignmentReaderExtended.class)
                    .listCourseAssignments(new ListCourseAssignmentsOptions(canvasCourseId))
            );
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to get the list of assignments Canvas course by ID [%s]", canvasCourseId), e);
        }
    }

    @Override
    public List<LmsAssignment> listAssignments(LtiUserEntity apiUser, Experiment experiment) throws ApiException, TerracottaConnectorException {
        String canvasCourseId = canvasLmsUtils.parseCourseId(
            experiment.getPlatformDeployment(),
            experiment.getLtiContextEntity().getContext_memberships_url()
        );

        try {
            return castList(
                getReader(apiUser,AssignmentReaderExtended.class)
                    .listCourseAssignments(new ListCourseAssignmentsOptions(canvasCourseId))
            );
        } catch (Exception e) {
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
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to get the list of assignments Canvas course [%s]", canvasCourseId), e);
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
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to get the assignments with id [%s] from canvas course [%s]", canvasAssignmentId, canvasCourseId), e);
        }
    }

    @Override
    public Optional<LmsAssignment> listAssignment(LtiUserEntity apiUser, String lmsCourseId, Assignment assignment) throws ApiException, TerracottaConnectorException {
        return listAssignment(apiUser, lmsCourseId, assignment.getLmsAssignmentId());
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
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to get the Assignment in Canvas course by ID [%s]", canvasCourseId), e);
        }
    }

    @Override
    public Optional<LmsAssignment> editAssignment(LtiUserEntity apiUser, LmsAssignment lmsAssignment, String canvasCourseId) throws ApiException {
        try {
            return castOptional(
                getWriter(apiUser, AssignmentWriterExtended.class)
                    .editAssignment(canvasCourseId, AssignmentExtended.of(lmsAssignment).getAssignment())
            );
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to edit the assignments with id [%s] from canvas course [%s]", lmsAssignment.getId(), canvasCourseId), e);
        }
    }

    @Override
    public Optional<LmsAssignment> editAssignment(PlatformDeployment platformDeployment, LmsAssignment lmsAssignment, String canvasCourseId, String tokenOverride) throws ApiException {
        try {
            return castOptional(
                getWriter(platformDeployment.getBaseUrl(), AssignmentWriterExtended.class, tokenOverride)
                    .editAssignment(canvasCourseId, AssignmentExtended.of(lmsAssignment).getAssignment())
            );
        } catch (Exception e) {
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
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to delete the LMS assignment with id [%s] from canvas course [%s]", lmsAssignment.getId(), lmsCourseId), e);
        }
    }

    @Override
    public AssignmentExtended uploadConsentFile(Experiment experiment, ConsentDocument consentDocument, LtiUserEntity instructorUser) throws ApiException, IOException, TerracottaConnectorException {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();
        ExternalToolTagAttribute canvasExternalToolTagAttributes = assignmentExtended.getAssignment().new ExternalToolTagAttribute();
        String consentPath = String.format("/lti3?consent=true&experiment=%s", experiment.getExperimentId());
        String url = null;

        try {
            // build URL from context
            url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(consentPath)
                .build()
                .toUriString();
        } catch (Exception e) {
            // build URL from platform deployment
            url = String.format("%s%s", instructorUser.getPlatformDeployment().getLocalUrl(), consentPath);
        }

        canvasExternalToolTagAttributes.setUrl(url);
        assignmentExtended.getAssignment().setExternalToolTagAttributes(canvasExternalToolTagAttributes);
        assignmentExtended.getAssignment().setName(consentDocument.getTitle());
        assignmentExtended.getAssignment().setDescription(StringUtils.EMPTY);
        assignmentExtended.getAssignment().setPublished(false);
        assignmentExtended.getAssignment().setGradingType("points");
        assignmentExtended.getAssignment().setPointsPossible(1.0);
        assignmentExtended.getAssignment().setSubmissionTypes(Collections.singletonList("external_tool"));

        try {
            String canvasCourseId = canvasLmsUtils.parseCourseId(
                experiment.getPlatformDeployment(),
                experiment.getLtiContextEntity().getContext_memberships_url()
            );

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

                    return ArrayUtils.isNotEmpty(baseUrl) || !Strings.CI.equals(baseUrl[0], platformDeployment.getLocalUrl());
                }
            )
            .map(
                assignmentToUpdate -> {
                    String updatedTargetLinkUri = Strings.CS.replaceOnce(
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
    public void updateAssignmentMetadata(Assignment assignment, LmsAssignment lmsAssignment) throws TerracottaConnectorException {
        // unused by Canvas
    }

    @Override
    public List<LmsCourse> listCoursesForUser(PlatformDeployment platformDeployment, String canvasUserId, String tokenOverride) throws ApiException {
        try {
            return castList(
                getReader(platformDeployment.getBaseUrl(), CourseReaderExtended.class, tokenOverride)
                    .listCoursesForUser(new ListUserCoursesOptions(canvasUserId))
            );
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to get the courses from canvas for user ID [%s]", canvasUserId), e);
        }
    }

    @Override
    public List<LmsSubmission> listSubmissions(LtiUserEntity apiUser, Outcome outcome, String canvasCourseId) throws ApiException, IOException, TerracottaConnectorException {
        return listSubmissions(apiUser, outcome.getLmsOutcomeId(), canvasCourseId);
    }

    @Override
    public List<LmsSubmission> listSubmissions(LtiUserEntity apiUser, String canvasAssignmentId, String canvasCourseId) throws ApiException, IOException, TerracottaConnectorException {
        GetSubmissionsOptions submissionsOptions = new GetSubmissionsOptions(canvasCourseId, Long.parseLong(canvasAssignmentId));
        submissionsOptions.includes(List.of(GetSubmissionsOptions.Include.USER));
        submissionsOptions.userIds(List.of(GetSubmissionsOptionsExtended.UserId.ALL.toString()));

        try {
            return castList(
                getReader(apiUser, SubmissionReaderExtended.class)
                    .getCourseSubmissions(submissionsOptions)
            );
        } catch (Exception e) {
            throw new ApiException(
                String.format(
                    "Failed to list submissions for the assignment with ID: [%s] in the course with ID: [%s] in Canvas",
                    canvasAssignmentId,
                    canvasCourseId
                ),
                e
            );
        }
    }

    @Override
    public List<LmsSubmission> listSubmissionsForMultipleAssignments(LtiUserEntity apiUser, String canvasCourseId, List<String> canvasAssignmentIds) throws ApiException, IOException, TerracottaConnectorException {
        GetSubmissionsOptionsExtended submissionsOptions = new GetSubmissionsOptionsExtended(canvasCourseId, canvasAssignmentIds);
        submissionsOptions.includes(Collections.singletonList(GetSubmissionsOptions.Include.USER));

        try {
            return castList(
                getReader(apiUser, SubmissionReaderExtended.class)
                    .listSubmissionsForMultipleAssignments(submissionsOptions)
            );
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to list submissions for the assignments with IDs: [%s] in the course with ID: [%s] in Canvas", String.join(",", canvasAssignmentIds), canvasCourseId), e);
        }
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

    @Override
    public List<LmsConversation> sendConversation(LmsCreateConversationOptions lmsCreateConversationOptions, LtiUserEntity apiUser) throws ApiException {
        CreateConversationOptions createConversationOptions = new CreateConversationOptions(
            lmsCreateConversationOptions.getLmsUserId(),
            lmsCreateConversationOptions.getBody()
        );
        createConversationOptions.attachmentIds(lmsCreateConversationOptions.getAttachmentIds().stream().map(Long::parseLong).collect(Collectors.toList()));
        createConversationOptions.forceNew(lmsCreateConversationOptions.isForceNew());
        createConversationOptions.groupConversation(lmsCreateConversationOptions.isGroupConversation());
        createConversationOptions.subject(lmsCreateConversationOptions.getSubject());

        try {
            return castList(getWriter(apiUser, ConversationWriterExtended.class).createConversation(createConversationOptions));
        } catch (IOException | CanvasException ex) {
            throw new ApiException(
                String.format("Failed to send a conversation from Canvas user ID: [%s] to Canvas user ID: [%s] in Canvas",
                    apiUser.getLmsUserId(),
                    StringUtils.join(createConversationOptions.getOptionsMap().get("recipients[]"), ',')
                ),
                ex
            );
        }
    }

    @Override
    public Optional<LmsConversation> getConversation(LmsGetSingleConversationOptions lmsGetSingleConversationOptions, LtiUserEntity apiUser) throws ApiException {
        GetSingleConversationOptions getSingleConversationOptions = new GetSingleConversationOptions(Long.parseLong(lmsGetSingleConversationOptions.getConversationId()));
        getSingleConversationOptions.autoMarkAsRead(lmsGetSingleConversationOptions.isAutoMarkAsRead());

        try {
            return castOptional(getReader(apiUser, ConversationReaderExtended.class).getSingleConversation(getSingleConversationOptions));
        } catch (IOException | CanvasException ex) {
            throw new ApiException(String.format("Failed to get conversation with ID: [%s] for Canvas user ID: [%s]", getSingleConversationOptions.getConversationId(), apiUser.getLmsUserId()), ex);
        }
    }

    @Override
    public List<LmsUser> listUsersForCourse(LmsGetUsersInCourseOptions lmsGetUsersInCourseOptions, LtiUserEntity apiUser) throws ApiException {
        List<GetUsersInCourseOptions.EnrollmentState> enrollmentStates = lmsGetUsersInCourseOptions.getEnrollmentState().stream()
            .map(enrollmentState -> EnumUtils.getEnumIgnoreCase(GetUsersInCourseOptions.EnrollmentState.class, enrollmentState.toString(), GetUsersInCourseOptions.EnrollmentState.ACTIVE))
            .toList();
        List<GetUsersInCourseOptions.EnrollmentType> enrollmentTypes = lmsGetUsersInCourseOptions.getEnrollmentType().stream()
            .map(enrollmentType -> EnumUtils.getEnumIgnoreCase(GetUsersInCourseOptions.EnrollmentType.class, enrollmentType.toString(), GetUsersInCourseOptions.EnrollmentType.STUDENT))
            .toList();
        GetUsersInCourseOptions getUsersInCourseOptions = new GetUsersInCourseOptions(lmsGetUsersInCourseOptions.getLmsCourseId());
        getUsersInCourseOptions.enrollmentState(enrollmentStates);
        getUsersInCourseOptions.enrollmentType(enrollmentTypes);

        try {
            return castList(getReader(apiUser, UserReaderExtended.class).getUsersInCourse(getUsersInCourseOptions));
        } catch (IOException | CanvasException ex) {
            throw new ApiException(String.format("Failed to get the list of users for Canvas course ID: [%s] for Canvas user ID: [%s]", getUsersInCourseOptions.getCourseId(), apiUser.getLmsUserId()), ex);
        }
    }

    @Override
    public Optional<LmsFile> getFile(LtiUserEntity apiUser, String canvasFileId) throws ApiException {
        try {
            return castOptional(
                getReader(
                    apiUser,
                    FileReaderExtended.class
                )
                .getFile(
                    String.format(
                "file/%s",
                        canvasFileId
                    )
                )
            );
        } catch (IOException | CanvasException e) {
            throw new ApiException(String.format("Failed to get for with Canvas file ID: [%s] for Canvas user ID: [%s]", canvasFileId, apiUser.getLmsUserId()), e);
        }
    }

    @Override
    public List<LmsFile> getFiles(LtiUserEntity apiUser) throws ApiException {
        try {
            List<FolderExtended> folders = getReader(apiUser, FolderReaderExtended.class).getFolders();

            if (CollectionUtils.isEmpty(folders)) {
                log.warn("No folders found for Canvas user ID: [{}]", apiUser.getLmsUserId());
                throw new ApiException(String.format("No folders found for Canvas user ID: [%s]", apiUser.getLmsUserId()));
            }

            // ind "my files/conversation attachments" folder if exists
            FolderExtended conversationAttachmentsFolder = folders.stream()
                .filter(folder -> Strings.CI.equals(folder.getFullName(), "my files/conversation attachments"))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                    String.format("The 'my files/conversation attachments' folder was not found for Canvas user ID: [%s]", apiUser.getLmsUserId())
                ));

            return castList(
                getReader(
                    apiUser,
                    FileReaderExtended.class
                )
                .getFiles(
                    String.format(
                        "folders/%s/files",
                        conversationAttachmentsFolder.getId()
                    )
                )
            );
        } catch (IOException | CanvasException e) {
            throw new ApiException(String.format("Failed to get files for Canvas user ID: [%s]", apiUser.getLmsUserId()), e);
        }
    }

    private List<AssignmentExtended> listAssignmentExtendeds(LtiUserEntity apiUser, String canvasCourseId, String tokenOverride) throws ApiException {
        try {
            return
                getReader(apiUser,AssignmentReaderExtended.class, tokenOverride)
                    .listCourseAssignments(new ListCourseAssignmentsOptions(canvasCourseId));
        } catch (Exception e) {
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
        try {
            return Optional.of(extended.get().from());
        } catch (Exception e) {
            log.error("Error casting extended entity to optional entity", e);
            return Optional.empty();
        }

    }

    private <T> List<T> castList(List<? extends LmsEntity<T>> extendeds) {
        try {
            return extendeds.stream()
                .map(
                    extended -> {
                        try {
                            return extended.from();
                        } catch (Exception e) {
                            log.error("Error casting extended LMS entity", e);
                            return null;
                        }
                    }
                )
                .filter(Objects::nonNull)
                .toList();
        } catch (Exception e) {
            log.error("Error casting extended LMS entity list", e);
            return Collections.emptyList();
        }
    }

}
