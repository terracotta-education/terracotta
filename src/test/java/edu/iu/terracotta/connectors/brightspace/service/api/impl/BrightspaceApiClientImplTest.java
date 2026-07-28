package edu.iu.terracotta.connectors.brightspace.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.brightspace.dao.model.extended.AssignmentExtended;
import edu.iu.terracotta.connectors.brightspace.dao.model.extended.UserExtended;
import edu.iu.terracotta.connectors.brightspace.io.exception.ObjectNotFoundException;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.AssignmentReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.AssignmentWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ClasslistUserReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectModuleReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectTopicReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.GradeObjectReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageLinkReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.UserGradeValueReaderService;
import edu.iu.terracotta.connectors.brightspace.io.model.Assignment;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectModule;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectTopic;
import edu.iu.terracotta.connectors.brightspace.io.model.DropboxFolder;
import edu.iu.terracotta.connectors.brightspace.io.model.GradeObject;
import edu.iu.terracotta.connectors.brightspace.io.model.GradeValue;
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageLink;
import edu.iu.terracotta.connectors.brightspace.io.model.User;
import edu.iu.terracotta.connectors.brightspace.io.model.UserGradeValue;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import edu.iu.terracotta.connectors.brightspace.service.io.impl.BrightspaceApiFactory;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsConversation;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFile;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetUsersInCourseOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentType;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;

/**
 * NOTE on strategy: {@code BrightspaceApiClientImpl} never receives a {@code BrightspaceApiFactory}
 * through its constructor; every reader/writer lookup does {@code new BrightspaceApiFactory(...)}
 * internally. We intercept those constructions with Mockito's {@code mockConstruction} (used
 * elsewhere in this codebase, e.g. {@code IntegrationServiceImplTest}) so every {@code getReader}/
 * {@code getWriter} call resolves to one of the interface mocks declared below.
 */
public class BrightspaceApiClientImplTest extends BaseTest {

    @Mock private AssignmentReaderService assignmentReaderService;
    @Mock private ClasslistUserReaderService classlistUserReaderService;
    @Mock private ContentObjectModuleReaderService contentObjectModuleReaderService;
    @Mock private ContentObjectTopicReaderService contentObjectTopicReaderService;
    @Mock private GradeObjectReaderService gradeObjectReaderService;
    @Mock private LtiAdvantageLinkReaderService ltiAdvantageLinkReaderService;
    @Mock private UserGradeValueReaderService userGradeValueReaderService;

