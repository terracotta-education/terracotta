package edu.iu.terracotta.connectors.brightspace.service.api.impl;

import edu.iu.terracotta.connectors.brightspace.dao.model.extended.AssignmentExtended;
import edu.iu.terracotta.connectors.brightspace.dao.model.extended.SubmissionExtended;
import edu.iu.terracotta.connectors.brightspace.io.exception.ObjectNotFoundException;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.AssignmentReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.AssignmentWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ClasslistUserReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectModuleReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectTopicReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.GradeObjectReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageLinkReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.UserGradeValueReaderService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.Assessment;
import edu.iu.terracotta.connectors.brightspace.io.model.Availability;
import edu.iu.terracotta.connectors.brightspace.io.model.BrightspaceAssignmentMetadata;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectModule;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectTopic;
import edu.iu.terracotta.connectors.brightspace.io.model.CustomParameter;
import edu.iu.terracotta.connectors.brightspace.io.model.GradeObject;
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageLink;
import edu.iu.terracotta.connectors.brightspace.io.model.UserGradeValue;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.api.BrightspaceUrl;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.content.ContentObjectType;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.content.TopicType;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.dropbox.CompletionType;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.dropbox.DropboxType;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.dropbox.SubmissionType;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.ltiadvantage.LinkType;
import edu.iu.terracotta.connectors.brightspace.io.oauth.NonRefreshableOauthToken;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import edu.iu.terracotta.connectors.brightspace.service.io.impl.BrightspaceApiFactory;
import edu.iu.terracotta.connectors.brightspace.service.lms.impl.BrightspaceLmsOAuthServiceImpl;
import edu.iu.terracotta.connectors.brightspace.service.lms.impl.BrightspaceLmsUtilsImpl;
import edu.iu.terracotta.connectors.brightspace.service.lti.advantage.impl.BrightspaceAdvantageAgsServiceImpl;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
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
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentType;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Outcome;
import edu.iu.terracotta.dao.entity.Submission;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@TerracottaConnector(LmsConnector.BRIGHTSPACE)
@SuppressWarnings({"unchecked", "PMD.GuardLogStatement", "PMD.LambdaCanBeMethodReference", "PMD.UnusedPrivateMethod", "PMD.LooseCoupling"})
public class BrightspaceApiClientImpl implements ApiClient {

    @Autowired private BrightspaceAdvantageAgsServiceImpl brightspaceAdvantageAgsService;
    @Autowired private BrightspaceLmsOAuthServiceImpl brightspaceLmsOAuthService;
    @Autowired private BrightspaceLmsUtilsImpl brightspaceLmsUtils;

    @Value("${app.token.logging.enabled:true}")
    private boolean tokenLoggingEnabled;

    @Value("${brightspace.api.lp.version:1.54}")
    private String lpApiVersion;

    @Value("${brightspace.api.le.version:1.89}")
    private String leApiVersion;

    private ApiVersion apiVersion;

    @PostConstruct
    public void init() {
        apiVersion = ApiVersion.builder()
            .lp(lpApiVersion)
            .le(leApiVersion)
            .build();
    }

