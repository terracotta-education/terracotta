package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.ParticipantDto;
import edu.iu.terracotta.dao.model.enums.DistributionTypes;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.ParticipantAlreadyStartedException;
import edu.iu.terracotta.service.app.async.LmsUserBatchAsyncService;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("unchecked")
public class ParticipantServiceImplTest extends BaseTest {

    // NOTE: LmsUserBatchRepository and LmsUserBatchAsyncService are NOT declared anywhere in the
    // BaseModelTest/BaseRepositoryTest/BaseServiceTest hierarchy, so without these local @Mock
    // fields, Mockito's @InjectMocks constructor-injection would wire null for these two
    // ParticipantServiceImpl constructor params, causing NPEs anywhere refreshParticipants()'s
    // real body is exercised (as opposed to being merely stubbed out at the participantService spy).
    @Mock
    private LmsUserBatchRepository lmsUserBatchRepository;

    @Mock
    private LmsUserBatchAsyncService lmsUserBatchAsyncService;

    private ParticipantServiceImpl participantService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
        clearInvocations(participant);

        // apiClient/apiJwtService below are ApiClient/ApiJwtService-typed mock candidates that also
        // collide with CanvasApiClientImpl/CanvasApiJwtServiceImpl in BaseServiceTest (see the
        // @InjectMocks pitfall note there), so this class is constructed manually instead of relying
        // on @InjectMocks, which non-deterministically wired the wrong mocks and left apiClient calls
        // (e.g. calculatedPublishedAssignmentIds, postConsentSubmission) silently unstubbed.
        participantService = Mockito.spy(
            new ParticipantServiceImpl(
                assignmentRepository,
                consentDocumentRepository,
                experimentRepository,
                groupRepository,
                lmsUserBatchRepository,
                ltiUserRepository,
                participantRepository,
                submissionRepository,
                treatmentRepository,
                advantageAgsService,
                advantageMembershipService,
                apiJwtService,
                apiClient,
                groupParticipantService,
                lmsUserBatchAsyncService,
                ltiDataService
            )
        );
        ReflectionTestUtils.setField(participantService, "batchSize", 500);
        ReflectionTestUtils.setField(participantService, "entityManager", entityManager);

