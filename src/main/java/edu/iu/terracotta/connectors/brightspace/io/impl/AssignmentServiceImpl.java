package edu.iu.terracotta.connectors.brightspace.io.impl;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.AssignmentReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.AssignmentWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.Assignment;
import edu.iu.terracotta.connectors.brightspace.io.model.BrightspaceAssignmentMetadata;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectModule;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectModuleUpdate;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectTopic;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectTopicUpdate;
import edu.iu.terracotta.connectors.brightspace.io.model.CustomParameter;
import edu.iu.terracotta.connectors.brightspace.io.model.DropboxFolder;
import edu.iu.terracotta.connectors.brightspace.io.model.DropboxFolderUpdate;
import edu.iu.terracotta.connectors.brightspace.io.model.GradeObject;
import edu.iu.terracotta.connectors.brightspace.io.model.GradeObjectUpdate;
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageLink;
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageLinkUpdate;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import edu.iu.terracotta.connectors.brightspace.service.lms.impl.BrightspaceLmsUtilsImpl;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import io.micrometer.common.util.StringUtils;
import edu.iu.terracotta.connectors.brightspace.dao.model.extended.AssignmentExtended;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Strings;

@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LooseCoupling"})
public class AssignmentServiceImpl extends BaseServiceImpl<AssignmentExtended, AssignmentReaderService, AssignmentWriterService> implements AssignmentReaderService, AssignmentWriterService {

    private BrightspaceLmsUtilsImpl brightspaceLmsUtils;
    private ContentObjectModuleServiceImpl contentObjectModuleService;
    private ContentObjectTopicServiceImpl contentObjectTopicService;
    private DropboxFolderServiceImpl dropboxFolderService;
    private GradeObjectServiceImpl gradeObjectService;
    private LtiAdvantageLinkServiceImpl ltiAdvantageLinkService;
    private LtiAdvantageQuickLinkServiceImpl ltiAdvantageQuickLinkService;

