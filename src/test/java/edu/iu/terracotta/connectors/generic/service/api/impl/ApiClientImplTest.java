package edu.iu.terracotta.connectors.generic.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

public class ApiClientImplTest extends BaseTest {

    @InjectMocks private ApiClientImpl apiClient;

    @BeforeEach
    public void setUp() throws TerracottaConnectorException, ApiException {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testCreateLmsAssignment() throws ApiException, TerracottaConnectorException {
        LmsAssignment ret = apiClient.createLmsAssignment(ltiUserEntity, assignment, "courseId");

        assertNotNull(ret);
    }

    @Test
    public void testListAssignmentsForContext() throws ApiException, TerracottaConnectorException {

        List<LmsAssignment> ret = apiClient.listAssignments(ltiUserEntity, ltiContextEntity);

        assertEquals(1, ret.size());
    }

    @Test
    public void testListAssignmentsForExperiment() throws ApiException, TerracottaConnectorException {

        List<LmsAssignment> ret = apiClient.listAssignments(ltiUserEntity, experiment);

        assertEquals(1, ret.size());
    }

    @Test
    public void testCheckAssignmentExists() throws ApiException, TerracottaConnectorException {
        Optional<LmsAssignment> ret = apiClient.checkAssignmentExists(ltiUserEntity, "1", "courseId");

        assertTrue(ret.isPresent());
    }

    @Test
    public void testEditAssignment() throws ApiException, TerracottaConnectorException {
        Optional<LmsAssignment> ret = apiClient.editAssignment(ltiUserEntity, lmsAssignment, "courseId");

        assertTrue(ret.isPresent());
    }

    @Test
    public void testRestoreAssignment() throws ApiException, IOException, TerracottaConnectorException {
        LmsAssignment ret = apiClient.restoreAssignment(assignment);

        assertNotNull(ret);
    }

    @Test
    public void testEditAssignmentNameInLms() throws ApiException, IOException, TerracottaConnectorException {
        apiClient.editAssignmentNameInLms(assignment, "courseId", "newName", ltiUserEntity);
        // Verify interaction
    }

    @Test
    public void testDeleteAssignmentInLms() throws ApiException, IOException, TerracottaConnectorException {
        apiClient.deleteAssignmentInLms(assignment, "courseId", ltiUserEntity);
        // Verify interaction
    }

    @Test
    public void testUploadConsentFile() throws ApiException, IOException, TerracottaConnectorException {
        LmsAssignment ret = apiClient.uploadConsentFile(experiment, consentDocument, ltiUserEntity);

        assertNotNull(ret);
    }

    @Test
    public void testListCoursesForUser() throws ApiException, TerracottaConnectorException {
        List<LmsCourse> ret = apiClient.listCoursesForUser(platformDeployment, "userId", "token");

        assertEquals(1, ret.size());
    }

    @Test
    public void testAddLmsExtensions() throws ApiException, IOException, TerracottaConnectorException {
        apiClient.addLmsExtensions(score, submission, true);
        // Verify interaction
    }

    @Test
    public void testListSubmissions() throws ApiException, IOException, TerracottaConnectorException {
        List<LmsSubmission> ret = apiClient.listSubmissions(ltiUserEntity, "1", "courseId");

        assertEquals(1, ret.size());
    }
}