package edu.iu.terracotta.connectors.oneedtech.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsConversation;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFile;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.oneedtech.dao.model.extended.AssignmentExtended;
import edu.iu.terracotta.connectors.oneedtech.service.lti.advantage.impl.OneEdTechAdvantageAgsServiceImpl;

public class OneEdTechApiClientImplTest extends BaseTest {

    // OneEdTechAdvantageAgsServiceImpl (the concrete class, not the AdvantageAgsService interface) is
    // the constructor dependency here. It is not declared anywhere in the
    // BaseModelTest/BaseRepositoryTest/BaseServiceTest hierarchy (only the AdvantageAgsService interface
    // and CanvasAdvantageAgsServiceImpl/BrightspaceAdvantageAgsServiceImpl are), so it must be mocked
    // locally or @InjectMocks would wire null for it.
    @Mock private OneEdTechAdvantageAgsServiceImpl oneEdTechAdvantageAgsService;

    @InjectMocks private OneEdTechApiClientImpl oneEdTechApiClient;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testCreateLmsAssignmentSuccess() throws ConnectionException, ApiException, TerracottaConnectorException {
        LineItem response = LineItem.builder()
            .id("lineItemId")
            .label("label")
            .scoreMaximum(100F)
            .build();
        when(oneEdTechAdvantageAgsService.getToken(any(), any())).thenReturn(ltiToken);
        when(oneEdTechAdvantageAgsService.postLineItem(any(LtiToken.class), any(), any(LineItem.class))).thenReturn(response);

        AssignmentExtended result = oneEdTechApiClient.createLmsAssignment(ltiUserEntity, assignment, "lmsCourseId");

        assertNotNull(result);
        assertEquals("lineItemId", result.getId());
        assertEquals("label", result.getName());
        verify(oneEdTechAdvantageAgsService).getToken(null, platformDeployment);
    }

    @Test
    public void testCreateLmsAssignmentGetTokenThrowsConnectionException() throws ConnectionException {
        when(oneEdTechAdvantageAgsService.getToken(any(), any())).thenThrow(new ConnectionException("error getting token"));

        assertThrows(TerracottaConnectorException.class, () -> oneEdTechApiClient.createLmsAssignment(ltiUserEntity, assignment, "lmsCourseId"));
    }

    @Test
    public void testCreateLmsAssignmentPostLineItemThrowsConnectionException() throws ConnectionException {
        when(oneEdTechAdvantageAgsService.getToken(any(), any())).thenReturn(ltiToken);
        when(oneEdTechAdvantageAgsService.postLineItem(any(LtiToken.class), any(), any(LineItem.class))).thenThrow(new ConnectionException("error posting line item"));

        assertThrows(TerracottaConnectorException.class, () -> oneEdTechApiClient.createLmsAssignment(ltiUserEntity, assignment, "lmsCourseId"));
    }

