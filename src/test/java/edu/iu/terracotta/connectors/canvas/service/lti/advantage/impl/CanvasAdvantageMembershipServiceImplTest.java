package edu.iu.terracotta.connectors.canvas.service.lti.advantage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUsers;
import edu.iu.terracotta.connectors.generic.dao.model.lti.Roles;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.service.app.async.LmsUserBatchAsyncService;

public class CanvasAdvantageMembershipServiceImplTest extends BaseTest {

    @InjectMocks private CanvasAdvantageMembershipServiceImpl canvasAdvantageMembershipService;

    @Mock private LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
    @Mock private LmsUserBatchRepository lmsUserBatchRepository;
    @Mock private LmsUserBatchAsyncService lmsUserBatchAsyncService;

    private ResponseEntity<CourseUsers> successResponse;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
        // @InjectMocks only does constructor injection here (all constructor params already match), so
        // the separate @PersistenceContext EntityManager field is never populated unless set explicitly
        org.springframework.test.util.ReflectionTestUtils.setField(canvasAdvantageMembershipService, "entityManager", entityManager);

        successResponse = new ResponseEntity<>(courseUsers, HttpStatusCode.valueOf(200));

        when(advantageConnectorHelper.createTokenizedRequestEntityWithAccept(any(), anyString())).thenReturn(httpEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(CourseUsers.class))).thenReturn(successResponse);
    }

    @Test
    public void testGetToken() throws ConnectionException, TerracottaConnectorException {
        var ret = canvasAdvantageMembershipService.getToken(platformDeployment);

        assertEquals(ltiToken, ret);
        verify(advantageConnectorHelper).getToken(platformDeployment, "https://purl.imsglobal.org/spec/lti-nrps/scope/contextmembership.readonly");
    }

    @Test
    public void testCallMembershipServiceThreeArgOverloadDelegatesToOnlyStudentsTrue() throws ConnectionException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();

        CourseUsers ret = canvasAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId);

        assertNull(ret);

        ArgumentCaptor<List<LmsUserBatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(lmsUserBatchRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("user_id", captor.getValue().get(0).getLmsUserId());
        assertEquals(batchId, captor.getValue().get(0).getBatchId());
    }

    @Test
    public void testCallMembershipServiceSuccessCreatesInProgressRecord() throws ConnectionException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();

        CourseUsers ret = canvasAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId, true);

        assertNull(ret);

        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository).saveAndFlush(captor.capture());
        assertEquals(LmsUserBatchStatus.IN_PROGRESS, captor.getValue().getStatus());
        assertEquals(batchId, captor.getValue().getBatchId());
        verify(entityManager).flush();
        verify(entityManager).clear();
    }

    @Test
    public void testCallMembershipServiceFiltersOutNonStudentsWhenOnlyStudentsTrue() throws ConnectionException, TerracottaConnectorException {
        when(courseUser.getRoles()).thenReturn(List.of("some.other.role"));

        canvasAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), true);

        ArgumentCaptor<List<LmsUserBatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(lmsUserBatchRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    public void testCallMembershipServiceIncludesMembershipLearnerRoleWhenOnlyStudentsTrue() throws ConnectionException, TerracottaConnectorException {
        when(courseUser.getRoles()).thenReturn(List.of(Roles.MEMBERSHIP_LEARNER));

        canvasAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), true);

        ArgumentCaptor<List<LmsUserBatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(lmsUserBatchRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    public void testCallMembershipServiceIncludesNonStudentsWhenOnlyStudentsFalse() throws ConnectionException, TerracottaConnectorException {
        when(courseUser.getRoles()).thenReturn(List.of("some.other.role"));

        canvasAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), false);

        ArgumentCaptor<List<LmsUserBatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(lmsUserBatchRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    public void testCallMembershipServicePaginatesUntilNoNextPage() throws ConnectionException, TerracottaConnectorException {
        when(advantageConnectorHelper.nextPage(any())).thenReturn("https://canvas.example.edu/next", (String) null);

        canvasAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), true);

        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(CourseUsers.class));
        verify(lmsUserBatchRepository, times(2)).saveAll(anyList());
        verify(entityManager, times(2)).flush();
        verify(entityManager, times(2)).clear();
    }

    @Test
    public void testCallMembershipServiceReturnsNullWhenBodyIsNull() throws ConnectionException, TerracottaConnectorException {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(CourseUsers.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(200)));

        CourseUsers ret = canvasAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), true);

        assertNull(ret);
        verify(lmsUserBatchRepository, never()).saveAll(anyList());
    }

    @Test
    public void testCallMembershipServiceThrowsConnectionExceptionOnBadStatusWithExistingProcessingRecord() {
        UUID batchId = UUID.randomUUID();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(CourseUsers.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));

        LmsUserBatchProcessing existing = LmsUserBatchProcessing.builder().batchId(batchId).status(LmsUserBatchStatus.IN_PROGRESS).build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.of(existing));

        assertThrows(ConnectionException.class, () -> canvasAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId, true));

        assertEquals(LmsUserBatchStatus.FAILED, existing.getStatus());
        verify(lmsUserBatchProcessingRepository, times(2)).saveAndFlush(any(LmsUserBatchProcessing.class));
    }

    @Test
    public void testCallMembershipServiceThrowsConnectionExceptionOnBadStatusWithNoExistingProcessingRecord() {
        UUID batchId = UUID.randomUUID();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(CourseUsers.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));

        assertThrows(ConnectionException.class, () -> canvasAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId, true));

        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository, times(2)).saveAndFlush(captor.capture());
        assertEquals(LmsUserBatchStatus.FAILED, captor.getValue().getStatus());
        assertEquals(batchId, captor.getValue().getBatchId());
    }

    @Test
    public void testCallMembershipServiceWrapsUnexpectedExceptionInConnectionException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(CourseUsers.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ConnectionException.class, () -> canvasAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), true));
    }

    @Test
    public void testGetAllLmsUsersReturnsBatchAndSignalsSuccess() throws ConnectionException, TerracottaConnectorException {
        List<LmsUserBatch> expected = List.of(LmsUserBatch.builder().lmsUserId("user_id").build());
        when(lmsUserBatchRepository.findByBatchId(any(UUID.class), eq(Pageable.unpaged()))).thenReturn(expected);

        List<LmsUserBatch> ret = canvasAdvantageMembershipService.getAllLmsUsers(ltiToken, ltiContextEntity);

        assertEquals(expected, ret);
        verify(lmsUserBatchAsyncService).success(any(UUID.class));
        verify(lmsUserBatchRepository).saveAll(anyList());
    }

    @Test
    public void testGetAllLmsUsersPropagatesConnectionExceptionAndSkipsSuccessSignal() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(CourseUsers.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));

        assertThrows(ConnectionException.class, () -> canvasAdvantageMembershipService.getAllLmsUsers(ltiToken, ltiContextEntity));

        verify(lmsUserBatchAsyncService, never()).success(any(UUID.class));
    }

}