    @Override
    public AssignmentExtended createLmsAssignment(LtiUserEntity apiUser, Assignment assignment, String orgSourcedId) throws ApiException {
        String orgUnitId = assignment.getExposure().getExperiment().getLtiContextEntity().getContextKey();

        try {
            String deploymentId = brightspaceLmsUtils.parseDeploymentId(
                null,
                assignment.getExposure().getExperiment().getLtiContextEntity().getContext_memberships_url()
            );
            AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

            /* dropbox folder update */

            assignmentExtended.getAssignment().getDropboxFolderUpdate().setAllowOnlyUsersWithSpecialAccess(false);
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setIsAnonymous(false);
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setAssessment(
                Assessment.builder()
                    .scoreDenominator(100F)
                    .build()
            );
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setAvailability(
                Availability.builder()
                    .startDate(null)
                    .endDate(null)
                    .startDateAvailabilityType(null)
                    .endDateAvailabilityType(null)
                    .build()
            );
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setCompletionType(CompletionType.OnSubmission.key());
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setDisplayInCalendar(false);
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setDropboxType(DropboxType.Individual.key());
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setDueDate(parseDate(assignment.getDueDate()));
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setIsHidden(!assignment.isPublished());
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setName(assignment.getTitle());
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setSubmissionType(SubmissionType.Text.key());

            /* content object module */

            assignmentExtended.getAssignment().getContentObjectModuleUpdate().setIsHidden(!assignment.isPublished());
            assignmentExtended.getAssignment().getContentObjectModuleUpdate().setIsLocked(false);
            assignmentExtended.getAssignment().getContentObjectModuleUpdate().setModuleDueDate(parseDate(assignment.getDueDate()));
            assignmentExtended.getAssignment().getContentObjectModuleUpdate().setTitle(assignment.getTitle());
            assignmentExtended.getAssignment().getContentObjectModuleUpdate().setType(ContentObjectType.Module.key());

            /* content object topic for LTI link */

            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setDueDate(parseDate(assignment.getDueDate()));
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setIsHidden(!assignment.isPublished());
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setIsLocked(false);
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setMajorUpdate(false);
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setOpenAsExternalResource(false);
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setResetCompletionTracking(false);
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setTitle(assignment.getTitle());
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setTopicType(TopicType.Link.key());
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setType(ContentObjectType.Topic.key());

            /* lti advantage link */

            List<CustomParameter> customParameters = new ArrayList<>();
            customParameters.add(
                CustomParameter.builder()
                    .name(CustomParameter.Keys.ASSIGNMENT_ID.key())
                    .value(String.valueOf(assignment.getAssignmentId()))
                    .build()
            );
            customParameters.add(
                CustomParameter.builder()
                    .name(CustomParameter.Keys.EXPERIMENT_ID.key())
                    .value(String.valueOf(assignment.getExposure().getExperiment().getExperimentId()))
                    .build()
            );

            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setCustomParameters(customParameters);
            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setDeploymentId(deploymentId);
            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setIsEnabled(true);
            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setName(assignment.getTitle());
            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setType(LinkType.Basic.key());
            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setUrl(
                String.format(
                    BrightspaceUrl.LTI_ASSIGNMENT_LAUNCH.url(),
                    apiUser.getPlatformDeployment().getLocalUrl(),
                    assignment.getExposure().getExperiment().getExperimentId(),
                    assignment.getAssignmentId()
                )
            );

            /* lti lineitem */

            createLineItem(assignmentExtended, assignment, orgUnitId);

            return getWriter(apiUser, AssignmentWriterService.class)
                .createAssignment(orgUnitId, assignmentExtended.getAssignment())
                .orElseThrow(() -> new ApiException(String.format("Failed to create Assignment in Brightspace course for orgUnitId [%s]", orgUnitId)));
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to create Assignment in Brightspace course for orgUnitId [%s]", orgUnitId), e);
        }
    }