        when(condition.getDefaultCondition()).thenReturn(true);
        when(experiment.getDistributionType()).thenReturn(DistributionTypes.CUSTOM);
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.AUTO);
        when(participant.getDateGiven()).thenReturn(Timestamp.from(Instant.now()));
        when(participant.getDateRevoked()).thenReturn(Timestamp.from(Instant.now()));
    }

    @Test
    public void testhandleExperimentParticipantAutoParticipation() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException {
        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        verify(participant, never()).setGroup(any(Group.class));
    }

    @Test
    public void testhandleExperimentParticipantNotAutoParticipation() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.MANUAL);
        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        verify(participant, never()).setGroup(any(Group.class));
    }

    @Test
    public void testhandleExperimentParticipantInGroup() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(this.participant.getConsent()).thenReturn(true);

        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        verify(participant, never()).setGroup(any(Group.class));
    }

    @Test
    public void testhandleExperimentParticipantNotInAGroup() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(this.participant.getConsent()).thenReturn(true);
        when(participant.getGroup()).thenReturn(null);
        doNothing().when(participantService).refreshParticipants(anyLong());
        doReturn(participant).when(participantService).findParticipant(anyLong(), anyString());

        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        verify(participantService).refreshParticipants(experiment.getExperimentId());
        verify(participant).setGroup(any(Group.class));
    }

    // Test handleExperimentParticipant when a student has consented but hasn't been
    // assigned a group: refreshParticipants should be triggered before the group is assigned.
    @Test
    public void testHandleExperimentParticipantConsentedButNoGroup() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(participant.getConsent()).thenReturn(true);
        when(participant.getGroup()).thenReturn(null);
        doNothing().when(participantService).refreshParticipants(anyLong());
        doReturn(participant).when(participantService).findParticipant(anyLong(), anyString());

        participantService.handleExperimentParticipant(experiment, securedInfo);

        verify(participantService).refreshParticipants(experiment.getExperimentId());
        verify(participant).setGroup(any(Group.class));
    }

    // Test handleExperimentParticipant when a student has not consented but is marked
    // as dropped: refreshParticipants should be triggered and the dropped flag cleared.
    @Test
    public void testHandleExperimentParticipantNotConsentedAndDropped() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(participant.getConsent()).thenReturn(false);
        when(participant.getDropped()).thenReturn(true);
        doNothing().when(participantService).refreshParticipants(anyLong());
        doReturn(participant).when(participantService).findParticipant(anyLong(), anyString());

        participantService.handleExperimentParticipant(experiment, securedInfo);

        verify(participantService).refreshParticipants(experiment.getExperimentId());
        verify(participant).setDropped(false);
        verify(participant, never()).setGroup(any(Group.class));
    }

    // Test handleExperimentParticipant when no participant record exists yet: it should be
    // created directly from the current LTI launch's already-resolved LtiUserEntity/
    // LtiMembershipEntity, without triggering a full roster refresh.
    @Test
    public void testHandleExperimentParticipantCreatesFromLaunchWhenNotFound() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(null);
        when(ltiDataService.findByUserKeyAndPlatformDeployment(USER_ID, platformDeployment)).thenReturn(ltiUserEntity);
        when(ltiDataService.findByUserAndContext(ltiUserEntity, ltiContextEntity)).thenReturn(ltiMembershipEntity);

        participantService.handleExperimentParticipant(experiment, securedInfo);

        verify(participantService, never()).refreshParticipants(anyLong());
        verify(participantService, never()).findParticipant(anyLong(), anyString());
        verify(ltiDataService, never()).saveLtiMembershipEntity(any(LtiMembershipEntity.class));

        ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
        verify(participantRepository, atLeastOnce()).save(captor.capture());
        Participant created = captor.getAllValues().get(0);

        assertEquals(experiment, created.getExperiment());
        assertEquals(ltiUserEntity, created.getLtiUserEntity());
        assertEquals(ltiMembershipEntity, created.getLtiMembershipEntity());
        assertEquals(false, created.getDropped());
        assertTrue(created.getConsent());
    }

    // Test handleExperimentParticipant when no participant record exists yet and the launch's
    // LtiUserEntity can't be resolved (shouldn't normally happen): it should fall back to a
    // full roster refresh rather than fail outright.
    @Test
    public void testHandleExperimentParticipantFallsBackToRefreshWhenLaunchUserNotFound() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(null);
        when(ltiDataService.findByUserKeyAndPlatformDeployment(USER_ID, platformDeployment)).thenReturn(null);
        doNothing().when(participantService).refreshParticipants(anyLong());
        doReturn(participant).when(participantService).findParticipant(anyLong(), anyString());

        Participant result = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(result);
        verify(participantService).refreshParticipants(experiment.getExperimentId());
    }

    @Test
    public void testGetParticipant() throws InvalidUserException, ParticipantNotMatchingException {
        Participant retVal = participantService.getParticipant(1l, 1l, USER_ID, false);

        assertNotNull(retVal);
    }

    @Test
    public void testGetParticipantStudent() throws InvalidUserException, ParticipantNotMatchingException {
        Participant retVal = participantService.getParticipant(1l, 1l, USER_ID, true);

        assertNotNull(retVal);
    }

    @Test
    public void testFindAllByExperimentId() {
        List<Participant> retVal = participantService.findAllByExperimentId(1l);

        assertEquals(1, retVal.size());
    }

    @Test
    public void testPostParticipant() throws IdInPostException, DataServiceException {
        when(participantDto.getParticipantId()).thenReturn(null);
        ParticipantDto retVal = participantService.postParticipant(participantDto, 1l, securedInfo);

        assertNotNull(retVal);
    }

    @Test
    public void testResetParticipantConsentIfExperimentNotStarted() {
        for (ParticipationTypes participationType : Arrays.asList(ParticipationTypes.NOSET, ParticipationTypes.AUTO, ParticipationTypes.MANUAL, ParticipationTypes.CONSENT)) {
                when(participant.getSource()).thenReturn(participationType);

            for (ParticipationTypes experimentParticipationType : Arrays.asList(ParticipationTypes.NOSET, ParticipationTypes.AUTO, ParticipationTypes.MANUAL, ParticipationTypes.CONSENT)) {
                when(experiment.getParticipationType()).thenReturn(experimentParticipationType);

                assertDoesNotThrow(() -> {
                    participantService.resetParticipantConsentIfExperimentNotStarted(experiment, participant);
                });
            }
        }
    }

    @Test
    public void testChangeParticipant() {
        assertDoesNotThrow(() -> {
            participantService.changeParticipant(Collections.singletonMap(participant, participantDto), 1l, securedInfo);
        });
    }

    @Test
    public void testChangeParticipantFalseToTrueConsent() {
        when(participant.getConsent()).thenReturn(false);
        when(participantDto.getConsent()).thenReturn(true);

        assertDoesNotThrow(() -> {
            participantService.changeParticipant(Collections.singletonMap(participant, participantDto), 1l, securedInfo);
        });
    }

    @Test
    public void testChangeParticipantTrueToFalseConsent() {
        when(participantDto.getConsent()).thenReturn(false);

        assertDoesNotThrow(() -> {
            participantService.changeParticipant(Collections.singletonMap(participant, participantDto), 1l, securedInfo);
        });
    }

    @Test
    public void testChangeParticipantSavesAllAtOnceInsteadOfPerEntry() {
        Participant secondParticipant = mock(Participant.class);
        when(secondParticipant.getParticipantId()).thenReturn(2L);

        ParticipantDto secondParticipantDto = mock(ParticipantDto.class);

        Map<Participant, ParticipantDto> map = new LinkedHashMap<>();
        map.put(participant, participantDto);
        map.put(secondParticipant, secondParticipantDto);

        List<Participant> retVal = participantService.changeParticipant(map, 1L, securedInfo);

        assertEquals(2, retVal.size());
        verify(participantRepository, never()).save(any(Participant.class));
        verify(participantRepository).saveAll(Arrays.asList(participant, secondParticipant));
    }

    @Test
    public void testChangeConsent() throws ParticipantAlreadyStartedException, ExperimentNotMatchingException, ParticipantNotMatchingException {
        when(securedInfo.getConsent()).thenReturn(true);

        Participant retVal = participantService.changeConsent(participantDto, securedInfo, 1L);

        assertNotNull(retVal);
    }

    @Test
    public void testChangeConsentNoConsentValueReturnsEarly() throws ParticipantAlreadyStartedException, ExperimentNotMatchingException, ParticipantNotMatchingException {
        // securedInfo.getConsent() defaults to null; should return participant immediately without touching experiment/changeParticipant.
        Participant retVal = participantService.changeConsent(participantDto, securedInfo, 1L);

        assertNotNull(retVal);
        verify(participantRepository, never()).saveAll(any());
    }

    @Test
    public void testChangeConsentFalseConsentValueReturnsEarly() throws ParticipantAlreadyStartedException, ExperimentNotMatchingException, ParticipantNotMatchingException {
        when(securedInfo.getConsent()).thenReturn(false);

        Participant retVal = participantService.changeConsent(participantDto, securedInfo, 1L);

        assertNotNull(retVal);
        verify(participantRepository, never()).saveAll(any());
    }

    @Test
    public void testChangeConsentUserKeyMismatchReturnsEarly() throws ParticipantAlreadyStartedException, ExperimentNotMatchingException, ParticipantNotMatchingException {
        when(securedInfo.getConsent()).thenReturn(true);
        when(securedInfo.getUserId()).thenReturn("someone_else");

        Participant retVal = participantService.changeConsent(participantDto, securedInfo, 1L);

        assertNotNull(retVal);
        verify(participantRepository, never()).saveAll(any());
    }

    @Test
    public void testChangeConsentTrueToFalseSetsRevokedSource() throws ParticipantAlreadyStartedException, ExperimentNotMatchingException, ParticipantNotMatchingException {
        when(securedInfo.getConsent()).thenReturn(true);
        when(participant.getConsent()).thenReturn(true);
        when(participantDto.getConsent()).thenReturn(false);

        Participant retVal = participantService.changeConsent(participantDto, securedInfo, 1L);

        assertNotNull(retVal);
        // setSource(REVOKED) is set once directly in changeConsent() and again inside the changeParticipant() call it delegates to.
        verify(participant, atLeastOnce()).setSource(ParticipationTypes.REVOKED);
    }

    @Test
    public void testChangeConsentAlreadyStartedThrows() {
        when(securedInfo.getConsent()).thenReturn(true);
        when(participant.getConsent()).thenReturn(false);
        when(participantDto.getConsent()).thenReturn(true);
        // more than one treatment for the assignment means the participant "has submitted"
        when(treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(Arrays.asList(treatment, treatment));

        assertThrows(
            ParticipantAlreadyStartedException.class,
            () -> participantService.changeConsent(participantDto, securedInfo, 1L)
        );
    }

    @Test
    public void testChangeConsentExperimentNotMatchingThrows() {
        when(securedInfo.getConsent()).thenReturn(true);
        // first call (inside changeParticipant) returns the real experiment; second call (back in changeConsent) returns null
        when(experimentRepository.findByExperimentId(anyLong())).thenReturn(experiment, (Experiment) null);

        assertThrows(
            ExperimentNotMatchingException.class,
            () -> participantService.changeConsent(participantDto, securedInfo, 1L)
        );
    }

    @Test
    public void testChangeConsentTestStudentDoesNotMarkExperimentStarted() throws ParticipantAlreadyStartedException, ExperimentNotMatchingException, ParticipantNotMatchingException {
        when(securedInfo.getConsent()).thenReturn(true);
        when(participant.isTestStudent()).thenReturn(true);

        Participant retVal = participantService.changeConsent(participantDto, securedInfo, 1L);

        assertNotNull(retVal);
        verify(experiment, never()).setStarted(any());
    }

    @Test
    public void testGetParticipantNonStudent() throws InvalidUserException, ParticipantNotMatchingException {
        Participant retVal = participantService.getParticipant(1L, 1L, USER_ID, false);

        assertNotNull(retVal);
    }

    @Test
    public void testGetParticipantNonStudentNotMatchingThrows() {
        when(participantRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(
            ParticipantNotMatchingException.class,
            () -> participantService.getParticipant(1L, 1L, USER_ID, false)
        );
    }

    @Test
    public void testGetParticipantStudentInvalidUserThrows() {
        when(participant.getParticipantId()).thenReturn(99L);

        assertThrows(
            InvalidUserException.class,
            () -> participantService.getParticipant(1L, 1L, USER_ID, true)
        );
    }

    @Test
    public void testGetParticipantsNonStudentNoRefresh() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());

        List<ParticipantDto> retVal = participantService.getParticipants(1L, USER_ID, false, securedInfo, false);

        assertEquals(1, retVal.size());
        // refreshParticipants() would call lmsUserBatchAsyncService.success(...) as a side effect; absence of that call proves it wasn't invoked.
        verify(lmsUserBatchAsyncService, never()).success(any());
    }

    @Test
    public void testGetParticipantsNonStudentWithRefresh() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());

        List<ParticipantDto> retVal = participantService.getParticipants(1L, USER_ID, false, securedInfo, true);

        assertEquals(1, retVal.size());
        // side effect proving refreshParticipants()'s real body executed
        verify(lmsUserBatchAsyncService).success(any(UUID.class));
    }

    @Test
    public void testGetParticipantsNonStudentExcludesTestStudents() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participant.isTestStudent()).thenReturn(true);
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());

        List<ParticipantDto> retVal = participantService.getParticipants(1L, USER_ID, false, securedInfo, false);

        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testGetParticipantsStudentFound() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        List<ParticipantDto> retVal = participantService.getParticipants(1L, USER_ID, true, securedInfo, false);

        assertEquals(1, retVal.size());
    }

    @Test
    public void testGetParticipantsStudentNotFoundReturnsEmptyList() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(null);

        List<ParticipantDto> retVal = participantService.getParticipants(1L, USER_ID, true, securedInfo, false);

        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testPostParticipantIdInPostExceptionThrows() {
        when(participantDto.getParticipantId()).thenReturn(5L);

        assertThrows(
            IdInPostException.class,
            () -> participantService.postParticipant(participantDto, 1L, securedInfo)
        );
    }

    @Test
    public void testPostParticipantFromDtoFailureWrapsDataServiceException() {
        when(participantDto.getParticipantId()).thenReturn(null);
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        DataServiceException ex = assertThrows(
            DataServiceException.class,
            () -> participantService.postParticipant(participantDto, 1L, securedInfo)
        );
        assertTrue(ex.getMessage().contains("Error 105"));
    }

    @Test
    public void testToDtoTwoArgOverload() {
        ParticipantDto retVal = participantService.toDto(participant, securedInfo);

        assertNotNull(retVal);
        assertEquals(1L, retVal.getGroupId());
    }

    @Test
    public void testToDtoNoGroupLeavesGroupIdUnset() {
        when(participant.getGroup()).thenReturn(null);

        ParticipantDto retVal = participantService.toDto(participant, List.of(1L), securedInfo);

        assertNotNull(retVal);
        assertNull(retVal.getGroupId());
    }

    @Test
    public void testFromDtoSuccess() throws DataServiceException {
        when(participantDto.getExperimentId()).thenReturn(1L);

        Participant retVal = participantService.fromDto(participantDto);

        assertNotNull(retVal);
    }

    @Test
    public void testFromDtoGroupAssignedWhenExists() throws DataServiceException {
        when(participantDto.getExperimentId()).thenReturn(1L);
        when(groupRepository.existsByExperiment_ExperimentIdAndGroupId(anyLong(), anyLong())).thenReturn(true);

        Participant retVal = participantService.fromDto(participantDto);

        assertNotNull(retVal.getGroup());
    }

    @Test
    public void testFromDtoExperimentNotFoundThrows() {
        when(participantDto.getExperimentId()).thenReturn(1L);
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(DataServiceException.class, () -> participantService.fromDto(participantDto));
    }

    @Test
    public void testFromDtoUserNotFoundThrows() {
        when(participantDto.getExperimentId()).thenReturn(1L);
        when(ltiUserRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(DataServiceException.class, () -> participantService.fromDto(participantDto));
    }

    @Test
    public void testSaveAndFlush() {
        participantService.saveAndFlush(participant);

        verify(participantRepository).saveAndFlush(participant);
    }

    @Test
    public void testRefreshParticipantsNoUsersInBatch() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        // lmsUserBatchRepository.findByBatchId is unstubbed -> returns empty list -> loop body never runs
        assertDoesNotThrow(() -> participantService.refreshParticipants(1L));

        verify(lmsUserBatchAsyncService).success(any(UUID.class));
    }

    @Test
    public void testRefreshParticipantsMatchesExistingParticipant() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        LmsUserBatch batchUser = LmsUserBatch.builder().userKey(USER_ID).email(EMAIL).name(DISPLAY_NAME).build();

        when(lmsUserBatchRepository.findByBatchId(any(UUID.class), any())).thenReturn(List.of(batchUser), Collections.emptyList());
        when(participantRepository.findAllByExperiment_ExperimentIdAndLtiUserEntity_UserKeyIn(anyLong(), any())).thenReturn(List.of(participant));

        assertDoesNotThrow(() -> participantService.refreshParticipants(1L));

        verify(participant).setDropped(false);
        verify(ltiDataService, never()).saveLtiUserEntity(any());
        verify(lmsUserBatchAsyncService).success(any(UUID.class));
    }

    @Test
    public void testRefreshParticipantsCreatesNewParticipantForUnmatchedUser() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        LmsUserBatch batchUser = LmsUserBatch.builder().userKey("new_user_key").email(EMAIL).name(DISPLAY_NAME).build();
        LtiUserEntity newLtiUserEntity = mock(LtiUserEntity.class);
        LtiMembershipEntity newLtiMembershipEntity = mock(LtiMembershipEntity.class);

        when(lmsUserBatchRepository.findByBatchId(any(UUID.class), any())).thenReturn(List.of(batchUser), Collections.emptyList());
        when(participantRepository.findAllByExperiment_ExperimentIdAndLtiUserEntity_UserKeyIn(anyLong(), any())).thenReturn(Collections.emptyList());
        when(ltiDataService.findByUserKeyAndPlatformDeployment(anyString(), any())).thenReturn(null);
        when(ltiDataService.saveLtiUserEntity(any())).thenReturn(newLtiUserEntity);
        when(ltiDataService.findByUserAndContext(any(), any())).thenReturn(null);
        when(ltiDataService.saveLtiMembershipEntity(any())).thenReturn(newLtiMembershipEntity);

        assertDoesNotThrow(() -> participantService.refreshParticipants(1L));

        verify(ltiDataService).saveLtiUserEntity(any());
        verify(ltiDataService).saveLtiMembershipEntity(any());
    }

    @Test
    public void testRefreshParticipantsCreatesNewParticipantReusingExistingUserAndMembership() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        LmsUserBatch batchUser = LmsUserBatch.builder().userKey("new_user_key").email(EMAIL).name(DISPLAY_NAME).build();

        when(lmsUserBatchRepository.findByBatchId(any(UUID.class), any())).thenReturn(List.of(batchUser), Collections.emptyList());
        when(participantRepository.findAllByExperiment_ExperimentIdAndLtiUserEntity_UserKeyIn(anyLong(), any())).thenReturn(Collections.emptyList());
        when(ltiDataService.findByUserKeyAndPlatformDeployment(anyString(), any())).thenReturn(ltiUserEntity);
        when(ltiDataService.findByUserAndContext(any(), any())).thenReturn(ltiMembershipEntity);

        assertDoesNotThrow(() -> participantService.refreshParticipants(1L));

        verify(ltiDataService, never()).saveLtiUserEntity(any());
        verify(ltiDataService, never()).saveLtiMembershipEntity(any());
    }

    @Test
    public void testRefreshParticipantsExperimentNotFoundThrows() {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(
            ExperimentNotMatchingException.class,
            () -> participantService.refreshParticipants(1L)
        );
    }

    @Test
    public void testRefreshParticipantsConnectionExceptionWrapsAndFails() throws Exception {
        when(advantageMembershipService.getToken(any())).thenThrow(new ConnectionException("connection failed"));

        assertThrows(
            ParticipantNotUpdatedException.class,
            () -> participantService.refreshParticipants(1L)
        );

        verify(lmsUserBatchAsyncService).fail(any(UUID.class), anyString());
    }

    @Test
    public void testPrepareParticipation() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        assertDoesNotThrow(() -> participantService.prepareParticipation(1L, securedInfo));

        // side effect proving refreshParticipants()'s real body executed
        verify(lmsUserBatchAsyncService).success(any(UUID.class));
    }

    @Test
    public void testFindParticipantFound() {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());

        Participant retVal = participantService.findParticipant(1L, USER_ID);

        assertNotNull(retVal);
    }

    @Test
    public void testFindParticipantNotFound() {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());

        Participant retVal = participantService.findParticipant(1L, "no_such_user");

        assertNull(retVal);
    }

    @Test
    public void testCalculatedPublishedAssignmentIdsIncludesPublished() {
        List<Long> retVal = participantService.calculatedPublishedAssignmentIds(1L, "1", ltiUserEntity);

        assertEquals(List.of(1L), retVal);
    }

    @Test
    public void testCalculatedPublishedAssignmentIdsExcludesUnpublished() {
        when(lmsAssignment.isPublished()).thenReturn(false);

        List<Long> retVal = participantService.calculatedPublishedAssignmentIds(1L, "1", ltiUserEntity);

        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testCalculatedPublishedAssignmentIdsExcludesOnException() throws Exception {
        when(apiClient.listAssignment(any(), anyString(), anyString())).thenThrow(new RuntimeException("api failure"));

        List<Long> retVal = participantService.calculatedPublishedAssignmentIds(1L, "1", ltiUserEntity);

        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testBuildHeaders() {
        org.springframework.web.util.UriComponentsBuilder ucBuilder = org.springframework.web.util.UriComponentsBuilder.fromUriString("http://localhost:8080");

        org.springframework.http.HttpHeaders headers = participantService.buildHeaders(ucBuilder, 1L, 2L);

        assertNotNull(headers.getLocation());
        assertTrue(headers.getLocation().toString().contains("/api/experiments/1/participant/2"));
    }

    @Test
    public void testSetAllToTrue() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());

        assertDoesNotThrow(() -> participantService.setAllToTrue(1L));

        verify(participant).setConsent(true);
        verify(participantRepository).saveAll(List.of(participant));
    }

    @Test
    public void testSetAllToFalse() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());

        assertDoesNotThrow(() -> participantService.setAllToFalse(1L));

        verify(participant).setConsent(false);
    }

    @Test
    public void testSetAllToNull() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());

        assertDoesNotThrow(() -> participantService.setAllToNull(1L));

        verify(participant).setConsent(null);
    }

    @Test
    public void testPostConsentSubmissionSuccess() throws Exception {
        assertDoesNotThrow(() -> participantService.postConsentSubmission(participant, securedInfo));

        verify(advantageAgsService).postScore(any(), any(), any(), anyString(), any());
    }

    @Test
    public void testPostConsentSubmissionFallsBackToFixResourceLinkId() throws Exception {
        // consentDocument's resourceLinkId doesn't match any lineitem initially, forcing the fallback lookup
        when(consentDocument.getResourceLinkId()).thenReturn("different_link_id");

        assertDoesNotThrow(() -> participantService.postConsentSubmission(participant, securedInfo));

        verify(consentDocumentRepository).save(consentDocument);
        verify(advantageAgsService).postScore(any(), any(), any(), anyString(), any());
    }

    @Test
    public void testPostConsentSubmissionThrowsWhenNoLineItemFound() {
        when(consentDocument.getResourceLinkId()).thenReturn("different_link_id");
        when(jwt.get(anyString())).thenReturn("no_matching_resource_link_id");

        assertThrows(
            DataServiceException.class,
            () -> participantService.postConsentSubmission(participant, securedInfo)
        );
    }

    @Test
    public void testPostConsentSubmissionApiExceptionWrapsAsDataServiceException() throws Exception {
        when(consentDocument.getResourceLinkId()).thenReturn("different_link_id");
        when(apiClient.listAssignment(any(), anyString(), anyString())).thenThrow(new ApiException("boom"));

        assertThrows(
            DataServiceException.class,
            () -> participantService.postConsentSubmission(participant, securedInfo)
        );
    }

}
