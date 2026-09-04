package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.connectors.brightspace.dao.model.extended.AssignmentExtended;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.Assignment;
import edu.iu.terracotta.connectors.brightspace.io.model.Availability;
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
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageQuickLink;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests for {@link AssignmentServiceImpl}. This class deals purely with building/parsing calls
 * against the Brightspace (D2L Valence) API through its sibling *ServiceImpl collaborators (all
 * of which are constructed directly inside {@link AssignmentServiceImpl}'s constructor rather
 * than injected by Spring), so this test intentionally does NOT extend
 * {@code edu.iu.terracotta.base.BaseTest} -- there are no Terracotta domain entities/repositories
 * involved. The collaborator services are swapped for mocks via {@code ReflectionTestUtils}
 * after construction, matching the pattern already used elsewhere in this codebase for mocking
 * concrete *ServiceImpl fields (e.g. {@code ConnectorServiceImplTest#canvasClientOAuthService}).
 */
public class AssignmentServiceImplTest {

    private static final String ORG_UNIT_ID = "orgUnit1";

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;

    @Mock private DropboxFolderServiceImpl dropboxFolderService;
    @Mock private GradeObjectServiceImpl gradeObjectService;
    @Mock private LtiAdvantageLinkServiceImpl ltiAdvantageLinkService;
    @Mock private LtiAdvantageQuickLinkServiceImpl ltiAdvantageQuickLinkService;
    @Mock private ContentObjectModuleServiceImpl contentObjectModuleService;
    @Mock private ContentObjectTopicServiceImpl contentObjectTopicService;

    private AssignmentServiceImpl assignmentService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);

        assignmentService = new AssignmentServiceImpl(
            "https://example.brightspace.com",
            ApiVersion.builder().le("1.40").lp("1.30").build(),
            oauthToken,
            restClient,
            10,
            10,
            100,
            false
        );

        // the constructor builds real collaborator instances; swap them for mocks
        ReflectionTestUtils.setField(assignmentService, "dropboxFolderService", dropboxFolderService);
        ReflectionTestUtils.setField(assignmentService, "gradeObjectService", gradeObjectService);
        ReflectionTestUtils.setField(assignmentService, "ltiAdvantageLinkService", ltiAdvantageLinkService);
        ReflectionTestUtils.setField(assignmentService, "ltiAdvantageQuickLinkService", ltiAdvantageQuickLinkService);
        ReflectionTestUtils.setField(assignmentService, "contentObjectModuleService", contentObjectModuleService);
        ReflectionTestUtils.setField(assignmentService, "contentObjectTopicService", contentObjectTopicService);
    }

    /* ============================== listCourseAssignments ============================== */

    @Test
    void testListCourseAssignments_returnsOneAssignmentPerDropboxFolder() throws IOException {
        DropboxFolder folder1 = DropboxFolder.builder().id(100L).name("Assignment 1").build();
        DropboxFolder folder2 = DropboxFolder.builder().id(200L).name("Assignment 2").build();
        when(dropboxFolderService.getAllForOrgUnitId(ORG_UNIT_ID)).thenReturn(List.of(folder1, folder2));

        List<AssignmentExtended> result = assignmentService.listCourseAssignments(ORG_UNIT_ID);

        assertEquals(2, result.size());
        assertEquals(100L, result.get(0).getAssignment().getDropboxFolder().getId());
        assertEquals(200L, result.get(1).getAssignment().getDropboxFolder().getId());
    }

    @Test
    void testListCourseAssignments_noFolders_returnsEmptyList() throws IOException {
        when(dropboxFolderService.getAllForOrgUnitId(ORG_UNIT_ID)).thenReturn(List.of());

        List<AssignmentExtended> result = assignmentService.listCourseAssignments(ORG_UNIT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void testListCourseAssignments_dropboxServiceThrows_propagatesIOException() throws IOException {
        when(dropboxFolderService.getAllForOrgUnitId(ORG_UNIT_ID)).thenThrow(new IOException("boom"));

        assertThrows(IOException.class, () -> assignmentService.listCourseAssignments(ORG_UNIT_ID));
    }

    /* ================================ listUserAssignments =============================== */

    @Test
    void testListUserAssignments_alwaysReturnsEmptyListRegardlessOfInput() throws IOException {
        // NOTE (dead code / unimplemented feature): listUserAssignments unconditionally returns
        // List.of() -- orgUnitId and userId are completely ignored and no collaborator is ever
        // called. This looks like a stubbed-out method that was never finished.
        List<AssignmentExtended> result = assignmentService.listUserAssignments(ORG_UNIT_ID, 42L);

        assertTrue(result.isEmpty());
        verifyNoInteractions(dropboxFolderService, gradeObjectService, ltiAdvantageLinkService, ltiAdvantageQuickLinkService, contentObjectModuleService, contentObjectTopicService);
    }

    /* ================================ getSingleAssignment ================================ */

    @Test
    void testGetSingleAssignment_found_returnsAssignmentExtendedWithDropboxFolder() throws IOException {
        DropboxFolder folder = DropboxFolder.builder().id(555L).name("A1").build();
        when(dropboxFolderService.get(ORG_UNIT_ID, 555L)).thenReturn(Optional.of(folder));

        Optional<AssignmentExtended> result = assignmentService.getSingleAssignment(ORG_UNIT_ID, 555L);

        assertTrue(result.isPresent());
        assertEquals(555L, result.get().getAssignment().getDropboxFolder().getId());
    }

    @Test
    void testGetSingleAssignment_notFound_throwsIOException() throws IOException {
        when(dropboxFolderService.get(ORG_UNIT_ID, 555L)).thenReturn(Optional.empty());

        IOException exception = assertThrows(IOException.class, () -> assignmentService.getSingleAssignment(ORG_UNIT_ID, 555L));

        assertTrue(exception.getMessage().contains("555"));
    }

    /* =================================== createAssignment ================================= */

    @Test
    void testCreateAssignment_allComponentsMissing_createsEverythingAndBuildsMetadata() throws IOException {
        Assignment assignment = Assignment.builder().build();
        assignment.getLineItem().setId("lineitem-abc");
        // mirrors the real (and only) caller, BrightspaceApiClientImpl, which always primes this
        // list before calling createAssignment -- see bug note further down for what happens
        // when it is left at its (null) default.
        assignment.getLtiAdvantageLinkUpdate().setCustomParameters(new ArrayList<>());

        GradeObject gradeObject = GradeObject.builder().id(10L).name("Grade").build();
        when(gradeObjectService.getLatest(ORG_UNIT_ID)).thenReturn(Optional.of(gradeObject));

        Availability availability = Availability.builder().startDate("2026-01-01T00:00:00.000Z").endDate("2026-02-01T00:00:00.000Z").build();
        DropboxFolder createdFolder = DropboxFolder.builder().id(20L).dueDate("2026-01-15T00:00:00.000Z").availability(availability).build();
        when(dropboxFolderService.create(eq(ORG_UNIT_ID), any(DropboxFolderUpdate.class))).thenReturn(Optional.of(createdFolder));

        LtiAdvantageLink createdLink = LtiAdvantageLink.builder().linkId(30L).build();
        when(ltiAdvantageLinkService.create(eq(ORG_UNIT_ID), any(LtiAdvantageLinkUpdate.class))).thenReturn(Optional.of(createdLink));

        LtiAdvantageQuickLink createdQuickLink = LtiAdvantageQuickLink.builder().linkId(31L).publicUrl("https://d2l.example.com/{orgUnitId}/quicklink").build();
        when(ltiAdvantageQuickLinkService.create(ORG_UNIT_ID, 30L)).thenReturn(Optional.of(createdQuickLink));

        ContentObjectModule createdModule = ContentObjectModule.builder().id(40L).build();
        when(contentObjectModuleService.create(eq(ORG_UNIT_ID), any(ContentObjectModuleUpdate.class))).thenReturn(Optional.of(createdModule));

        ContentObjectTopic createdTopic = ContentObjectTopic.builder().id(50L).parentModuleId(40L).build();
        when(contentObjectTopicService.create(eq(ORG_UNIT_ID), eq(40L), any(ContentObjectTopicUpdate.class))).thenReturn(Optional.of(createdTopic));

        Optional<AssignmentExtended> result = assignmentService.createAssignment(ORG_UNIT_ID, assignment);

        assertTrue(result.isPresent());
        AssignmentExtended assignmentExtended = result.get();
        assertEquals(20L, assignmentExtended.getAssignment().getDropboxFolder().getId());
        assertEquals(30L, assignmentExtended.getAssignment().getLtiAdvantageLink().getLinkId());
        assertEquals(31L, assignmentExtended.getAssignment().getLtiAdvantageQuickLink().getLinkId());
        assertEquals(40L, assignmentExtended.getAssignment().getContentObjectModule().getId());
        assertEquals(50L, assignmentExtended.getAssignment().getContentObjectTopicLtiLink().getId());

        // secure params contain the lti line item id
        JsonMapper jsonMapper = JsonMapper.builder().build();
        Map<String, Object> secureParams = jsonMapper.readValue(assignmentExtended.getSecureParams(), new TypeReference<Map<String, Object>>() { });
        assertEquals("lineitem-abc", secureParams.get(BrightspaceAssignmentMetadata.LTI_ASSIGNMENT_ID));

        // metadata reflects everything created above
        Map<String, Map<String, Object>> parsedMetadata = jsonMapper.readValue(assignmentExtended.getMetadata(), new TypeReference<Map<String, Map<String, Object>>>() { });
        Map<String, Object> brightspaceMetadata = parsedMetadata.get(BrightspaceAssignmentMetadata.KEY);
        assertEquals(40, ((Number) brightspaceMetadata.get("contentModuleId")).intValue());
        assertEquals(50, ((Number) brightspaceMetadata.get("contentTopicId")).intValue());
        assertEquals(20, ((Number) brightspaceMetadata.get("dropboxFolderId")).intValue());
        assertEquals(10, ((Number) brightspaceMetadata.get("gradeObjectId")).intValue());
        assertEquals(30, ((Number) brightspaceMetadata.get("ltiAdvantageLinkId")).intValue());
        assertEquals(31, ((Number) brightspaceMetadata.get("ltiAdvantageQuickLinkId")).intValue());
        assertEquals("2026-01-15T00:00:00.000Z", brightspaceMetadata.get("dueAt"));
        assertEquals("2026-02-01T00:00:00.000Z", brightspaceMetadata.get("lockAt"));
        assertEquals("2026-01-01T00:00:00.000Z", brightspaceMetadata.get("unlockAt"));

        // the newly-created dropbox folder id is threaded through as a custom parameter on the lti link
        ArgumentCaptor<LtiAdvantageLinkUpdate> linkUpdateCaptor = ArgumentCaptor.forClass(LtiAdvantageLinkUpdate.class);
        verify(ltiAdvantageLinkService).create(eq(ORG_UNIT_ID), linkUpdateCaptor.capture());
        boolean hasCustomParam = linkUpdateCaptor.getValue().getCustomParameters().stream()
            .anyMatch(param -> CustomParameter.Keys.BRIGHTSPACE_ASSIGNMENT_ID.key().equals(param.getName()) && "20".equals(param.getValue()));
        assertTrue(hasCustomParam);

        // the fetched grade object id is threaded through to the dropbox folder update
        ArgumentCaptor<DropboxFolderUpdate> folderUpdateCaptor = ArgumentCaptor.forClass(DropboxFolderUpdate.class);
        verify(dropboxFolderService).create(eq(ORG_UNIT_ID), folderUpdateCaptor.capture());
        assertEquals(10L, folderUpdateCaptor.getValue().getGradeItemId());

        // the content topic url is built from the quick link's public url, substituting orgUnitId
        ArgumentCaptor<ContentObjectTopicUpdate> topicUpdateCaptor = ArgumentCaptor.forClass(ContentObjectTopicUpdate.class);
        verify(contentObjectTopicService).create(eq(ORG_UNIT_ID), eq(40L), topicUpdateCaptor.capture());
        assertEquals("https://d2l.example.com/" + ORG_UNIT_ID + "/quicklink", topicUpdateCaptor.getValue().getUrl());
    }

    @Test
    void testCreateAssignment_gradeObjectAlreadySet_doesNotFetchLatestGradeObject() throws IOException {
        Assignment assignment = Assignment.builder().build();
        assignment.getLineItem().setId("lineitem-1");
        assignment.getLtiAdvantageLinkUpdate().setCustomParameters(new ArrayList<>());
        assignment.setGradeObject(GradeObject.builder().id(999L).build());

        when(dropboxFolderService.create(eq(ORG_UNIT_ID), any(DropboxFolderUpdate.class))).thenReturn(Optional.of(DropboxFolder.builder().id(20L).build()));
        when(ltiAdvantageLinkService.create(eq(ORG_UNIT_ID), any(LtiAdvantageLinkUpdate.class))).thenReturn(Optional.of(LtiAdvantageLink.builder().linkId(30L).build()));
        when(ltiAdvantageQuickLinkService.create(ORG_UNIT_ID, 30L)).thenReturn(Optional.of(LtiAdvantageQuickLink.builder().linkId(31L).publicUrl("https://x/{orgUnitId}").build()));
        when(contentObjectModuleService.create(eq(ORG_UNIT_ID), any(ContentObjectModuleUpdate.class))).thenReturn(Optional.of(ContentObjectModule.builder().id(40L).build()));
        when(contentObjectTopicService.create(eq(ORG_UNIT_ID), eq(40L), any(ContentObjectTopicUpdate.class))).thenReturn(Optional.of(ContentObjectTopic.builder().id(50L).parentModuleId(40L).build()));

        assignmentService.createAssignment(ORG_UNIT_ID, assignment);

        verify(gradeObjectService, never()).getLatest(any());
        ArgumentCaptor<DropboxFolderUpdate> folderUpdateCaptor = ArgumentCaptor.forClass(DropboxFolderUpdate.class);
        verify(dropboxFolderService).create(eq(ORG_UNIT_ID), folderUpdateCaptor.capture());
        assertEquals(999L, folderUpdateCaptor.getValue().getGradeItemId());
    }

    @Test
    void testCreateAssignment_dropboxFolderAndLtiLinkAlreadySet_reusesExistingIds() throws IOException {
        Assignment assignment = Assignment.builder().build();
        assignment.getLineItem().setId("lineitem-1");
        assignment.setGradeObject(GradeObject.builder().id(1L).build());
        assignment.setDropboxFolder(DropboxFolder.builder().id(20L).build());
        assignment.setLtiAdvantageLink(LtiAdvantageLink.builder().linkId(30L).build());

        when(ltiAdvantageQuickLinkService.create(ORG_UNIT_ID, 30L)).thenReturn(Optional.of(LtiAdvantageQuickLink.builder().linkId(31L).publicUrl("https://x/{orgUnitId}").build()));
        when(contentObjectModuleService.create(eq(ORG_UNIT_ID), any(ContentObjectModuleUpdate.class))).thenReturn(Optional.of(ContentObjectModule.builder().id(40L).build()));
        when(contentObjectTopicService.create(eq(ORG_UNIT_ID), eq(40L), any(ContentObjectTopicUpdate.class))).thenReturn(Optional.of(ContentObjectTopic.builder().id(50L).parentModuleId(40L).build()));

        assignmentService.createAssignment(ORG_UNIT_ID, assignment);

        verify(dropboxFolderService, never()).create(any(), any());
        verify(ltiAdvantageLinkService, never()).create(any(), any());
        verify(ltiAdvantageQuickLinkService).create(ORG_UNIT_ID, 30L);
    }

    @Test
    void testCreateAssignment_contentModuleAlreadySet_skipsModuleCreationButStillCreatesTopic() throws IOException {
        Assignment assignment = Assignment.builder().build();
        assignment.getLineItem().setId("lineitem-1");
        assignment.setGradeObject(GradeObject.builder().id(1L).build());
        assignment.setDropboxFolder(DropboxFolder.builder().id(20L).build());
        assignment.setLtiAdvantageLink(LtiAdvantageLink.builder().linkId(30L).build());
        assignment.setContentObjectModule(ContentObjectModule.builder().id(555L).build());

        when(ltiAdvantageQuickLinkService.create(ORG_UNIT_ID, 30L)).thenReturn(Optional.of(LtiAdvantageQuickLink.builder().linkId(31L).publicUrl("https://x/{orgUnitId}").build()));
        when(contentObjectTopicService.create(eq(ORG_UNIT_ID), eq(555L), any(ContentObjectTopicUpdate.class))).thenReturn(Optional.of(ContentObjectTopic.builder().id(50L).parentModuleId(555L).build()));

        assignmentService.createAssignment(ORG_UNIT_ID, assignment);

        verify(contentObjectModuleService, never()).create(any(), any());
        verify(contentObjectTopicService).create(eq(ORG_UNIT_ID), eq(555L), any(ContentObjectTopicUpdate.class));
    }

    @Test
    void testCreateAssignment_contentTopicAlreadySet_skipsModuleAndTopicCreation() throws IOException {
        Assignment assignment = Assignment.builder().build();
        assignment.getLineItem().setId("lineitem-1");
        assignment.setGradeObject(GradeObject.builder().id(1L).build());
        assignment.setDropboxFolder(DropboxFolder.builder().id(20L).build());
        assignment.setLtiAdvantageLink(LtiAdvantageLink.builder().linkId(30L).build());
        assignment.setContentObjectTopicLtiLink(ContentObjectTopic.builder().id(999L).parentModuleId(888L).build());

        when(ltiAdvantageQuickLinkService.create(ORG_UNIT_ID, 30L)).thenReturn(Optional.of(LtiAdvantageQuickLink.builder().linkId(31L).publicUrl("https://x/{orgUnitId}").build()));

        Optional<AssignmentExtended> result = assignmentService.createAssignment(ORG_UNIT_ID, assignment);

        verify(contentObjectModuleService, never()).create(any(), any());
        verify(contentObjectTopicService, never()).create(any(), anyLong(), any());
        assertEquals(999L, result.get().getAssignment().getContentObjectTopicLtiLink().getId());
    }

    @Test
    void testCreateAssignment_lineItemIdNull_completesWithNullLineItemIdInSecureParams() throws IOException {
        // the line item id is null here whenever lineitem creation failed upstream and was
        // swallowed (see BrightspaceApiClientImpl.createLineItem's ConnectionException catch).
        // createAssignment should still complete a best-effort assignment (gradebook sync just
        // won't work) instead of crashing -- Map.of(key, value), which was used here previously,
        // throws an NPE on a null value; a null-tolerant map fixes that.
        Assignment assignment = Assignment.builder().build();
        assignment.setGradeObject(GradeObject.builder().id(1L).build());
        assignment.setDropboxFolder(DropboxFolder.builder().id(20L).build());
        assignment.setLtiAdvantageLink(LtiAdvantageLink.builder().linkId(30L).build());
        assignment.setContentObjectTopicLtiLink(ContentObjectTopic.builder().id(999L).parentModuleId(888L).build());

        when(ltiAdvantageQuickLinkService.create(ORG_UNIT_ID, 30L)).thenReturn(Optional.of(LtiAdvantageQuickLink.builder().linkId(31L).publicUrl("https://x/{orgUnitId}").build()));

        Optional<AssignmentExtended> result = assignmentService.createAssignment(ORG_UNIT_ID, assignment);

        assertTrue(result.isPresent());
        JsonMapper jsonMapper = JsonMapper.builder().build();
        Map<String, Object> secureParams = jsonMapper.readValue(result.get().getSecureParams(), new TypeReference<Map<String, Object>>() { });
        assertTrue(secureParams.containsKey(BrightspaceAssignmentMetadata.LTI_ASSIGNMENT_ID));
        assertNull(secureParams.get(BrightspaceAssignmentMetadata.LTI_ASSIGNMENT_ID));
    }

    @Test
    void testCreateAssignment_noExistingGradeObjectFound_throwsNullPointerException() throws IOException {
        // BUG: when there is no existing grade object in the course (gradeObjectService.getLatest
        // returns empty), assignment.setGradeObject(null) runs, but the very next line
        // unconditionally calls assignment.getGradeObject().getId() to populate the dropbox
        // folder update -- no null check -- so this throws an unchecked NullPointerException.
        Assignment assignment = Assignment.builder().build();
        when(gradeObjectService.getLatest(ORG_UNIT_ID)).thenReturn(Optional.empty());

        assertThrows(NullPointerException.class, () -> assignmentService.createAssignment(ORG_UNIT_ID, assignment));
    }

    @Test
    void testCreateAssignment_ltiAdvantageLinkUpdateCustomParametersNotPrimed_throwsNullPointerException() {
        // BUG/fragile precondition: Assignment#ltiAdvantageLinkUpdate defaults to
        // LtiAdvantageLinkUpdate.builder().build(), whose "customParameters" list has no
        // @Builder.Default and is therefore null. createAssignment calls
        // .getCustomParameters().add(...) directly with no null guard. In production this is
        // only safe because the sole caller (BrightspaceApiClientImpl) always calls
        // setCustomParameters(new ArrayList<>()) first -- AssignmentServiceImpl itself neither
        // enforces nor documents this precondition.
        Assignment assignment = Assignment.builder().build();
        assignment.setGradeObject(GradeObject.builder().id(1L).build());
        assignment.setDropboxFolder(DropboxFolder.builder().id(20L).build());
        // ltiAdvantageLink/ltiAdvantageLinkUpdate left at their (unprimed) defaults

        assertThrows(NullPointerException.class, () -> assignmentService.createAssignment(ORG_UNIT_ID, assignment));
    }

    /* =================================== deleteAssignment ================================= */

    @Test
    void testDeleteAssignment_allDeletesSucceed_returnsEmptyOptional() throws IOException {
        LmsAssignment lmsAssignment = fullMetadataLmsAssignment();

        Optional<AssignmentExtended> result = assignmentService.deleteAssignment(ORG_UNIT_ID, lmsAssignment);

        assertTrue(result.isEmpty());
        verify(contentObjectTopicService).delete(ORG_UNIT_ID, 50L);
        verify(contentObjectModuleService).delete(ORG_UNIT_ID, 40L);
        verify(dropboxFolderService).delete(ORG_UNIT_ID, 20L);
        verify(gradeObjectService).delete(ORG_UNIT_ID, 10L);
        verify(ltiAdvantageLinkService).delete(ORG_UNIT_ID, 30L);
    }

    @Test
    void testDeleteAssignment_contentTopicDeleteFails_wrapsInIOExceptionAndAbortsRemainingDeletes() throws IOException {
        LmsAssignment lmsAssignment = fullMetadataLmsAssignment();
        doThrow(new IOException("topic delete failed")).when(contentObjectTopicService).delete(ORG_UNIT_ID, 50L);

        IOException exception = assertThrows(IOException.class, () -> assignmentService.deleteAssignment(ORG_UNIT_ID, lmsAssignment));
        assertTrue(exception.getMessage().contains("content topic"));

        // NOTE: because each cleanup step's failure is re-thrown immediately (rather than
        // collected/logged and continued), a failure on an earlier resource silently prevents
        // any attempt to clean up the resources that follow it.
        verify(contentObjectModuleService, never()).delete(any(), anyLong());
        verify(dropboxFolderService, never()).delete(any(), anyLong());
        verify(gradeObjectService, never()).delete(any(), anyLong());
        verify(ltiAdvantageLinkService, never()).delete(any(), anyLong());
    }

    @Test
    void testDeleteAssignment_gradeObjectDeleteFails_skipsFinalLtiAdvantageLinkDelete() throws IOException {
        LmsAssignment lmsAssignment = fullMetadataLmsAssignment();
        doThrow(new RuntimeException("grade delete failed")).when(gradeObjectService).delete(ORG_UNIT_ID, 10L);

        IOException exception = assertThrows(IOException.class, () -> assignmentService.deleteAssignment(ORG_UNIT_ID, lmsAssignment));
        assertTrue(exception.getMessage().contains("grade object"));

        verify(contentObjectTopicService).delete(ORG_UNIT_ID, 50L);
        verify(contentObjectModuleService).delete(ORG_UNIT_ID, 40L);
        verify(dropboxFolderService).delete(ORG_UNIT_ID, 20L);
        verify(ltiAdvantageLinkService, never()).delete(any(), anyLong());
    }

    @Test
    void testDeleteAssignment_malformedMetadataJson_throwsUncheckedJacksonException() {
        // NOTE: metadata parsing (readValue/convertValue) is not wrapped in a try/catch here, so
        // malformed JSON surfaces as an unchecked tools.jackson.core.JacksonException rather than
        // the checked IOException the method signature promises.
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").metadata("not-json").build();

        assertThrows(JacksonException.class, () -> assignmentService.deleteAssignment(ORG_UNIT_ID, lmsAssignment));
    }

    /* ==================================== editAssignment =================================== */

    @Test
    void testEditAssignment_dropboxFolderNotFound_throwsIOException() throws IOException {
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").name("New Name").build();
        when(dropboxFolderService.get(ORG_UNIT_ID, 20L)).thenReturn(Optional.empty());

        assertThrows(IOException.class, () -> assignmentService.editAssignment(ORG_UNIT_ID, lmsAssignment));
    }

    @Test
    void testEditAssignment_dropboxFolderUpdateFails_throwsIOException() throws IOException {
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").name("New Name").build();
        when(dropboxFolderService.get(ORG_UNIT_ID, 20L)).thenReturn(Optional.of(DropboxFolder.builder().id(20L).name("Old Name").build()));
        when(dropboxFolderService.update(eq(ORG_UNIT_ID), eq(20L), any(DropboxFolderUpdate.class))).thenReturn(Optional.empty());

        assertThrows(IOException.class, () -> assignmentService.editAssignment(ORG_UNIT_ID, lmsAssignment));
    }

    @Test
    void testEditAssignment_blankMetadata_updatesOnlyDropboxFolderAndReturnsEarly() throws IOException {
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").name("New Name").build(); // metadata is null
        when(dropboxFolderService.get(ORG_UNIT_ID, 20L)).thenReturn(Optional.of(DropboxFolder.builder().id(20L).name("Old Name").build()));
        when(dropboxFolderService.update(eq(ORG_UNIT_ID), eq(20L), any(DropboxFolderUpdate.class))).thenReturn(Optional.of(DropboxFolder.builder().id(20L).name("New Name").build()));

        Optional<AssignmentExtended> result = assignmentService.editAssignment(ORG_UNIT_ID, lmsAssignment);

        assertTrue(result.isPresent());
        assertEquals("New Name", result.get().getAssignment().getDropboxFolder().getName());
        verifyNoInteractions(contentObjectTopicService, contentObjectModuleService, gradeObjectService, ltiAdvantageLinkService);
    }

    @Test
    void testEditAssignment_fullMetadata_updatesAllComponents() throws IOException {
        String name = "Renamed/Assignment";
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").name(name).metadata(fullMetadataJson()).build();
        stubEditPrerequisitesSuccessfully(name);

        Optional<AssignmentExtended> result = assignmentService.editAssignment(ORG_UNIT_ID, lmsAssignment);

        assertTrue(result.isPresent());
        Assignment updatedAssignment = result.get().getAssignment();
        assertEquals(name, updatedAssignment.getDropboxFolder().getName());
        // NOTE: for the content topic & content module, the PUT response is discarded; the
        // returned object is populated from the earlier GET result instead (per the code's own
        // "NOTE" comments), so the pre-update title is what ends up on the returned assignment
        // even though the remote resource itself was renamed.
        assertEquals("Old Topic", updatedAssignment.getContentObjectTopicLtiLink().getTitle());
        assertEquals("Old Module", updatedAssignment.getContentObjectModule().getTitle());
        // grade object & lti advantage link use the UPDATE result, so they do reflect the rename
        assertEquals("Renamed_Assignment", updatedAssignment.getGradeObject().getName());
        assertEquals(name, updatedAssignment.getLtiAdvantageLink().getName());

        // the grade object update uses the sanitized name (brightspaceLmsUtils.sanitize replaces "/")
        ArgumentCaptor<GradeObjectUpdate> gradeUpdateCaptor = ArgumentCaptor.forClass(GradeObjectUpdate.class);
        verify(gradeObjectService).update(eq(ORG_UNIT_ID), eq(10L), gradeUpdateCaptor.capture());
        assertEquals("Renamed_Assignment", gradeUpdateCaptor.getValue().getName());

        // whereas the dropbox folder / content topic / content module / lti link updates use the raw name
        ArgumentCaptor<DropboxFolderUpdate> folderUpdateCaptor = ArgumentCaptor.forClass(DropboxFolderUpdate.class);
        verify(dropboxFolderService).update(eq(ORG_UNIT_ID), eq(20L), folderUpdateCaptor.capture());
        assertEquals(name, folderUpdateCaptor.getValue().getName());
    }

    @Test
    void testEditAssignment_contentTopicNotFound_throwsIOException() throws IOException {
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").name("Renamed").metadata(fullMetadataJson()).build();
        when(dropboxFolderService.get(ORG_UNIT_ID, 20L)).thenReturn(Optional.of(DropboxFolder.builder().id(20L).build()));
        when(dropboxFolderService.update(eq(ORG_UNIT_ID), eq(20L), any(DropboxFolderUpdate.class))).thenReturn(Optional.of(DropboxFolder.builder().id(20L).name("Renamed").build()));
        when(contentObjectTopicService.get(ORG_UNIT_ID, 50L)).thenReturn(Optional.empty());

        assertThrows(IOException.class, () -> assignmentService.editAssignment(ORG_UNIT_ID, lmsAssignment));
    }

    @Test
    void testEditAssignment_contentModuleNotFound_throwsIOException() throws IOException {
        String name = "Renamed";
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").name(name).metadata(fullMetadataJson()).build();
        stubEditPrerequisitesSuccessfully(name);
        when(contentObjectModuleService.get(ORG_UNIT_ID, 40L)).thenReturn(Optional.empty());

        assertThrows(IOException.class, () -> assignmentService.editAssignment(ORG_UNIT_ID, lmsAssignment));
    }

    @Test
    void testEditAssignment_gradeObjectNotFound_throwsIOException() throws IOException {
        String name = "Renamed";
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").name(name).metadata(fullMetadataJson()).build();
        stubEditPrerequisitesSuccessfully(name);
        when(gradeObjectService.get(ORG_UNIT_ID, 10L)).thenReturn(Optional.empty());

        assertThrows(IOException.class, () -> assignmentService.editAssignment(ORG_UNIT_ID, lmsAssignment));
    }

    @Test
    void testEditAssignment_gradeObjectUpdateFails_throwsIOException() throws IOException {
        // unlike content topic/module (whose update result is discarded), grade object update
        // IS checked with orElseThrow
        String name = "Renamed";
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").name(name).metadata(fullMetadataJson()).build();
        stubEditPrerequisitesSuccessfully(name);
        when(gradeObjectService.update(eq(ORG_UNIT_ID), eq(10L), any(GradeObjectUpdate.class))).thenReturn(Optional.empty());

        assertThrows(IOException.class, () -> assignmentService.editAssignment(ORG_UNIT_ID, lmsAssignment));
    }

    @Test
    void testEditAssignment_ltiAdvantageLinkNotFound_throwsIOException() throws IOException {
        String name = "Renamed";
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").name(name).metadata(fullMetadataJson()).build();
        stubEditPrerequisitesSuccessfully(name);
        when(ltiAdvantageLinkService.get(ORG_UNIT_ID, 30L)).thenReturn(Optional.empty());

        assertThrows(IOException.class, () -> assignmentService.editAssignment(ORG_UNIT_ID, lmsAssignment));
    }

    @Test
    void testEditAssignment_ltiAdvantageLinkUpdateFails_throwsIOException() throws IOException {
        String name = "Renamed";
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").name(name).metadata(fullMetadataJson()).build();
        stubEditPrerequisitesSuccessfully(name);
        when(ltiAdvantageLinkService.update(eq(ORG_UNIT_ID), eq(30L), any(LtiAdvantageLinkUpdate.class))).thenReturn(Optional.empty());

        assertThrows(IOException.class, () -> assignmentService.editAssignment(ORG_UNIT_ID, lmsAssignment));
    }

    @Test
    void testEditAssignment_malformedMetadataJson_throwsUncheckedJacksonException() throws IOException {
        LmsAssignment lmsAssignment = LmsAssignment.builder().id("20").name("Renamed").metadata("not-json").build();
        when(dropboxFolderService.get(ORG_UNIT_ID, 20L)).thenReturn(Optional.of(DropboxFolder.builder().id(20L).build()));
        when(dropboxFolderService.update(eq(ORG_UNIT_ID), eq(20L), any(DropboxFolderUpdate.class))).thenReturn(Optional.of(DropboxFolder.builder().id(20L).name("Renamed").build()));

        assertThrows(JacksonException.class, () -> assignmentService.editAssignment(ORG_UNIT_ID, lmsAssignment));
    }

    /* ==================================== test helpers ===================================== */

    private String fullMetadataJson() {
        return "{\"brightspace\":{\"contentModuleId\":40,\"contentTopicId\":50,\"dropboxFolderId\":20,"
            + "\"dueAt\":\"2026-01-15T00:00:00.000Z\",\"lockAt\":\"2026-02-01T00:00:00.000Z\","
            + "\"gradeObjectId\":10,\"ltiAdvantageLinkId\":30,\"ltiAdvantageQuickLinkId\":31,"
            + "\"unlockAt\":\"2026-01-01T00:00:00.000Z\"}}";
    }

    private LmsAssignment fullMetadataLmsAssignment() {
        return LmsAssignment.builder()
            .id("20")
            .name("Assignment 1")
            .metadata(fullMetadataJson())
            .build();
    }

    /**
     * Stubs dropboxFolderService/contentObjectTopicService/contentObjectModuleService/
     * gradeObjectService/ltiAdvantageLinkService get+update calls to succeed, matching the ids
     * encoded in {@link #fullMetadataJson()}. Individual tests can override one specific stub
     * (e.g. to Optional.empty()) after calling this to exercise a single failure branch.
     */
    private void stubEditPrerequisitesSuccessfully(String name) throws IOException {
        when(dropboxFolderService.get(ORG_UNIT_ID, 20L)).thenReturn(Optional.of(DropboxFolder.builder().id(20L).name("Old Name").build()));
        when(dropboxFolderService.update(eq(ORG_UNIT_ID), eq(20L), any(DropboxFolderUpdate.class))).thenReturn(Optional.of(DropboxFolder.builder().id(20L).name(name).build()));

        when(contentObjectTopicService.get(ORG_UNIT_ID, 50L)).thenReturn(Optional.of(ContentObjectTopic.builder().id(50L).title("Old Topic").type(1).topicType(1).build()));
        when(contentObjectTopicService.update(eq(ORG_UNIT_ID), eq(50L), any(ContentObjectTopicUpdate.class))).thenReturn(Optional.of(ContentObjectTopic.builder().id(50L).title(name).build()));

        when(contentObjectModuleService.get(ORG_UNIT_ID, 40L)).thenReturn(Optional.of(ContentObjectModule.builder().id(40L).title("Old Module").build()));
        when(contentObjectModuleService.update(eq(ORG_UNIT_ID), eq(40L), any(ContentObjectModuleUpdate.class))).thenReturn(Optional.of(ContentObjectModule.builder().id(40L).title(name).build()));

        when(gradeObjectService.get(ORG_UNIT_ID, 10L)).thenReturn(Optional.of(GradeObject.builder().id(10L).name("Old Grade").build()));
        when(gradeObjectService.update(eq(ORG_UNIT_ID), eq(10L), any(GradeObjectUpdate.class))).thenReturn(Optional.of(GradeObject.builder().id(10L).name("Renamed_Assignment").build()));

        when(ltiAdvantageLinkService.get(ORG_UNIT_ID, 30L)).thenReturn(Optional.of(LtiAdvantageLink.builder().linkId(30L).name("Old Link").build()));
        when(ltiAdvantageLinkService.update(eq(ORG_UNIT_ID), eq(30L), any(LtiAdvantageLinkUpdate.class))).thenReturn(Optional.of(LtiAdvantageLink.builder().linkId(30L).name(name).build()));
    }

}