    @Override
    public AssignmentExtended restoreAssignment(Assignment assignment) throws ApiException, IOException {
        String orgUnitId = assignment.getExposure().getExperiment().getLtiContextEntity().getContextKey();

        try {
            String deploymentId = brightspaceLmsUtils.parseDeploymentId(
                assignment.getExposure().getExperiment().getLtiContextEntity().getToolDeployment().getPlatformDeployment(),
                assignment.getExposure().getExperiment().getLtiContextEntity().getContext_memberships_url()
            );
            AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();
            Map<String, Map<String, Object>> metadata = JsonMapper.builder()
                .build()
                .readValue(
                    assignment.getMetadata(),
                    new TypeReference<Map<String, Map<String, Object>>>() {}
                );
            BrightspaceAssignmentMetadata brightspaceAssignmentMetadata = JsonMapper.builder()
                .build()
                .convertValue(
                    metadata.get(BrightspaceAssignmentMetadata.KEY),
                    BrightspaceAssignmentMetadata.class
                );

            /* dropbox folder update */

            assignmentExtended.getAssignment().getDropboxFolderUpdate().setAllowOnlyUsersWithSpecialAccess(false);
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setIsAnonymous(false);
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setAssessment(
                Assessment.builder()
                    .scoreDenominator(100F)
                    .build()
            );
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setAvailability(
                Availability.builder()
                    .startDate(null)
                    .endDate(null)
                    .startDateAvailabilityType(null)
                    .endDateAvailabilityType(null)
                    .build()
            );
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setCompletionType(CompletionType.OnSubmission.key());
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setDisplayInCalendar(false);
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setDropboxType(DropboxType.Individual.key());
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setDueDate(parseDate(assignment.getDueDate()));
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setIsHidden(!assignment.isPublished());
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setName(assignment.getTitle());
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setSubmissionType(SubmissionType.Text.key());

            /* content object topic for LTI link */

            Optional<ContentObjectTopic> contentObjectTopic = getReader(assignment.getExposure().getExperiment().getCreatedBy(), ContentObjectTopicReaderService.class)
                .get(orgUnitId, brightspaceAssignmentMetadata.getContentTopicId());

            if (contentObjectTopic.isPresent()) {
                assignmentExtended.getAssignment().setContentObjectTopicLtiLink(contentObjectTopic.get());
            } else {
                assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setDueDate(parseDate(assignment.getDueDate()));
                assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setIsHidden(!assignment.isPublished());
                assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setIsLocked(false);
                assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setMajorUpdate(false);
                assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setOpenAsExternalResource(false);
                assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setResetCompletionTracking(false);
                assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setTitle(assignment.getTitle());
                assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setTopicType(TopicType.Link.key());
                assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setType(ContentObjectType.Topic.key());

                /*
                 NOTE: we only need to process the content object module if a topic does not already exist
                 as an existing topic is already associated with another module
                 */

                /* content object module */

                Optional<ContentObjectModule> contentObjectModule = getReader(assignment.getExposure().getExperiment().getCreatedBy(), ContentObjectModuleReaderService.class)
                    .get(orgUnitId, brightspaceAssignmentMetadata.getContentModuleId());

                if (contentObjectModule.isPresent()) {
                    assignmentExtended.getAssignment().setContentObjectModule(contentObjectModule.get());
                } else {
                    assignmentExtended.getAssignment().getContentObjectModuleUpdate().setIsHidden(!assignment.isPublished());
                    assignmentExtended.getAssignment().getContentObjectModuleUpdate().setIsLocked(false);
                    assignmentExtended.getAssignment().getContentObjectModuleUpdate().setModuleDueDate(parseDate(assignment.getDueDate()));
                    assignmentExtended.getAssignment().getContentObjectModuleUpdate().setTitle(assignment.getTitle());
                    assignmentExtended.getAssignment().getContentObjectModuleUpdate().setType(ContentObjectType.Module.key());
                }
            }

            /* lti advantage link */

            Optional<LtiAdvantageLink> ltiAdvantageLink = getReader(assignment.getExposure().getExperiment().getCreatedBy(), LtiAdvantageLinkReaderService.class)
                .get(orgUnitId, brightspaceAssignmentMetadata.getLtiAdvantageLinkId());

            if (ltiAdvantageLink.isPresent()) {
                assignmentExtended.getAssignment().setLtiAdvantageLink(ltiAdvantageLink.get());
            } else {
                List<CustomParameter> customParameters = new ArrayList<>();
                customParameters.add(
                    CustomParameter.builder()
                        .name(CustomParameter.Keys.ASSIGNMENT_ID.key())
                        .value(String.valueOf(assignment.getAssignmentId()))
                        .build()
                );
                customParameters.add(
                    CustomParameter.builder()
                        .name(CustomParameter.Keys.EXPERIMENT_ID.key())
                        .value(String.valueOf(assignment.getExposure().getExperiment().getExperimentId()))
                        .build()
                );

                assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setCustomParameters(customParameters);
                assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setDeploymentId(deploymentId);
                assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setIsEnabled(true);
                assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setName(assignment.getTitle());
                assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setType(LinkType.Basic.key());
                assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setUrl(
                    String.format(
                        BrightspaceUrl.LTI_ASSIGNMENT_LAUNCH.url(),
                        assignment.getExposure().getExperiment().getPlatformDeployment().getLocalUrl(),
                        assignment.getExposure().getExperiment().getExperimentId(),
                        assignment.getAssignmentId()
                    )
                );
            }

            /* lti lineitem */

            Optional<GradeObject> gradeObject = getReader(assignment.getExposure().getExperiment().getCreatedBy(), GradeObjectReaderService.class)
                .get(orgUnitId, brightspaceAssignmentMetadata.getGradeObjectId());

            if (gradeObject.isPresent()) {
                assignmentExtended.getAssignment().setGradeObject(gradeObject.get());
                getLineItem(assignmentExtended, assignment);
            } else {
                createLineItem(assignmentExtended, assignment, orgUnitId);
            }

            return getWriter(assignment.getExposure().getExperiment().getCreatedBy(), AssignmentWriterService.class)
                .createAssignment(orgUnitId, assignmentExtended.getAssignment())
                .orElseThrow(() -> new ApiException(String.format("Failed to create Assignment in Brightspace course by orgUnitId [%s]", orgUnitId)));
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to create Assignment in Brightspace course by orgUnitId [%s]", orgUnitId), e);
        }
    }

    @Override
    public List<LmsAssignment> listAssignments(LtiUserEntity apiUser, LtiContextEntity ltiContext) throws ApiException, TerracottaConnectorException {
        String orgUnitId = ltiContext.getContextKey();

        try {
            return castList(
                getReader(apiUser, AssignmentReaderService.class)
                    .listCourseAssignments(orgUnitId)
            );
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to get the list of assignments in Brightspace course by orgUnitId [%s]", orgUnitId), e);
        }
    }

    @Override
    public List<LmsAssignment> listAssignments(LtiUserEntity apiUser, Experiment experiment) throws ApiException {
        String orgUnitId = experiment.getLtiContextEntity().getContextKey();

        try {
            return castList(
                getReader(apiUser,AssignmentReaderService.class)
                    .listCourseAssignments(orgUnitId)
            );
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to get the list of assignments in Brightspace course by orgUnitId [%s]", orgUnitId), e);
        }
    }

