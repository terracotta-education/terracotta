package edu.iu.terracotta.connectors.oneedtech.service.lti.advantage.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUser;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUsers;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.Roles;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.service.app.async.LmsUserBatchAsyncService;

@SuppressWarnings("unchecked")
public class OneEdTechAdvantageMembershipServiceImplTest extends BaseTest {

    // LmsUserBatchProcessingRepository, LmsUserBatchRepository and LmsUserBatchAsyncService are NOT
    // declared anywhere in the BaseModelTest/BaseRepositoryTest/BaseServiceTest hierarchy, so without
    // these local @Mock fields, Mockito's @InjectMocks constructor-injection would wire null for
    // these constructor params, causing NPEs anywhere the real method body runs.
    @Mock private LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
    @Mock private LmsUserBatchRepository lmsUserBatchRepository;
    @Mock private LmsUserBatchAsyncService lmsUserBatchAsyncService;

    @InjectMocks private OneEdTechAdvantageMembershipServiceImpl oneEdTechAdvantageMembershipService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        // @Value fields are never populated by Mockito's @InjectMocks (there's no Spring context in
        // this unit test), so they default to 0/false unless set explicitly.
        ReflectionTestUtils.setField(oneEdTechAdvantageMembershipService, "batchSize", 500);
        ReflectionTestUtils.setField(oneEdTechAdvantageMembershipService, "tokenLoggingEnabled", false);
        // @InjectMocks only does constructor injection here (all constructor params already match), so
        // the separate @PersistenceContext EntityManager field is never populated unless set explicitly
        ReflectionTestUtils.setField(oneEdTechAdvantageMembershipService, "entityManager", entityManager);

        // createTokenizedRequestEntityWithAccept(LtiToken, String) is not stubbed anywhere in
        // BaseServiceTest/BaseModelTest.
        when(advantageConnectorHelper.createTokenizedRequestEntityWithAccept(any(LtiToken.class), anyString())).thenReturn(httpEntity);