    public AssignmentServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.contentObjectModuleService = new ContentObjectModuleServiceImpl(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.contentObjectTopicService = new ContentObjectTopicServiceImpl(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.dropboxFolderService = new DropboxFolderServiceImpl(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.gradeObjectService = new GradeObjectServiceImpl(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.ltiAdvantageLinkService = new LtiAdvantageLinkServiceImpl(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.ltiAdvantageQuickLinkService = new LtiAdvantageQuickLinkServiceImpl(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.brightspaceLmsUtils = new BrightspaceLmsUtilsImpl();
    }

    public List<AssignmentExtended> listCourseAssignments(String orgUnitId) throws IOException {
        List<DropboxFolder> dropboxFolders = dropboxFolderService.getAllForOrgUnitId(orgUnitId);

        return dropboxFolders.stream()
            .map(
                dropboxFolder -> {
                    AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();
                    assignmentExtended.getAssignment().setDropboxFolder(dropboxFolder);

                    return assignmentExtended;
                }
            )
            .toList();
    }

    public List<AssignmentExtended> listUserAssignments(String orgUnitId, long userId) throws IOException {
        return List.of();
    }

    public Optional<AssignmentExtended> getSingleAssignment(String orgUnitId, long assignmentId) throws IOException {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();
        assignmentExtended.getAssignment().setDropboxFolder(
            dropboxFolderService.get(orgUnitId, assignmentId)
                .orElseThrow(() -> new IOException(String.format("No dropbox folder found for orgUnitId: [%s] and assignmentId: [%s]", orgUnitId, assignmentId)))
        );

        return Optional.of(assignmentExtended);
    }

    public Optional<AssignmentExtended> createAssignment(String orgUnitId, Assignment assignment) throws IOException {
        /* grade object */

        if (assignment.getGradeObject() == null || assignment.getGradeObject().getId() == null) {
            assignment.setGradeObject(
                gradeObjectService.getLatest(orgUnitId)
                    .orElse(null)
            );
        }

        /* dropbox folder */

        if (assignment.getDropboxFolder() == null || assignment.getDropboxFolder().getId() == null) {
            // set grade object id for the dropbox folder
            assignment.getDropboxFolderUpdate().setGradeItemId(assignment.getGradeObject().getId());

            assignment.setDropboxFolder(
                dropboxFolderService.create(
                    orgUnitId,
                    assignment.getDropboxFolderUpdate()
                )
                .orElse(null)
            );
        }

        /* lti advantage link and associated quick link */

        if (assignment.getLtiAdvantageLink() == null || assignment.getLtiAdvantageLink().getLinkId() == null) {
            assignment.getLtiAdvantageLinkUpdate().getCustomParameters().add(
                CustomParameter.builder()
                    .name(CustomParameter.Keys.BRIGHTSPACE_ASSIGNMENT_ID.key())
                    .value(String.valueOf(assignment.getDropboxFolder().getId()))
                    .build()
            );

            assignment.setLtiAdvantageLink(
                ltiAdvantageLinkService.create(
                    orgUnitId,
                    assignment.getLtiAdvantageLinkUpdate()
                )
                .orElse(null)
            );
        }

        /* lti advantage quick link for the lti advantage link */

        assignment.setLtiAdvantageQuickLink(
            ltiAdvantageQuickLinkService.create(
                orgUnitId,
                assignment.getLtiAdvantageLink().getLinkId()
            )
            .orElse(null)
        );

        /* content module */

        // only create the content module if we don't already have a topic (which may be associated with another module)
        if (assignment.getContentObjectTopicLtiLink().getId() == null && (assignment.getContentObjectModule() == null || assignment.getContentObjectModule().getId() == null)) {
            assignment.setContentObjectModule(
                contentObjectModuleService.create(
                    orgUnitId,
                    assignment.getContentObjectModuleUpdate()
                )
                .orElse(null)
            );
        }

        /* content topic for LTI launch */

        if (assignment.getContentObjectTopicLtiLink() == null || assignment.getContentObjectTopicLtiLink().getId() == null) {
            //assignment.getContentObjectTopicLtiLinkUpdate().setGradeItemId(assignment.getGradeObject().getId());
            // set URL for the content topic to be the quick link URL
            assignment.getContentObjectTopicLtiLinkUpdate().setUrl(
                Strings.CS.replace(
                    assignment.getLtiAdvantageQuickLink().getPublicUrl(),
                    "{orgUnitId}",
                    orgUnitId
                )
            );

            assignment.setContentObjectTopicLtiLink(
                contentObjectTopicService.create(
                    orgUnitId,
                    assignment.getContentObjectModule().getId(),
                    assignment.getContentObjectTopicLtiLinkUpdate()
                )
                .orElse(null)
            );
        }

        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();
        assignmentExtended.setAssignment(assignment);
        assignmentExtended.setSecureParams(
            JsonMapper.builder()
            .build()
            .writeValueAsString(
                Map.of(
                    BrightspaceAssignmentMetadata.LTI_ASSIGNMENT_ID, assignment.getLineItem().getId()
                )
            )
        );

        try {
            assignmentExtended.addMetadata(
                BrightspaceAssignmentMetadata.KEY,
                JsonMapper.builder()
                .build()
                .convertValue(
                    BrightspaceAssignmentMetadata.builder()
                        .contentModuleId(assignment.getContentObjectTopicLtiLink().getParentModuleId())
                        .contentTopicId(assignment.getContentObjectTopicLtiLink().getId())
                        .dropboxFolderId(assignment.getDropboxFolder().getId())
                        .dueAt(assignment.getDropboxFolder().getDueDate())
                        .gradeObjectId(assignment.getGradeObject().getId())
                        .lockAt(assignment.getDropboxFolder().getAvailability() != null ? assignment.getDropboxFolder().getAvailability().getEndDate() : null)
                        .ltiAdvantageLinkId(assignment.getLtiAdvantageLink().getLinkId())
                        .ltiAdvantageQuickLinkId(assignment.getLtiAdvantageQuickLink().getLinkId())
                        .unlockAt(assignment.getDropboxFolder().getAvailability() != null ? assignment.getDropboxFolder().getAvailability().getStartDate() : null)
                        .build(),
                    new TypeReference<Map<String, Object>>() {}
                )
            );
        } catch (TerracottaConnectorException e) {
            throw new IOException(String.format("Error adding metadata to assignment: [%s]", assignmentExtended.getId()), e);
        }

        return Optional.of(assignmentExtended);
    }

    public Optional<AssignmentExtended> deleteAssignment(String orgUnitId, LmsAssignment lmsAssignment) throws IOException {
        Map<String, Map<String, Object>> metadata = JsonMapper.builder()
            .build()
            .readValue(
                lmsAssignment.getMetadata(),
                new TypeReference<Map<String, Map<String, Object>>>() {}
            );
        BrightspaceAssignmentMetadata brightspaceAssignmentMetadata = JsonMapper.builder()
            .build()
            .convertValue(
                metadata.get(BrightspaceAssignmentMetadata.KEY),
                BrightspaceAssignmentMetadata.class
            );

        try {
            contentObjectTopicService.delete(orgUnitId, brightspaceAssignmentMetadata.getContentTopicId());
        } catch (Exception e) {
            throw new IOException(
                String.format(
                    "Error deleting content topic ID: [%s] for orgUnitId: [%s] and assignment ID: [%s]",
                    brightspaceAssignmentMetadata.getContentTopicId(),
                    orgUnitId,
                    lmsAssignment.getId()
                ),
                e
            );
        }

        try {
            contentObjectModuleService.delete(orgUnitId, brightspaceAssignmentMetadata.getContentModuleId());
        } catch (Exception e) {
            throw new IOException(
                String.format(
                    "Error deleting content module ID: [%s] for orgUnitId: [%s] and assignment ID: [%s]",
                    brightspaceAssignmentMetadata.getContentModuleId(),
                    orgUnitId,
                    lmsAssignment.getId()
                ),
                e
            );
        }

        try {
            dropboxFolderService.delete(orgUnitId, brightspaceAssignmentMetadata.getDropboxFolderId());
        } catch (Exception e) {
            throw new IOException(
                String.format(
                    "Error deleting dropbox folder ID: [%s] for orgUnitId: [%s] and assignment ID: [%s]",
                    brightspaceAssignmentMetadata.getDropboxFolderId(),
                    orgUnitId,
                    lmsAssignment.getId()
                ),
                e
            );
        }

        try {
            gradeObjectService.delete(orgUnitId, brightspaceAssignmentMetadata.getGradeObjectId());
        } catch (Exception e) {
            throw new IOException(
                String.format(
                    "Error deleting grade object ID: [%s] for orgUnitId: [%s] and assignment ID: [%s]",
                    brightspaceAssignmentMetadata.getGradeObjectId(),
                    orgUnitId,
                    lmsAssignment.getId()
                ),
                e
            );
        }

        try {
            ltiAdvantageLinkService.delete(orgUnitId, brightspaceAssignmentMetadata.getLtiAdvantageLinkId());
        } catch (Exception e) {
            throw new IOException(
                String.format(
                    "Error deleting LTI advantage link ID: [%s] components for orgUnitId: [%s] and assignment ID: [%s]",
                    brightspaceAssignmentMetadata.getLtiAdvantageLinkId(),
                    orgUnitId,
                    lmsAssignment.getId()
                ),
                e
            );
        }

        return Optional.empty();
    }

    public Optional<AssignmentExtended> editAssignment(String orgUnitId, LmsAssignment lmsAssignment) throws IOException {
        DropboxFolder existingDropboxFolder = dropboxFolderService.get(orgUnitId, Long.parseLong(lmsAssignment.getId()))
            .orElseThrow(() -> new IOException(String.format("No dropbox folder found for orgUnitId: [%s] and assignmentId: [%s]", orgUnitId, lmsAssignment.getId())));

        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        /* dropbox folder */

        DropboxFolderUpdate dropboxFolderUpdate = DropboxFolderUpdate.from(existingDropboxFolder);
        dropboxFolderUpdate.setName(lmsAssignment.getName());

        assignmentExtended.getAssignment().setDropboxFolder(
            dropboxFolderService.update(orgUnitId, Long.parseLong(lmsAssignment.getId()), dropboxFolderUpdate)
                .orElseThrow(() -> new IOException(String.format("Error updating dropbox folder ID: [%s] for orgUnitId: [%s]", lmsAssignment.getId(), orgUnitId)))
        );

        if (StringUtils.isBlank(lmsAssignment.getMetadata())) {
            return Optional.of(assignmentExtended);
        }

        Map<String, Map<String, Object>> metadata = JsonMapper.builder()
            .build()
            .readValue(
                lmsAssignment.getMetadata(),
                new TypeReference<Map<String, Map<String, Object>>>() {}
            );
        final BrightspaceAssignmentMetadata brightspaceAssignmentMetadata = JsonMapper.builder()
            .build()
            .convertValue(
                metadata.get(BrightspaceAssignmentMetadata.KEY),
                BrightspaceAssignmentMetadata.class
            );

        /* content topic */

        ContentObjectTopic contentObjectTopic = contentObjectTopicService.get(orgUnitId, brightspaceAssignmentMetadata.getContentTopicId())
            .orElseThrow(() -> new IOException(String.format("No content topic found for orgUnitId: [%s] and contentTopicId: [%s]", orgUnitId, brightspaceAssignmentMetadata.getContentTopicId())));

        ContentObjectTopicUpdate contentObjectTopicUpdate = ContentObjectTopicUpdate.from(contentObjectTopic);
        contentObjectTopicUpdate.setTitle(lmsAssignment.getName());

        // NOTE: the PUT call does not return the object; so set it to the GET result
        contentObjectTopicService.update(orgUnitId, brightspaceAssignmentMetadata.getContentTopicId(), contentObjectTopicUpdate);
        assignmentExtended.getAssignment().setContentObjectTopicLtiLink(contentObjectTopic);

        /* content module */

        ContentObjectModule contentObjectModule = contentObjectModuleService.get(orgUnitId, brightspaceAssignmentMetadata.getContentModuleId())
            .orElseThrow(() -> new IOException(String.format("No content module found for orgUnitId: [%s] and contentModuleId: [%s]", orgUnitId, brightspaceAssignmentMetadata.getContentModuleId())));
        ContentObjectModuleUpdate contentObjectModuleUpdate = ContentObjectModuleUpdate.from(contentObjectModule);
        contentObjectModuleUpdate.setTitle(lmsAssignment.getName());

        // NOTE: the PUT call does not return the object; so set it to the GET result
        contentObjectModuleService.update(orgUnitId, brightspaceAssignmentMetadata.getContentModuleId(), contentObjectModuleUpdate);
        assignmentExtended.getAssignment().setContentObjectModule(contentObjectModule);

        /* grade object */

        GradeObject gradeObject = gradeObjectService.get(orgUnitId, brightspaceAssignmentMetadata.getGradeObjectId())
            .orElseThrow(() -> new IOException(String.format("No grade object found for orgUnitId: [%s] and gradeObjectId: [%s]", orgUnitId, brightspaceAssignmentMetadata.getGradeObjectId())));

        GradeObjectUpdate gradeObjectUpdate = GradeObjectUpdate.from(gradeObject);
        gradeObjectUpdate.setName(brightspaceLmsUtils.sanitize(lmsAssignment.getName()));

        assignmentExtended.getAssignment().setGradeObject(
            gradeObjectService.update(orgUnitId, brightspaceAssignmentMetadata.getGradeObjectId(), gradeObjectUpdate)
                .orElseThrow(() -> new IOException(String.format("Error updating grade object ID: [%s] for orgUnitId: [%s]", brightspaceAssignmentMetadata.getGradeObjectId(), orgUnitId)))
        );

        /* lti advantage link */

        LtiAdvantageLink ltiAdvantageLink = ltiAdvantageLinkService.get(orgUnitId, brightspaceAssignmentMetadata.getLtiAdvantageLinkId())
            .orElseThrow(() -> new IOException(String.format("No LTI advantage link found for orgUnitId: [%s] and linkId: [%s]", orgUnitId, brightspaceAssignmentMetadata.getLtiAdvantageLinkId())));

        LtiAdvantageLinkUpdate ltiAdvantageLinkUpdate = LtiAdvantageLinkUpdate.from(ltiAdvantageLink);
        ltiAdvantageLinkUpdate.setName(lmsAssignment.getName());

        assignmentExtended.getAssignment().setLtiAdvantageLink(
            ltiAdvantageLinkService.update(orgUnitId, brightspaceAssignmentMetadata.getLtiAdvantageLinkId(), ltiAdvantageLinkUpdate)
                .orElseThrow(() -> new IOException(String.format("Error updating LTI advantage link ID: [%s] for orgUnitId: [%s]", brightspaceAssignmentMetadata.getLtiAdvantageLinkId(), orgUnitId)))
        );

        return Optional.of(assignmentExtended);
    }

    @Override
    protected TypeReference<List<AssignmentExtended>> listType() {
        return new TypeReference<List<AssignmentExtended>>() {};
    }

    @Override
    protected Class<AssignmentExtended> objectType() {
        return AssignmentExtended.class;
    }

}