    @Override
    public List<LmsAssignment> listAssignments(PlatformDeployment platformDeployment, String orgUnitId, String tokenOverride) throws ApiException {
        try {
            return castList(
                getReader(platformDeployment.getBaseUrl(), AssignmentReaderService.class, tokenOverride)
                    .listCourseAssignments(orgUnitId)
            );
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to get the list of assignments in Brightspace course by orgUnitId [%s]", orgUnitId), e);
        }
    }

    @Override
    public Optional<LmsAssignment> listAssignment(LtiUserEntity apiUser, String orgUnitId, String assignmentId) throws ApiException {
        try {
            return castOptional(
                getReader(apiUser, AssignmentReaderService.class)
                    .getSingleAssignment(orgUnitId, Long.parseLong(assignmentId))
            );
        } catch (ObjectNotFoundException ex) {
            return Optional.empty();
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to get the assignments with id [%s] from Brightspace orgUnitId [%s]", assignmentId, orgUnitId), e);
        }
    }

    @Override
    public Optional<LmsAssignment> listAssignment(LtiUserEntity apiUser, String lmsCourseId, Assignment assignment) throws ApiException, TerracottaConnectorException {
        Optional<LmsAssignment> lmsAssignment = listAssignment(apiUser, lmsCourseId, assignment.getLmsAssignmentId());

        if (!lmsAssignment.get().isPublished()) {
            return lmsAssignment;
        }

        if (StringUtils.isBlank(assignment.getMetadata())) {
            return lmsAssignment;
        }

        Map<String, Map<String, Object>> metadata = JsonMapper.builder()
            .build()
            .readValue(
                assignment.getMetadata(),
                new TypeReference<Map<String, Map<String, Object>>>() {}
            );
        BrightspaceAssignmentMetadata brightspaceAssignmentMetadata = JsonMapper.builder()
            .build()
            .convertValue(
                metadata.get(BrightspaceAssignmentMetadata.KEY),
                BrightspaceAssignmentMetadata.class
            );

        try {
            Optional<ContentObjectModule> contentObjectModule = getReader(apiUser, ContentObjectModuleReaderService.class)
                .get(lmsCourseId, brightspaceAssignmentMetadata.getContentModuleId());

            if (contentObjectModule.get().getIsHidden()) {
                lmsAssignment.get().setPublished(false);

                return lmsAssignment;
            }

        } catch (IOException e) {
            throw new ApiException(
                String.format(
                    "Failed to get the content module with id [%s] from Brightspace orgUnitId [%s]",
                    brightspaceAssignmentMetadata.getContentModuleId(),
                    lmsCourseId
                ),
                e
            );
        }

        try {
            Optional<ContentObjectTopic> contentObjectTopic = getReader(apiUser, ContentObjectTopicReaderService.class)
                .get(lmsCourseId, brightspaceAssignmentMetadata.getContentTopicId());

            if (contentObjectTopic.get().getIsHidden()) {
                lmsAssignment.get().setPublished(false);

                return lmsAssignment;
            }

        } catch (IOException e) {
            throw new ApiException(
                String.format(
                    "Failed to get the content topic with id [%s] from Brightspace orgUnitId [%s]",
                    brightspaceAssignmentMetadata.getContentTopicId(),
                    lmsCourseId
                ),
                e
            );
        }

        return lmsAssignment;
    }

    @Override
    public Optional<LmsAssignment> checkAssignmentExists(LtiUserEntity apiUser, String assignmentId, String orgUnitId) throws ApiException {
        try {
            return castOptional(
                getReader(apiUser, AssignmentReaderService.class)
                    .getSingleAssignment(orgUnitId, Long.parseLong(assignmentId))
            );
        } catch (ObjectNotFoundException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to get the Assignment in Brightspace orgUnitId [%s]", orgUnitId), e);
        }
    }

    @Override
    public Optional<LmsAssignment> editAssignment(LtiUserEntity apiUser, LmsAssignment lmsAssignment, String orgUnitId) throws ApiException {
        try {
            return castOptional(
                getWriter(apiUser, AssignmentWriterService.class)
                    .editAssignment(orgUnitId, lmsAssignment)
            );
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to edit the assignments with id [%s] from Brightspace orgUnitId [%s]", lmsAssignment.getId(), orgUnitId), e);
        }
    }

    @Override
    public Optional<LmsAssignment> editAssignment(PlatformDeployment platformDeployment, LmsAssignment lmsAssignment, String orgUnitId, String tokenOverride) throws ApiException {
        try {
            return castOptional(
                getWriter(platformDeployment.getBaseUrl(), AssignmentWriterService.class, tokenOverride)
                    .editAssignment(orgUnitId, lmsAssignment)
            );
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to edit the assignment with id [%s] from Brightspace orgUnitId [%s]", lmsAssignment.getId(), orgUnitId), e);
        }
    }

