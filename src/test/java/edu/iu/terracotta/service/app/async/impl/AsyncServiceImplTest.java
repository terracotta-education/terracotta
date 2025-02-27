package edu.iu.terracotta.service.app.async.impl;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.ObsoleteAssignment;
import edu.iu.terracotta.exceptions.DataServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AsyncServiceImplTest extends BaseTest {

    @InjectMocks private AsyncServiceImpl asyncService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContext() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(obsoleteAssignmentRepository.findAllByContext_ContextId(anyLong())).thenReturn(Collections.emptyList());
        when(lmsAssignment.getLmsExternalToolFields().getUrl()).thenReturn(LTI_URL + "?experiment=2&assignment=1");
        when(lmsAssignment.getId()).thenReturn("2");

        asyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo);

        verify(apiClient).editAssignment(any(LtiUserEntity.class), any(LmsAssignment.class), anyString());
        verify(obsoleteAssignmentRepository).save(any(ObsoleteAssignment.class));
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextNoAssignments() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(securedInfo.getContextId()).thenReturn(1L);
        when(assignmentRepository.findAssignmentsToCheckByContext(anyLong())).thenReturn(Collections.emptyList());

        asyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo);

        verify(apiClient, never()).editAssignment(any(LtiUserEntity.class), any(LmsAssignment.class), anyString());
        verify(obsoleteAssignmentRepository, never()).save(any(ObsoleteAssignment.class));
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextNoLmsAssignments() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(assignmentService.getAllAssignmentsForLmsCourse(any(SecuredInfo.class))).thenReturn(Collections.emptyList());

        asyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo);

        verify(apiClient, never()).editAssignment(any(LtiUserEntity.class), any(LmsAssignment.class), anyString());
        verify(obsoleteAssignmentRepository, never()).save(any(ObsoleteAssignment.class));
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextException() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(obsoleteAssignmentRepository.findAllByContext_ContextId(anyLong())).thenReturn(Collections.emptyList());
        when(lmsAssignment.getLmsExternalToolFields().getUrl()).thenReturn(LTI_URL + "?experiment=2&assignment=1");
        when(lmsAssignment.getId()).thenReturn("2");

        doThrow(new ApiException("API Exception")).when(apiClient).editAssignment(any(LtiUserEntity.class), any(LmsAssignment.class), anyString());

        asyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo);

        verify(apiClient).editAssignment(any(LtiUserEntity.class), any(LmsAssignment.class), anyString());
        verify(obsoleteAssignmentRepository, never()).save(any(ObsoleteAssignment.class));
    }

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContext() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(lmsAssignment.getId()).thenReturn("2");

        asyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo);

        verify(assignmentService).restoreAssignmentInLms(any(Assignment.class));
    }

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContextNoAssignmentsToCheck() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(assignmentRepository.findAssignmentsToCheckByContext(anyLong())).thenReturn(Collections.emptyList());

        asyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo);

        verify(assignmentService, never()).restoreAssignmentInLms(any(Assignment.class));
    }

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContextNoLmsAssignments() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(assignmentService.getAllAssignmentsForLmsCourse(any(SecuredInfo.class))).thenReturn(Collections.emptyList());

        asyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo);

        verify(assignmentService, never()).restoreAssignmentInLms(any(Assignment.class));
    }

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContextException() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(lmsAssignment.getId()).thenReturn("2");

        doThrow(new ApiException("API Exception")).when(assignmentService).restoreAssignmentInLms(any(Assignment.class));

        asyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo);

        verify(assignmentService).restoreAssignmentInLms(any(Assignment.class));
    }
}