        // restTemplate.exchange(..., CourseUsers.class) is not stubbed anywhere in BaseModelTest either;
        // default (one page, 200 OK, using the inherited courseUsers/courseUser mocks) success stub:
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(CourseUsers.class))).thenReturn(new ResponseEntity<>(courseUsers, HttpStatus.OK));
    }

    @Test
    public void testGetToken() throws ConnectionException, TerracottaConnectorException {
        LtiToken ret = oneEdTechAdvantageMembershipService.getToken(platformDeployment);

        assertEquals(ltiToken, ret);
        verify(advantageConnectorHelper).getToken(platformDeployment, LtiAgsScope.NRPS_MEMBERSHIP_READONLY.key());
    }

    @Test
    public void testCallMembershipServiceThreeArgOverloadDefaultsToOnlyStudentsTrue() throws ConnectionException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();

        oneEdTechAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId);

        // the inherited `courseUser` mock has role LEARNER, so it should be kept when onlyStudents=true
        ArgumentCaptor<List<LmsUserBatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(lmsUserBatchRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    public void testCallMembershipServiceSuccessCreatesInProgressRecordWithContextId() throws ConnectionException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();

        oneEdTechAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId, true);

        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository).saveAndFlush(captor.capture());
        assertEquals(LmsUserBatchStatus.IN_PROGRESS, captor.getValue().getStatus());
        assertEquals(batchId, captor.getValue().getBatchId());
        assertEquals(ltiContextEntity.getContextId(), captor.getValue().getContextId());
    }

    // a caller (e.g. startPrepareParticipation) may have already created the
    // LmsUserBatchProcessing row for this batchId - callMembershipService must update that row
    // in place rather than inserting a second one for the same logical operation
    @Test
    public void testCallMembershipServiceReusesExistingRecordRatherThanCreatingNew() throws ConnectionException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchProcessing existing = LmsUserBatchProcessing.builder().batchId(batchId).status(LmsUserBatchStatus.IN_PROGRESS).build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.of(existing));

        oneEdTechAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId, true);

        verify(lmsUserBatchProcessingRepository).saveAndFlush(existing);
        assertEquals(LmsUserBatchStatus.IN_PROGRESS, existing.getStatus());
        assertEquals(ltiContextEntity.getContextId(), existing.getContextId());
    }

    @Test
    public void testCallMembershipServiceOnlyStudentsTrueFiltersNonLearners() throws ConnectionException, TerracottaConnectorException {
        CourseUser learner = CourseUser.builder().userId("learnerId").name("Learner One").email("learner@example.com").roles(List.of(Roles.LEARNER)).build();
        CourseUser instructor = CourseUser.builder().userId("instructorId").name("Instructor One").email("instructor@example.com").roles(List.of(Roles.INSTRUCTOR)).build();
        CourseUsers page = CourseUsers.builder().courseUserList(List.of(learner, instructor)).build();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(CourseUsers.class))).thenReturn(new ResponseEntity<>(page, HttpStatus.OK));

        oneEdTechAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), true);

        ArgumentCaptor<List<LmsUserBatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(lmsUserBatchRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("learnerId", captor.getValue().get(0).getLmsUserId());
    }

    @Test
    public void testCallMembershipServiceOnlyStudentsFalseKeepsEveryone() throws ConnectionException, TerracottaConnectorException {
        CourseUser learner = CourseUser.builder().userId("learnerId").name("Learner One").email("learner@example.com").roles(List.of(Roles.LEARNER)).build();
        CourseUser instructor = CourseUser.builder().userId("instructorId").name("Instructor One").email("instructor@example.com").roles(List.of(Roles.INSTRUCTOR)).build();
        CourseUsers page = CourseUsers.builder().courseUserList(List.of(learner, instructor)).build();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(CourseUsers.class))).thenReturn(new ResponseEntity<>(page, HttpStatus.OK));

        oneEdTechAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), false);

        ArgumentCaptor<List<LmsUserBatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(lmsUserBatchRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    public void testCallMembershipServiceNullBodyReturnsNull() throws ConnectionException, TerracottaConnectorException {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(CourseUsers.class))).thenReturn(new ResponseEntity<CourseUsers>((CourseUsers) null, HttpStatus.OK));

        CourseUsers ret = oneEdTechAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), true);

        assertNull(ret);
        verify(lmsUserBatchRepository, never()).saveAll(anyList());
    }

    @Test
    public void testCallMembershipServiceBadRequestMarksBatchFailed() throws ConnectionException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(CourseUsers.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.empty());

        assertThrows(ConnectionException.class, () -> oneEdTechAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId, true));

        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository, times(2)).saveAndFlush(captor.capture());
        assertEquals(LmsUserBatchStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    public void testCallMembershipServiceGenericExceptionWrapsAsConnectionException() throws ConnectionException, TerracottaConnectorException {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(CourseUsers.class))).thenThrow(new RuntimeException("connection failure"));

        assertThrows(ConnectionException.class, () -> oneEdTechAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), true));
    }

    @Test
    public void testCallMembershipServiceTokenLoggingEnabledDoesNotThrow() {
        ReflectionTestUtils.setField(oneEdTechAdvantageMembershipService, "tokenLoggingEnabled", true);

        assertDoesNotThrow(() -> oneEdTechAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), true));
    }

    @Test
    public void testCallMembershipServiceWithPaginationProcessesEachPage() throws ConnectionException, TerracottaConnectorException {
        CourseUser learnerPage2 = CourseUser.builder().userId("learnerPage2").name("Learner Two").email("learner2@example.com").roles(List.of(Roles.LEARNER)).build();
        CourseUsers page2 = CourseUsers.builder().courseUserList(List.of(learnerPage2)).build();

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(CourseUsers.class)))
            .thenReturn(new ResponseEntity<>(courseUsers, HttpStatus.OK), new ResponseEntity<>(page2, HttpStatus.OK));
        when(advantageConnectorHelper.nextPage(any(HttpHeaders.class))).thenReturn(LTI_URL, (String) null);

        oneEdTechAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), true);

        verify(lmsUserBatchRepository, times(2)).saveAll(anyList());
        verify(entityManager, times(2)).flush();
        verify(entityManager, times(2)).clear();
    }

    // BUG (also present, verbatim, in CanvasAdvantageMembershipServiceImpl): the null-body check inside
    // the pagination loop tests `membershipGetResponse.getBody()` (the FIRST page's response, already
    // known non-null) instead of `responseForNextPage.getBody()` (the CURRENT page). So a null body on
    // page 2+ is never caught by that check, and addToBatchData() is called with
    // `responseForNextPage.getBody().getCourseUserList()`, throwing an NPE instead of returning null
    // gracefully like the first-page case does. The NPE is still caught by the method's own
    // catch (Exception e) and re-wrapped as a ConnectionException, so it doesn't escape the method,
    // but data for that page (and any page after it) is silently lost instead of what the surrounding
    // code clearly intended (an early, graceful `return null`).
    @Test
    public void testCallMembershipServiceNextPageNullBodyReturnsNull() throws ConnectionException, TerracottaConnectorException {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(CourseUsers.class)))
            .thenReturn(new ResponseEntity<>(courseUsers, HttpStatus.OK), new ResponseEntity<CourseUsers>((CourseUsers) null, HttpStatus.OK));
        when(advantageConnectorHelper.nextPage(any(HttpHeaders.class))).thenReturn(LTI_URL, (String) null);

        assertNull(oneEdTechAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, UUID.randomUUID(), true));
    }

    @Test
    public void testGetAllLmsUsers() throws ConnectionException, TerracottaConnectorException {
        LmsUserBatch savedUser = LmsUserBatch.builder().lmsUserId("learnerId").email("learner@example.com").name("Learner One").userKey("learnerId").build();
        when(lmsUserBatchRepository.findByBatchId(any(UUID.class), eq(Pageable.unpaged()))).thenReturn(List.of(savedUser));

        List<LmsUserBatch> ret = oneEdTechAdvantageMembershipService.getAllLmsUsers(ltiToken, ltiContextEntity);

        assertNotNull(ret);
        assertEquals(1, ret.size());
        verify(lmsUserBatchAsyncService).success(any(UUID.class));
        // getAllLmsUsers always calls the 4-arg overload with onlyStudents=false
        verify(lmsUserBatchRepository).saveAll(anyList());
    }

}