    @Override
    public void editAssignmentNameInLms(Assignment assignment, String lmsCourseId, String newName, LtiUserEntity instructorUser) throws ApiException, IOException {
        Optional<LmsAssignment> lmsAssignment = listAssignment(instructorUser, lmsCourseId, assignment.getLmsAssignmentId());

        if (lmsAssignment.isEmpty()) {
            log.warn("The assignment [{}] (Brightspace id: [{}]) was already deleted", assignment.getTitle(), assignment.getLmsAssignmentId());
            return;
        }

        lmsAssignment.get().setName(newName);
        lmsAssignment.get().setMetadata(assignment.getMetadata());
        editAssignment(instructorUser, lmsAssignment.get(), lmsCourseId);
    }

    @Override
    public void deleteAssignmentInLms(Assignment assignment, String lmsCourseId, LtiUserEntity instructorUser) throws ApiException, IOException {
        Optional<LmsAssignment> lmsAssignment = listAssignment(instructorUser, lmsCourseId, assignment.getLmsAssignmentId());

        if (lmsAssignment.isEmpty()) {
            log.warn("The assignment [{}] (Brightspace id: [{}]) was already deleted", assignment.getTitle(), assignment.getLmsAssignmentId());
            return;
        }

        lmsAssignment.get().setMetadata(assignment.getMetadata());
        deleteAssignmentInLms(lmsAssignment.get(), lmsCourseId, instructorUser);
    }

    @Override
    public void deleteAssignmentInLms(LmsAssignment lmsAssignment, String lmsCourseId, LtiUserEntity instructorUser) throws ApiException, IOException {
        try {
            getWriter(instructorUser, AssignmentWriterService.class).deleteAssignment(lmsCourseId, lmsAssignment);
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to delete the Brightspace assignment with id [%s] from Brightspace orgUnitId [%s]", lmsAssignment.getId(), lmsCourseId), e);
        }
    }

    @Override
    public AssignmentExtended uploadConsentFile(Experiment experiment, ConsentDocument consentDocument, LtiUserEntity instructorUser) throws ApiException, IOException {
        String orgUnitId = experiment.getLtiContextEntity().getContextKey();

        try {
            String deploymentId = brightspaceLmsUtils.parseDeploymentId(
                experiment.getLtiContextEntity().getToolDeployment().getPlatformDeployment(),
                experiment.getLtiContextEntity().getContext_memberships_url()
            );
            AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

            /* dropbox folder update */

            assignmentExtended.getAssignment().getDropboxFolderUpdate().setAllowOnlyUsersWithSpecialAccess(false);
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setIsAnonymous(false);
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setAssessment(
                Assessment.builder()
                    .scoreDenominator(1F)
                    .build()
            );
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setAvailability(
                Availability.builder()
                    .startDate(null)
                    .endDate(null)
                    .startDateAvailabilityType(null)
                    .endDateAvailabilityType(null)
                    .build()
            );
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setCompletionType(CompletionType.OnSubmission.key());
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setDisplayInCalendar(false);
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setDropboxType(DropboxType.Individual.key());
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setIsHidden(true);
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setName(consentDocument.getTitle());
            assignmentExtended.getAssignment().getDropboxFolderUpdate().setSubmissionType(SubmissionType.Text.key());

            /* content object module */

            assignmentExtended.getAssignment().getContentObjectModuleUpdate().setIsHidden(true);
            assignmentExtended.getAssignment().getContentObjectModuleUpdate().setIsLocked(false);
            assignmentExtended.getAssignment().getContentObjectModuleUpdate().setTitle(consentDocument.getTitle());
            assignmentExtended.getAssignment().getContentObjectModuleUpdate().setType(ContentObjectType.Module.key());

            /* content object topic for LTI link */

            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setIsHidden(true);
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setIsLocked(false);
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setMajorUpdate(false);
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setOpenAsExternalResource(false);
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setResetCompletionTracking(false);
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setTitle(consentDocument.getTitle());
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setTopicType(TopicType.Link.key());
            assignmentExtended.getAssignment().getContentObjectTopicLtiLinkUpdate().setType(ContentObjectType.Topic.key());

            /* lti advantage link */

            List<CustomParameter> customParameters = new ArrayList<>();
            customParameters.add(
                CustomParameter.builder()
                    .name(CustomParameter.Keys.EXPERIMENT_ID.key())
                    .value(String.valueOf(experiment.getExperimentId()))
                    .build()
            );

            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setCustomParameters(customParameters);
            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setDeploymentId(deploymentId);
            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setIsEnabled(true);
            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setName(consentDocument.getTitle());
            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setType(LinkType.Basic.key());
            assignmentExtended.getAssignment().getLtiAdvantageLinkUpdate().setUrl(
                String.format(
                    BrightspaceUrl.LTI_CONSENT_ASSIGNMENT_LAUNCH.url(),
                    instructorUser.getPlatformDeployment().getLocalUrl(),
                    experiment.getExperimentId()
                )
            );

            /* lti lineitem */

            createLineItem(assignmentExtended, experiment, consentDocument, orgUnitId);

            return getWriter(instructorUser, AssignmentWriterService.class)
                .createAssignment(orgUnitId, assignmentExtended.getAssignment())
                .orElseThrow(() -> new ApiException(String.format("Failed to create Consent Assignment in Brightspace course by orgUnitId [%s]", orgUnitId)));
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to create Consent Assignment in Brightspace course by orgUnitId [%s]", orgUnitId), e);
        }
    }