    private BrightspaceApiClientImpl brightspaceApiClient;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        // constructed manually (not via @InjectMocks): the constructor params are the concrete
        // Impl classes, which map 1:1 onto the inherited base mocks, but manual construction keeps
        // this test immune to future @InjectMocks type-matching surprises (see class Javadoc pitfalls).
        brightspaceApiClient = new BrightspaceApiClientImpl(brightspaceAdvantageAgsService, brightspaceLmsOAuthService, brightspaceLmsUtils);
        brightspaceApiClient.init();
    }

    private MockedConstruction<BrightspaceApiFactory> mockApiFactory() {
        return mockConstruction(
            BrightspaceApiFactory.class,
            (mock, context) -> {
                when(mock.getReader(eq(AssignmentReaderService.class), any(OauthToken.class))).thenReturn(assignmentReaderService);
                when(mock.getReader(eq(ClasslistUserReaderService.class), any(OauthToken.class))).thenReturn(classlistUserReaderService);
                when(mock.getReader(eq(ContentObjectModuleReaderService.class), any(OauthToken.class))).thenReturn(contentObjectModuleReaderService);
                when(mock.getReader(eq(ContentObjectTopicReaderService.class), any(OauthToken.class))).thenReturn(contentObjectTopicReaderService);
                when(mock.getReader(eq(GradeObjectReaderService.class), any(OauthToken.class))).thenReturn(gradeObjectReaderService);
                when(mock.getReader(eq(LtiAdvantageLinkReaderService.class), any(OauthToken.class))).thenReturn(ltiAdvantageLinkReaderService);
                when(mock.getReader(eq(UserGradeValueReaderService.class), any(OauthToken.class))).thenReturn(userGradeValueReaderService);
                when(mock.getWriter(eq(AssignmentWriterService.class), any(OauthToken.class))).thenReturn(brightspaceAssignmentWriterService);
            }
        );
    }

    private Assignment buildBrightspaceAssignment(boolean published, Long gradeItemId) {
        return Assignment.builder()
            .dropboxFolder(
                DropboxFolder.builder()
                    .id(500L)
                    .isHidden(!published)
                    .gradeItemId(gradeItemId)
                    .submissionType(1)
                    .build()
            )
            .build();
    }

    private AssignmentExtended buildAssignmentExtended(boolean published, Long gradeItemId) {
        return AssignmentExtended.builder()
            .assignment(buildBrightspaceAssignment(published, gradeItemId))
            .build();
    }

    private UserGradeValue buildUserGradeValue(String pointsNumerator) {
        return UserGradeValue.builder()
            .user(
                User.builder()
                    .identifier("user1")
                    .userName("user1username")
                    .displayName("User One")
                    .build()
            )
            .gradeValue(
                GradeValue.builder()
                    .pointsNumerator(Double.valueOf(pointsNumerator))
                    .build()
            )
            .build();
    }

    private String metadataJson(long contentModuleId, long contentTopicId, long ltiAdvantageLinkId, long gradeObjectId) {
        return String.format(
            "{\"brightspace\":{\"contentModuleId\":%d,\"contentTopicId\":%d,\"ltiAdvantageLinkId\":%d,\"gradeObjectId\":%d}}",
            contentModuleId, contentTopicId, ltiAdvantageLinkId, gradeObjectId
        );
    }

    /* createLmsAssignment */

    @Test
    public void testCreateLmsAssignmentSuccess() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            AssignmentExtended result = brightspaceApiClient.createLmsAssignment(ltiUserEntity, assignment, "orgSourcedId");

            assertSame(brightspaceAssignmentExtended, result);
        }
    }

    @Test
    public void testCreateLmsAssignmentThrowsApiExceptionWhenWriterReturnsEmpty() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(brightspaceAssignmentWriterService.createAssignment(anyString(), any(Assignment.class))).thenReturn(Optional.empty());

            ApiException exception = assertThrows(ApiException.class, () -> brightspaceApiClient.createLmsAssignment(ltiUserEntity, assignment, "orgSourcedId"));

            assertTrue(exception.getMessage().contains("Failed to create Assignment"));
        }
    }

    @Test
    public void testCreateLmsAssignmentNullExposureThrowsRawNpeInsteadOfApiException() {
        // BUG: orgUnitId is dereferenced from assignment.getExposure() OUTSIDE the try/catch in the
        // production method, so a broken exposure chain surfaces as an unchecked NullPointerException
        // instead of the documented ApiException.
        when(assignment.getExposure()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> brightspaceApiClient.createLmsAssignment(ltiUserEntity, assignment, "orgSourcedId"));
    }

    @Test
    public void testCreateLmsAssignmentSwallowsConnectionExceptionDuringLineItemCreation() throws Exception {
        when(brightspaceAdvantageAgsService.getToken(any(), any())).thenThrow(new ConnectionException("down"));

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            AssignmentExtended result = brightspaceApiClient.createLmsAssignment(ltiUserEntity, assignment, "orgSourcedId");

            assertSame(brightspaceAssignmentExtended, result);
        }
    }

    /* restoreAssignment */

    @Test
    public void testRestoreAssignmentReusesExistingRelatedEntities() throws Exception {
        when(assignment.getMetadata()).thenReturn(metadataJson(10L, 20L, 30L, 40L));
        when(assignment.isPublished()).thenReturn(true);

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(contentObjectTopicReaderService.get(anyString(), eq(20L))).thenReturn(Optional.of(ContentObjectTopic.builder().id(20L).build()));
            when(ltiAdvantageLinkReaderService.get(anyString(), eq(30L))).thenReturn(Optional.of(LtiAdvantageLink.builder().linkId(30L).build()));
            when(gradeObjectReaderService.get(anyString(), eq(40L))).thenReturn(Optional.of(GradeObject.builder().id(40L).build()));
            when(brightspaceAdvantageAgsService.getLineItem(any(), any(), any())).thenReturn(lineItem);

            AssignmentExtended result = brightspaceApiClient.restoreAssignment(assignment);

            assertSame(brightspaceAssignmentExtended, result);
            verify(contentObjectModuleReaderService, never()).get(anyString(), anyLong());
        }
    }

    @Test
    public void testRestoreAssignmentCreatesNewRelatedEntitiesWhenNoneExist() throws Exception {
        when(assignment.getMetadata()).thenReturn(metadataJson(10L, 20L, 30L, 40L));
        when(assignment.isPublished()).thenReturn(false);

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(contentObjectTopicReaderService.get(anyString(), eq(20L))).thenReturn(Optional.empty());
            when(contentObjectModuleReaderService.get(anyString(), eq(10L))).thenReturn(Optional.empty());
            when(ltiAdvantageLinkReaderService.get(anyString(), eq(30L))).thenReturn(Optional.empty());
            when(gradeObjectReaderService.get(anyString(), eq(40L))).thenReturn(Optional.empty());

            AssignmentExtended result = brightspaceApiClient.restoreAssignment(assignment);

            assertSame(brightspaceAssignmentExtended, result);
            verify(contentObjectModuleReaderService).get(anyString(), eq(10L));
        }
    }

    @Test
    public void testRestoreAssignmentWrapsApiExceptionOnMalformedMetadata() {
        when(assignment.getMetadata()).thenReturn("not-valid-json");

        ApiException exception = assertThrows(ApiException.class, () -> brightspaceApiClient.restoreAssignment(assignment));

        assertTrue(exception.getMessage().contains("Failed to create Assignment"));
    }

    @Test
    public void testRestoreAssignmentThrowsApiExceptionWhenWriterReturnsEmpty() throws Exception {
        when(assignment.getMetadata()).thenReturn(metadataJson(10L, 20L, 30L, 40L));

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(contentObjectTopicReaderService.get(anyString(), anyLong())).thenReturn(Optional.empty());
            when(contentObjectModuleReaderService.get(anyString(), anyLong())).thenReturn(Optional.empty());
            when(ltiAdvantageLinkReaderService.get(anyString(), anyLong())).thenReturn(Optional.empty());
            when(gradeObjectReaderService.get(anyString(), anyLong())).thenReturn(Optional.empty());
            when(brightspaceAssignmentWriterService.createAssignment(anyString(), any(Assignment.class))).thenReturn(Optional.empty());

            assertThrows(ApiException.class, () -> brightspaceApiClient.restoreAssignment(assignment));
        }
    }

    /* listAssignments overloads */

    @Test
    public void testListAssignmentsByLtiContextSuccess() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.listCourseAssignments(anyString())).thenReturn(List.of(buildAssignmentExtended(true, 1L)));

            List<LmsAssignment> result = brightspaceApiClient.listAssignments(ltiUserEntity, ltiContextEntity);

            assertEquals(1, result.size());
        }
    }

    @Test
    public void testListAssignmentsByLtiContextWrapsException() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.listCourseAssignments(anyString())).thenThrow(new IOException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.listAssignments(ltiUserEntity, ltiContextEntity));
        }
    }

    @Test
    public void testListAssignmentsByExperimentSuccess() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.listCourseAssignments(anyString())).thenReturn(List.of(buildAssignmentExtended(true, 1L)));

            List<LmsAssignment> result = brightspaceApiClient.listAssignments(ltiUserEntity, experiment);

            assertEquals(1, result.size());
        }
    }

    @Test
    public void testListAssignmentsByExperimentWrapsException() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.listCourseAssignments(anyString())).thenThrow(new IOException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.listAssignments(ltiUserEntity, experiment));
        }
    }

    @Test
    public void testListAssignmentsByPlatformDeploymentAndTokenOverrideSuccess() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.listCourseAssignments(anyString())).thenReturn(List.of(buildAssignmentExtended(true, 1L)));

            List<LmsAssignment> result = brightspaceApiClient.listAssignments(platformDeployment, "orgUnitId", "override-token");

            assertEquals(1, result.size());
        }
    }

    @Test
    public void testListAssignmentsByPlatformDeploymentAndTokenOverrideWrapsException() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.listCourseAssignments(anyString())).thenThrow(new IOException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.listAssignments(platformDeployment, "orgUnitId", "override-token"));
        }
    }

    /* listAssignment(apiUser, orgUnitId, assignmentId) */

    @Test
    public void testListAssignmentByIdSuccess() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(55L))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));

            Optional<LmsAssignment> result = brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", "55");

            assertTrue(result.isPresent());
            assertTrue(result.get().isPublished());
        }
    }

    @Test
    public void testListAssignmentByIdNotFoundReturnsEmpty() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(55L))).thenThrow(new ObjectNotFoundException());

            Optional<LmsAssignment> result = brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", "55");

            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testListAssignmentByIdWrapsGenericException() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(55L))).thenThrow(new IOException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", "55"));
        }
    }

    /* checkAssignmentExists */

    @Test
    public void testCheckAssignmentExistsSuccess() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(55L))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));

            Optional<LmsAssignment> result = brightspaceApiClient.checkAssignmentExists(ltiUserEntity, "55", "orgUnitId");

            assertTrue(result.isPresent());
        }
    }

    @Test
    public void testCheckAssignmentExistsNotFoundReturnsEmpty() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(55L))).thenThrow(new ObjectNotFoundException());

            Optional<LmsAssignment> result = brightspaceApiClient.checkAssignmentExists(ltiUserEntity, "55", "orgUnitId");

            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testCheckAssignmentExistsWrapsGenericException() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(55L))).thenThrow(new IOException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.checkAssignmentExists(ltiUserEntity, "55", "orgUnitId"));
        }
    }

    /* listAssignment(apiUser, lmsCourseId, Assignment) */

    @Test
    public void testListAssignmentByCourseReturnsEmptyWhenDelegateNotFound() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.empty());

            Optional<LmsAssignment> result = brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", assignment);

            assertTrue(result.isEmpty());
            verify(contentObjectModuleReaderService, never()).get(anyString(), anyLong());
        }
    }

    @Test
    public void testListAssignmentByCourseReturnsAsIsWhenNotPublished() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(false, 1L)));

            Optional<LmsAssignment> result = brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", assignment);

            assertTrue(result.isPresent());
            assertFalse(result.get().isPublished());
            verify(contentObjectModuleReaderService, never()).get(anyString(), anyLong());
        }
    }

    @Test
    public void testListAssignmentByCourseReturnsAsIsWhenMetadataBlank() throws Exception {
        when(assignment.getMetadata()).thenReturn("");

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));

            Optional<LmsAssignment> result = brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", assignment);

            assertTrue(result.isPresent());
            assertTrue(result.get().isPublished());
            verify(contentObjectModuleReaderService, never()).get(anyString(), anyLong());
        }
    }

    @Test
    public void testListAssignmentByCourseUnpublishesWhenModuleHidden() throws Exception {
        when(assignment.getMetadata()).thenReturn(metadataJson(10L, 20L, 30L, 40L));

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));
            when(contentObjectModuleReaderService.get(anyString(), eq(10L))).thenReturn(Optional.of(ContentObjectModule.builder().isHidden(true).build()));

            Optional<LmsAssignment> result = brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", assignment);

            assertFalse(result.get().isPublished());
            verify(contentObjectTopicReaderService, never()).get(anyString(), anyLong());
        }
    }

    @Test
    public void testListAssignmentByCourseUnpublishesWhenModuleMissing() throws Exception {
        when(assignment.getMetadata()).thenReturn(metadataJson(10L, 20L, 30L, 40L));

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));
            when(contentObjectModuleReaderService.get(anyString(), eq(10L))).thenReturn(Optional.empty());

            Optional<LmsAssignment> result = brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", assignment);

            assertFalse(result.get().isPublished());
        }
    }

    @Test
    public void testListAssignmentByCourseUnpublishesWhenTopicHidden() throws Exception {
        when(assignment.getMetadata()).thenReturn(metadataJson(10L, 20L, 30L, 40L));

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));
            when(contentObjectModuleReaderService.get(anyString(), eq(10L))).thenReturn(Optional.of(ContentObjectModule.builder().isHidden(false).build()));
            when(contentObjectTopicReaderService.get(anyString(), eq(20L))).thenReturn(Optional.of(ContentObjectTopic.builder().isHidden(true).build()));

            Optional<LmsAssignment> result = brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", assignment);

            assertFalse(result.get().isPublished());
        }
    }

    @Test
    public void testListAssignmentByCourseRemainsPublishedWhenModuleAndTopicVisible() throws Exception {
        when(assignment.getMetadata()).thenReturn(metadataJson(10L, 20L, 30L, 40L));

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));
            when(contentObjectModuleReaderService.get(anyString(), eq(10L))).thenReturn(Optional.of(ContentObjectModule.builder().isHidden(false).build()));
            when(contentObjectTopicReaderService.get(anyString(), eq(20L))).thenReturn(Optional.of(ContentObjectTopic.builder().isHidden(false).build()));

            Optional<LmsAssignment> result = brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", assignment);

            assertTrue(result.get().isPublished());
        }
    }

    @Test
    public void testListAssignmentByCourseWrapsApiExceptionWhenModuleReadFails() throws Exception {
        when(assignment.getMetadata()).thenReturn(metadataJson(10L, 20L, 30L, 40L));

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));
            when(contentObjectModuleReaderService.get(anyString(), eq(10L))).thenThrow(new IOException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", assignment));
        }
    }

    @Test
    public void testListAssignmentByCourseWrapsApiExceptionWhenTopicReadFails() throws Exception {
        when(assignment.getMetadata()).thenReturn(metadataJson(10L, 20L, 30L, 40L));

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));
            when(contentObjectModuleReaderService.get(anyString(), eq(10L))).thenReturn(Optional.of(ContentObjectModule.builder().isHidden(false).build()));
            when(contentObjectTopicReaderService.get(anyString(), eq(20L))).thenThrow(new IOException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.listAssignment(ltiUserEntity, "orgUnitId", assignment));
        }
    }

    /* editAssignment overloads */

    @Test
    public void testEditAssignmentSuccess() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(brightspaceAssignmentWriterService.editAssignment(anyString(), any(LmsAssignment.class))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));

            Optional<LmsAssignment> result = brightspaceApiClient.editAssignment(ltiUserEntity, lmsAssignment, "orgUnitId");

            assertTrue(result.isPresent());
        }
    }

    @Test
    public void testEditAssignmentWrapsException() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(brightspaceAssignmentWriterService.editAssignment(anyString(), any(LmsAssignment.class))).thenThrow(new IOException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.editAssignment(ltiUserEntity, lmsAssignment, "orgUnitId"));
        }
    }

    @Test
    public void testEditAssignmentWithTokenOverrideSuccess() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(brightspaceAssignmentWriterService.editAssignment(anyString(), any(LmsAssignment.class))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));

            Optional<LmsAssignment> result = brightspaceApiClient.editAssignment(platformDeployment, lmsAssignment, "orgUnitId", "override-token");

            assertTrue(result.isPresent());
        }
    }

    @Test
    public void testEditAssignmentWithTokenOverrideWrapsException() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(brightspaceAssignmentWriterService.editAssignment(anyString(), any(LmsAssignment.class))).thenThrow(new IOException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.editAssignment(platformDeployment, lmsAssignment, "orgUnitId", "override-token"));
        }
    }

    /* editAssignmentNameInLms */

    @Test
    public void testEditAssignmentNameInLmsLogsWarnAndReturnsWhenAlreadyDeleted() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> brightspaceApiClient.editAssignmentNameInLms(assignment, "orgUnitId", "newName", ltiUserEntity));

            verify(brightspaceAssignmentWriterService, never()).editAssignment(anyString(), any(LmsAssignment.class));
        }
    }

    @Test
    public void testEditAssignmentNameInLmsUpdatesNameAndDelegates() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));
            when(brightspaceAssignmentWriterService.editAssignment(anyString(), any(LmsAssignment.class))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));

            brightspaceApiClient.editAssignmentNameInLms(assignment, "orgUnitId", "newName", ltiUserEntity);

            verify(brightspaceAssignmentWriterService).editAssignment(eq("orgUnitId"), argThat(lms -> "newName".equals(lms.getName())));
        }
    }

    /* deleteAssignmentInLms overloads */

    @Test
    public void testDeleteAssignmentInLmsByAssignmentLogsWarnWhenAlreadyDeleted() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> brightspaceApiClient.deleteAssignmentInLms(assignment, "orgUnitId", ltiUserEntity));

            verify(brightspaceAssignmentWriterService, never()).deleteAssignment(anyString(), any(LmsAssignment.class));
        }
    }

    @Test
    public void testDeleteAssignmentInLmsByAssignmentDelegatesWhenPresent() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 1L)));
            when(brightspaceAssignmentWriterService.deleteAssignment(anyString(), any(LmsAssignment.class))).thenReturn(Optional.empty());

            brightspaceApiClient.deleteAssignmentInLms(assignment, "orgUnitId", ltiUserEntity);

            verify(brightspaceAssignmentWriterService).deleteAssignment(eq("orgUnitId"), any(LmsAssignment.class));
        }
    }

    @Test
    public void testDeleteAssignmentInLmsByLmsAssignmentSuccess() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(brightspaceAssignmentWriterService.deleteAssignment(anyString(), any(LmsAssignment.class))).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> brightspaceApiClient.deleteAssignmentInLms(lmsAssignment, "orgUnitId", ltiUserEntity));
        }
    }

    @Test
    public void testDeleteAssignmentInLmsByLmsAssignmentWrapsException() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(brightspaceAssignmentWriterService.deleteAssignment(anyString(), any(LmsAssignment.class))).thenThrow(new IOException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.deleteAssignmentInLms(lmsAssignment, "orgUnitId", ltiUserEntity));
        }
    }

    /* uploadConsentFile */

    @Test
    public void testUploadConsentFileSuccess() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            AssignmentExtended result = brightspaceApiClient.uploadConsentFile(experiment, consentDocument, ltiUserEntity);

            assertSame(brightspaceAssignmentExtended, result);
        }
    }

    @Test
    public void testUploadConsentFileWrapsExceptionWhenWriterReturnsEmpty() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(brightspaceAssignmentWriterService.createAssignment(anyString(), any(Assignment.class))).thenReturn(Optional.empty());

            assertThrows(ApiException.class, () -> brightspaceApiClient.uploadConsentFile(experiment, consentDocument, ltiUserEntity));
        }
    }

    /* listUsersForCourse */

    @Test
    public void testListUsersForCourseSuccess() throws Exception {
        LmsGetUsersInCourseOptions options = LmsGetUsersInCourseOptions.builder()
            .lmsCourseId("orgUnitId")
            .enrollmentType(List.of(EnrollmentType.STUDENT))
            .build();

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(classlistUserReaderService.getAll(anyString(), eq(true), isNull())).thenReturn(List.of(UserExtended.builder().build()));

            List<LmsUser> result = brightspaceApiClient.listUsersForCourse(options, ltiUserEntity);

            assertEquals(1, result.size());
        }
    }

    @Test
    public void testListUsersForCourseWrapsException() throws Exception {
        LmsGetUsersInCourseOptions options = LmsGetUsersInCourseOptions.builder()
            .lmsCourseId("orgUnitId")
            .enrollmentType(List.of(EnrollmentType.STUDENT))
            .build();

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(classlistUserReaderService.getAll(anyString(), eq(true), isNull())).thenThrow(new RuntimeException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.listUsersForCourse(options, ltiUserEntity));
        }
    }

    /* resyncAssignmentTargetUrisInLms (no-op for Brightspace) */

    @Test
    public void testResyncAssignmentTargetUrisInLmsIsNoOp() {
        assertDoesNotThrow(
            () -> brightspaceApiClient.resyncAssignmentTargetUrisInLms(platformDeployment, ltiUserEntity, 1L, "token", List.of("1"), List.of("2"), List.of("1", "2"))
        );
    }

    /* updateAssignmentMetadata */

    @Test
    public void testUpdateAssignmentMetadataUpdatesDatesWhenKeyPresent() throws Exception {
        when(assignment.getMetadata()).thenReturn(metadataJson(10L, 20L, 30L, 40L));
        when(lmsAssignment.getDueAt()).thenReturn(new Date());

        brightspaceApiClient.updateAssignmentMetadata(assignment, lmsAssignment);

        verify(assignment).setMetadata(contains("dueAt"));
    }

    @Test
    public void testUpdateAssignmentMetadataNoOpWhenKeyMissing() throws Exception {
        when(assignment.getMetadata()).thenReturn("{\"canvas\":{\"id\":1}}");

        brightspaceApiClient.updateAssignmentMetadata(assignment, lmsAssignment);

        verify(assignment, never()).setMetadata(anyString());
    }

    @Test
    public void testUpdateAssignmentMetadataLogsAndSwallowsMalformedJson() {
        when(assignment.getMetadata()).thenReturn("not-json");

        assertDoesNotThrow(() -> brightspaceApiClient.updateAssignmentMetadata(assignment, lmsAssignment));

        verify(assignment, never()).setMetadata(anyString());
    }

    /* listCoursesForUser (not used by Brightspace) */

    @Test
    public void testListCoursesForUserReturnsEmptyList() throws Exception {
        List<LmsCourse> result = brightspaceApiClient.listCoursesForUser(platformDeployment, "userId", "token");

        assertTrue(result.isEmpty());
    }

    /* listSubmissions overloads */

    @Test
    public void testListSubmissionsByOutcomeDelegatesToLmsAssignmentIdOverload() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 999L)));
            when(userGradeValueReaderService.getAll(anyString(), eq("999"))).thenReturn(List.of(buildUserGradeValue("50")));

            List<LmsSubmission> result = brightspaceApiClient.listSubmissions(ltiUserEntity, outcome, "orgUnitId");

            assertEquals(1, result.size());
        }
    }

    @Test
    public void testListSubmissionsThrowsApiExceptionWhenAssignmentNotFound() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.empty());

            assertThrows(ApiException.class, () -> brightspaceApiClient.listSubmissions(ltiUserEntity, "1", "orgUnitId"));
        }
    }

    @Test
    public void testListSubmissionsReturnsEmptyWhenNoGradeItem() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, null)));

            List<LmsSubmission> result = brightspaceApiClient.listSubmissions(ltiUserEntity, "1", "orgUnitId");

            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testListSubmissionsSuccess() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 999L)));
            when(userGradeValueReaderService.getAll(anyString(), eq("999"))).thenReturn(List.of(buildUserGradeValue("88")));

            List<LmsSubmission> result = brightspaceApiClient.listSubmissions(ltiUserEntity, "1", "orgUnitId");

            assertEquals(1, result.size());
            assertEquals(Double.valueOf(88D), result.get(0).getScore());
        }
    }

    @Test
    public void testListSubmissionsWrapsExceptionFromUserGradeValueLookup() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(anyString(), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 999L)));
            when(userGradeValueReaderService.getAll(anyString(), eq("999"))).thenThrow(new IOException("boom"));

            assertThrows(ApiException.class, () -> brightspaceApiClient.listSubmissions(ltiUserEntity, "1", "orgUnitId"));
        }
    }

    /* addLmsExtensions (no-op for Brightspace) */

    @Test
    public void testAddLmsExtensionsIsNoOp() {
        assertDoesNotThrow(() -> brightspaceApiClient.addLmsExtensions(null, submission, true));
    }

    /* listSubmissionsForMultipleAssignments */

    @Test
    public void testListSubmissionsForMultipleAssignmentsAggregatesAcrossIds() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(eq("orgUnitId"), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 100L)));
            when(assignmentReaderService.getSingleAssignment(eq("orgUnitId"), eq(2L))).thenReturn(Optional.of(buildAssignmentExtended(true, 200L)));
            when(userGradeValueReaderService.getAll(eq("orgUnitId"), eq("100"))).thenReturn(List.of(buildUserGradeValue("10")));
            when(userGradeValueReaderService.getAll(eq("orgUnitId"), eq("200"))).thenReturn(List.of(buildUserGradeValue("20"), buildUserGradeValue("30")));

            List<LmsSubmission> result = brightspaceApiClient.listSubmissionsForMultipleAssignments(ltiUserEntity, "orgUnitId", List.of("1", "2"));

            assertEquals(3, result.size());
        }
    }

    @Test
    public void testListSubmissionsForMultipleAssignmentsSkipsFailingAssignmentsAndContinues() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.getSingleAssignment(eq("orgUnitId"), eq(1L))).thenReturn(Optional.of(buildAssignmentExtended(true, 100L)));
            when(userGradeValueReaderService.getAll(eq("orgUnitId"), eq("100"))).thenReturn(List.of(buildUserGradeValue("10")));

            // "not-a-number" blows up Long.parseLong() with an unchecked NumberFormatException inside
            // listSubmissions(); this asserts the per-item catch(Exception) in the multi-assignment
            // fan-out swallows it and still returns results for the id that succeeded.
            List<LmsSubmission> result = brightspaceApiClient.listSubmissionsForMultipleAssignments(ltiUserEntity, "orgUnitId", List.of("1", "not-a-number"));

            assertEquals(1, result.size());
        }
    }

    /* sendConversation / getConversation / getFile / getFiles (all no-ops for Brightspace) */

    @Test
    public void testSendConversationReturnsEmptyList() throws Exception {
        List<LmsConversation> result = brightspaceApiClient.sendConversation(null, ltiUserEntity);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetConversationReturnsEmptyOptional() throws Exception {
        Optional<LmsConversation> result = brightspaceApiClient.getConversation(null, ltiUserEntity);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFileReturnsEmptyOptional() throws Exception {
        Optional<LmsFile> result = brightspaceApiClient.getFile(ltiUserEntity, "fileId");

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFilesReturnsEmptyList() throws Exception {
        List<LmsFile> result = brightspaceApiClient.getFiles(ltiUserEntity);

        assertTrue(result.isEmpty());
    }

    /* getAccessToken branches (exercised indirectly through listAssignments) */

    @Test
    public void testGetAccessTokenUsesAdminApiTokenWhenOAuthNotConfigured() throws Exception {
        when(brightspaceLmsOAuthService.isConfigured(any(PlatformDeployment.class))).thenReturn(false);
        when(platformDeployment.getApiToken()).thenReturn("admin-token");

        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.listCourseAssignments(anyString())).thenReturn(List.of(buildAssignmentExtended(true, 1L)));

            List<LmsAssignment> result = brightspaceApiClient.listAssignments(ltiUserEntity, ltiContextEntity);

            assertEquals(1, result.size());
        }
    }

    @Test
    public void testGetAccessTokenThrowsApiExceptionWhenNoOAuthAndNoAdminToken() {
        when(brightspaceLmsOAuthService.isConfigured(any(PlatformDeployment.class))).thenReturn(false);
        when(platformDeployment.getApiToken()).thenReturn(null);

        ApiException exception = assertThrows(ApiException.class, () -> brightspaceApiClient.listAssignments(ltiUserEntity, ltiContextEntity));

        assertNotNull(exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Could not get a Brightspace API token"));
    }

    @Test
    public void testGetAccessTokenWrapsLmsOAuthException() throws Exception {
        when(brightspaceLmsOAuthService.getAccessToken(any(LtiUserEntity.class))).thenThrow(new LmsOAuthException("expired"));

        ApiException exception = assertThrows(ApiException.class, () -> brightspaceApiClient.listAssignments(ltiUserEntity, ltiContextEntity));

        assertNotNull(exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Could not get a Brightspace API token"));
    }

    @Test
    public void testGetAccessTokenUsesTokenOverrideSkipsOAuthLookup() throws Exception {
        try (MockedConstruction<BrightspaceApiFactory> _ = mockApiFactory()) {
            when(assignmentReaderService.listCourseAssignments(anyString())).thenReturn(List.of());

            brightspaceApiClient.listAssignments(platformDeployment, "orgUnitId", "override-token");

            verify(brightspaceLmsOAuthService, never()).isConfigured(any());
            verify(brightspaceLmsOAuthService, never()).getAccessToken(any());
        }
    }

}