    @Test
    public void testListAssignmentsByLtiContext() throws ApiException, TerracottaConnectorException {
        List<LmsAssignment> result = oneEdTechApiClient.listAssignments(ltiUserEntity, ltiContextEntity);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testListAssignmentsByExperiment() throws ApiException, TerracottaConnectorException {
        List<LmsAssignment> result = oneEdTechApiClient.listAssignments(ltiUserEntity, experiment);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testListAssignmentsByPlatformDeployment() throws ApiException, TerracottaConnectorException {
        List<LmsAssignment> result = oneEdTechApiClient.listAssignments(platformDeployment, "lmsCourseId", "tokenOverride");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testCheckAssignmentExists() throws ApiException, TerracottaConnectorException {
        Optional<LmsAssignment> result = oneEdTechApiClient.checkAssignmentExists(ltiUserEntity, "assignmentId", "lmsCourseId");

        assertTrue(result.isPresent());
    }

    @Test
    public void testListAssignmentByCourseAndAssignmentId() throws ApiException, TerracottaConnectorException {
        Optional<LmsAssignment> result = oneEdTechApiClient.listAssignment(ltiUserEntity, "lmsCourseId", "assignmentId");

        assertTrue(result.isPresent());
    }

    @Test
    public void testListAssignmentByAssignmentEntityDelegatesToStringOverload() throws ApiException, TerracottaConnectorException {
        when(assignment.getLmsAssignmentId()).thenReturn("lmsAssignmentId");

        Optional<LmsAssignment> result = oneEdTechApiClient.listAssignment(ltiUserEntity, "lmsCourseId", assignment);

        assertTrue(result.isPresent());
        verify(assignment).getLmsAssignmentId();
    }

    @Test
    public void testEditAssignment() throws ApiException, TerracottaConnectorException {
        Optional<LmsAssignment> result = oneEdTechApiClient.editAssignment(ltiUserEntity, lmsAssignment, "lmsCourseId");

        assertTrue(result.isPresent());
    }

    @Test
    public void testEditAssignmentWithPlatformDeployment() throws ApiException, TerracottaConnectorException {
        Optional<LmsAssignment> result = oneEdTechApiClient.editAssignment(platformDeployment, lmsAssignment, "lmsCourseId", "tokenOverride");

        assertTrue(result.isPresent());
    }

    @Test
    public void testRestoreAssignment() throws Exception {
        AssignmentExtended result = oneEdTechApiClient.restoreAssignment(assignment);

        assertNotNull(result);
    }

    @Test
    public void testEditAssignmentNameInLms() {
        assertDoesNotThrow(() -> oneEdTechApiClient.editAssignmentNameInLms(assignment, "lmsCourseId", "newName", ltiUserEntity));
    }

    @Test
    public void testDeleteAssignmentInLmsWithAssignment() {
        assertDoesNotThrow(() -> oneEdTechApiClient.deleteAssignmentInLms(assignment, "lmsCourseId", ltiUserEntity));
    }

    @Test
    public void testDeleteAssignmentInLmsWithLmsAssignment() {
        assertDoesNotThrow(() -> oneEdTechApiClient.deleteAssignmentInLms(lmsAssignment, "lmsCourseId", ltiUserEntity));
    }

    @Test
    public void testUploadConsentFile() throws Exception {
        AssignmentExtended result = oneEdTechApiClient.uploadConsentFile(experiment, consentDocument, ltiUserEntity);

        assertNotNull(result);
    }

    @Test
    public void testResyncAssignmentTargetUrisInLms() {
        assertDoesNotThrow(
            () -> oneEdTechApiClient.resyncAssignmentTargetUrisInLms(platformDeployment, ltiUserEntity, 1L, "tokenOverride", List.of("1"), List.of("2"), List.of("1", "2"))
        );
    }

    @Test
    public void testUpdateAssignmentMetadata() {
        assertDoesNotThrow(() -> oneEdTechApiClient.updateAssignmentMetadata(assignment, lmsAssignment));
    }

    @Test
    public void testListCoursesForUser() throws ApiException, TerracottaConnectorException {
        List<?> result = oneEdTechApiClient.listCoursesForUser(platformDeployment, "lmsUserId", "tokenOverride");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testListSubmissionsByOutcome() throws Exception {
        List<LmsSubmission> result = oneEdTechApiClient.listSubmissions(ltiUserEntity, outcome, "lmsCourseId");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testListSubmissionsByAssignmentId() throws Exception {
        List<LmsSubmission> result = oneEdTechApiClient.listSubmissions(ltiUserEntity, "lmsAssignmentId", "lmsCourseId");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testListSubmissionsForMultipleAssignments() throws Exception {
        List<LmsSubmission> result = oneEdTechApiClient.listSubmissionsForMultipleAssignments(ltiUserEntity, "lmsCourseId", List.of("1", "2"));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testAddLmsExtensions() {
        assertDoesNotThrow(() -> oneEdTechApiClient.addLmsExtensions(score, submission, true));
    }

    @Test
    public void testSendConversation() throws ApiException {
        List<LmsConversation> result = oneEdTechApiClient.sendConversation(null, ltiUserEntity);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetConversation() throws ApiException {
        Optional<LmsConversation> result = oneEdTechApiClient.getConversation(null, ltiUserEntity);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testListUsersForCourse() throws ApiException {
        List<LmsUser> result = oneEdTechApiClient.listUsersForCourse(null, ltiUserEntity);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFile() throws ApiException {
        Optional<LmsFile> result = oneEdTechApiClient.getFile(ltiUserEntity, "lmsFileId");

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFiles() throws ApiException, TerracottaConnectorException {
        List<LmsFile> result = oneEdTechApiClient.getFiles(ltiUserEntity);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}