    @Override
    public List<LmsUser> listUsersForCourse(LmsGetUsersInCourseOptions lmsGetUsersInCourseOptions, LtiUserEntity apiUser) throws ApiException, TerracottaConnectorException {
        String orgUnitId = lmsGetUsersInCourseOptions.getLmsCourseId();

        try {
            return castList(
                getReader(apiUser, ClasslistUserReaderService.class)
                    .getAll(
                        orgUnitId,
                        lmsGetUsersInCourseOptions.getEnrollmentType().contains(EnrollmentType.STUDENT),
                        null
                    )
            );
        } catch (Exception e) {
            throw new ApiException(String.format("Failed to get the list of users in Brightspace course by orgUnitId [%s]", orgUnitId), e);
        }
    }

    @Override
    public void resyncAssignmentTargetUrisInLms(PlatformDeployment platformDeployment, LtiUserEntity ltiUserEntity, long orgUnitId, String tokenOverride, List<String> assignmentIds, List<String> consentAssignmentIds, List<String> allAssignmentIds)
        throws ApiException, TerracottaConnectorException {
        // not used by Brightspace
    }

    @Override
    public void updateAssignmentMetadata(Assignment assignment, LmsAssignment lmsAssignment) throws TerracottaConnectorException {
        try {
            JsonMapper jsonMapper = JsonMapper.builder().build();
            Map<String, Object> metadataMap = jsonMapper.readValue(assignment.getMetadata(), new TypeReference<Map<String, Object>>() {});

            if (MapUtils.isNotEmpty(metadataMap) && metadataMap.containsKey(BrightspaceAssignmentMetadata.KEY)) {
                BrightspaceAssignmentMetadata brightspaceMetadata = jsonMapper.convertValue((Map<String, String>) metadataMap.get(BrightspaceAssignmentMetadata.KEY), BrightspaceAssignmentMetadata.class);
                brightspaceMetadata.setDueAt(lmsAssignment.getDueAt() != null ? lmsAssignment.getDueAt().toInstant().toString() : null);
                brightspaceMetadata.setLockAt(lmsAssignment.getLockAt() != null ? lmsAssignment.getLockAt().toInstant().toString() : null);
                brightspaceMetadata.setUnlockAt(lmsAssignment.getUnlockAt() != null ? lmsAssignment.getUnlockAt().toInstant().toString() : null);
                metadataMap.put(BrightspaceAssignmentMetadata.KEY, jsonMapper.convertValue(brightspaceMetadata, new TypeReference<Map<String, Object>>() {}));
                assignment.setMetadata(jsonMapper.writeValueAsString(metadataMap));
            }
        } catch (JacksonException e) {
            log.error("Error updating Brightspace assignment metadata for assignment id: [{}]", assignment.getAssignmentId(), e);
        }
    }

    @Override
    public List<LmsCourse> listCoursesForUser(PlatformDeployment platformDeployment, String userId, String tokenOverride) throws ApiException {
        // not used by Brightspace
        return List.of();
    }

    @Override
    public List<LmsSubmission> listSubmissions(LtiUserEntity apiUser, Outcome outcome, String orgUnitId) throws ApiException, IOException, TerracottaConnectorException {
        return listSubmissions(apiUser, outcome.getLmsOutcomeId(), orgUnitId);
    }

    @Override
    public List<LmsSubmission> listSubmissions(LtiUserEntity apiUser, String lmsAssignmentId, String orgUnitId) throws ApiException, IOException, TerracottaConnectorException {
        AssignmentExtended assignmentExtended = getReader(apiUser, AssignmentReaderService.class)
            .getSingleAssignment(orgUnitId, Long.parseLong(lmsAssignmentId))
            .orElseThrow(() -> new ApiException(String.format("Failed to get the Assignment ID: [%s] in Brightspace orgUnitId [%s]", lmsAssignmentId, orgUnitId)));

        if (assignmentExtended.getAssignment() != null && assignmentExtended.getAssignment().getDropboxFolder() != null && assignmentExtended.getAssignment().getDropboxFolder().getGradeItemId() == null) {
            log.warn("The assignment (Brightspace id: [{}]) does not have a grade item associated with it", assignmentExtended.getAssignment().getDropboxFolder().getId());
            return List.of();
        }

        try {
            List<UserGradeValue> userGradeValues = getReader(apiUser, UserGradeValueReaderService.class)
                .getAll(orgUnitId, Long.toString(assignmentExtended.getAssignment().getDropboxFolder().getGradeItemId()));

            return CollectionUtils.emptyIfNull(userGradeValues).stream()
                .map(userGradeValue -> SubmissionExtended.of(userGradeValue, lmsAssignmentId, orgUnitId))
                .filter(Objects::nonNull)
                .map(submissionExtended -> submissionExtended.from())
                .toList();
        } catch (Exception e) {
            throw new ApiException(
                String.format(
                    "Failed to get the list of user grade values for grade object ID: [%s] in Brightspace course by orgUnitId: [%s]",
                    assignmentExtended.getAssignment().getDropboxFolder().getGradeItemId(),
                    orgUnitId
                ),
                e
            );
        }
    }

    @Override
    public void addLmsExtensions(Score score, Submission submission, boolean studentSubmission) throws ApiException, IOException {
        // not used by Brightspace
    }

    @Override
    public List<LmsSubmission> listSubmissionsForMultipleAssignments(LtiUserEntity apiUser, String lmsCourseId, List<String> lmsAssignmentIds) throws ApiException, IOException, TerracottaConnectorException {
        List<LmsSubmission> submissions = new ArrayList<>();
        CollectionUtils.emptyIfNull(lmsAssignmentIds).stream()
            .forEach(
                lmsAssignmentId -> {
                    try {
                        submissions.addAll(listSubmissions(apiUser, lmsAssignmentId, lmsCourseId));
                    } catch (Exception e) {
                        log.error("Error retrieving submissions for LMS assignment ID: [{}] in LMS course ID: [{}]", lmsAssignmentId, lmsCourseId, e);
                    }
                }
            );

        return submissions;
    }

    @Override
    public List<LmsConversation> sendConversation(LmsCreateConversationOptions lmsConversationOptions, LtiUserEntity apiUser) throws ApiException, TerracottaConnectorException {
        return List.of();
    }

    @Override
    public Optional<LmsConversation> getConversation(LmsGetSingleConversationOptions lmsGetSingleConversationOptions, LtiUserEntity apiUser) throws ApiException, TerracottaConnectorException {
        return Optional.empty();
    }

    @Override
    public Optional<LmsFile> getFile(LtiUserEntity apiUser, String lmsFileId) throws ApiException, TerracottaConnectorException {
        return Optional.empty();
    }

    @Override
    public List<LmsFile> getFiles(LtiUserEntity apiUser) throws ApiException, TerracottaConnectorException {
        return List.of();
    }

    private void getLineItem(AssignmentExtended assignmentExtended, Assignment assignment) {
        try {
            assignmentExtended.getAssignment().setLineItem(
                brightspaceAdvantageAgsService.getLineItem(
                    brightspaceAdvantageAgsService.getToken(
                        LtiAgsScope.LINEITEMS,
                        assignment.getExposure().getExperiment().getPlatformDeployment()
                    ),
                    assignment.getExposure().getExperiment().getLtiContextEntity(),
                    assignment.getResourceLinkId()
                )
            );
        } catch (ConnectionException e) {
            log.error("Error retrieving lineitem: [{}] for assignment id: [{}]", assignment.getResourceLinkId(), assignment.getAssignmentId(), e);
        }
    }

    private void createLineItem(AssignmentExtended assignmentExtended, Assignment assignment, String orgUnitId) {
        assignmentExtended.getAssignment().getLineItemUpdate().setEndDateTime(
            parseDate(assignment.getDueDate())
        );
        assignmentExtended.getAssignment().getLineItemUpdate().setLabel(assignment.getTitle());
        assignmentExtended.getAssignment().getLineItemUpdate().setScoreMaximum(100F);
        assignmentExtended.getAssignment().getLineItemUpdate().setTag(
            String.format(
                "Terracotta_Experiment_%s_Assignment_%s",
                assignment.getExposure().getExperiment().getTitle(),
                assignment.getTitle()
            )
        );

        try {
            assignmentExtended.getAssignment().setLineItem(
                brightspaceAdvantageAgsService.postLineItem(
                    brightspaceAdvantageAgsService.getToken(
                        LtiAgsScope.LINEITEMS,
                        assignment.getExposure().getExperiment().getPlatformDeployment()
                    ),
                    assignment.getExposure().getExperiment().getLtiContextEntity(),
                    assignmentExtended.getAssignment().getLineItemUpdate()
                )
            );
        } catch (ConnectionException e) {
            log.error("Error creating lineitem for OrgUnitId: [{}]", orgUnitId, e);
        }
    }

    private void createLineItem(AssignmentExtended assignmentExtended, Experiment experiment, ConsentDocument consentDocument, String orgUnitId) {
        assignmentExtended.getAssignment().getLineItemUpdate().setLabel(consentDocument.getTitle());
        assignmentExtended.getAssignment().getLineItemUpdate().setScoreMaximum(1F);
        assignmentExtended.getAssignment().getLineItemUpdate().setTag(
            String.format(
                "Terracotta_Experiment_%s_Consent",
                experiment.getTitle()
            )
        );

        try {
            assignmentExtended.getAssignment().setLineItem(
                brightspaceAdvantageAgsService.postLineItem(
                    brightspaceAdvantageAgsService.getToken(
                        LtiAgsScope.LINEITEMS,
                        experiment.getPlatformDeployment()
                    ),
                    experiment.getLtiContextEntity(),
                    assignmentExtended.getAssignment().getLineItemUpdate()
                )
            );
        } catch (ConnectionException e) {
            log.error("Error creating lineitem for OrgUnitId: [{}] and experiment ID: [{}]", orgUnitId, experiment.getExperimentId(), e);
        }
    }

    private <T extends BrightspaceWriterService<?, T>> T getWriter(LtiUserEntity apiUser, Class<T> clazz) throws ApiException {
        return getWriterInternal(apiUser, clazz, getOauthToken(apiUser));
    }

    private <T extends BrightspaceWriterService<?, T>> T getWriter(String baseUrl, Class<T> clazz, String tokenOverride) throws ApiException {
        return getWriterInternal(baseUrl, clazz, getOauthToken(null, tokenOverride));
    }

    private <T extends BrightspaceReaderService<?, T>> T getReader(LtiUserEntity apiUser, Class<T> clazz) throws ApiException {
        return getReaderInternal(apiUser, clazz, getOauthToken(apiUser));
    }

    private <T extends BrightspaceReaderService<?, T>> T getReader(String baseUrl, Class<T> clazz, String tokenOverride) throws ApiException {
        return getReaderInternal(baseUrl, clazz, getOauthToken(null, tokenOverride));
    }

    public <T extends BrightspaceReaderService<?, T>> T getReaderInternal(LtiUserEntity apiUser, Class<T> clazz, OauthToken oauthToken) {
        return getApiFactory(apiUser).getReader(clazz, oauthToken);
    }

    public <T extends BrightspaceReaderService<?, T>> T getReaderInternal(String baseUrl, Class<T> clazz, OauthToken oauthToken) {
        return getApiFactory(baseUrl).getReader(clazz, oauthToken);
    }

    public <T extends BrightspaceWriterService<?, T>> T getWriterInternal(LtiUserEntity apiUser, Class<T> clazz, OauthToken oauthToken) {
        return getApiFactory(apiUser).getWriter(clazz, oauthToken);
    }

    public <T extends BrightspaceWriterService<?, T>> T getWriterInternal(String baseUrl, Class<T> clazz, OauthToken oauthToken) {
        return getApiFactory(baseUrl).getWriter(clazz, oauthToken);
    }

    private BrightspaceApiFactory getApiFactory(LtiUserEntity apiUser) {
        return new BrightspaceApiFactory(apiUser.getPlatformDeployment().getBaseUrl(), apiVersion);
    }

    private BrightspaceApiFactory getApiFactory(String baseUrl) {
        return new BrightspaceApiFactory(baseUrl, apiVersion);
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

        if (brightspaceLmsOAuthService.isConfigured(apiUser.getPlatformDeployment())) {
            try {
                ApiTokenEntity brightspaceApiTokenEntity = brightspaceLmsOAuthService.getAccessToken(apiUser);

                if (tokenLoggingEnabled) {
                    log.debug("Using access token for user key: [{}]", apiUser.getUserKey());
                }

                accessToken = brightspaceApiTokenEntity.getAccessToken();
            } catch (LmsOAuthException e) {
                throw new ApiException(String.format("Could not get a Brightspace API token for user [%s]", apiUser.getUserKey()), e);
            }
        } else if (apiUser.getPlatformDeployment().getApiToken() != null) {
            if (tokenLoggingEnabled) {
                log.debug("Using admin api token configured for platform deployment key ID: [{}]", apiUser.getPlatformDeployment().getKeyId());
            }

            accessToken = apiUser.getPlatformDeployment().getApiToken();
        } else {
            throw new ApiException(
                String.format(
                    "Could not get a Brightspace API token for platform deployment key ID: [%s] and user key: [%s]",
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

    private String parseDate(Date date) {
        if (date == null) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AssignmentExtended.DATE_FORMAT);

        return formatter.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }

}
