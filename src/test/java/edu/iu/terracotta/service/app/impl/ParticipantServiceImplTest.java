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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Duration;
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
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
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
import edu.iu.terracotta.dao.model.dto.LmsUserBatchStatusDto;
import edu.iu.terracotta.dao.model.dto.ParticipantDto;
import edu.iu.terracotta.dao.model.enums.DistributionTypes;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.ParticipantAlreadyStartedException;
import edu.iu.terracotta.service.app.ParticipantRosterWriteService;
import edu.iu.terracotta.service.app.async.LmsUserBatchAsyncService;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Mock
    private LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;

    @Mock
    private ParticipantRosterWriteService participantRosterWriteService;

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
                ltiDataService,
                ltiContextRepository,
                lmsUserBatchProcessingRepository,
                participantRosterWriteService
            )
        );
        ReflectionTestUtils.setField(participantService, "batchSize", 500);
        ReflectionTestUtils.setField(participantService, "entityManager", entityManager);
        ReflectionTestUtils.setField(participantService, "refreshThrottleHours", 24L);
        ReflectionTestUtils.setField(participantService, "refreshThrottleMinParticipants", 1000L);
        ReflectionTestUtils.setField(participantService, "refreshDebounceSeconds", 300L);

        when(condition.getDefaultCondition()).thenReturn(true);
        when(experiment.getDistributionType()).thenReturn(DistributionTypes.CUSTOM);
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.AUTO);
        when(participant.getDateGiven()).thenReturn(Timestamp.from(Instant.now()));
        when(participant.getDateRevoked()).thenReturn(Timestamp.from(Instant.now()));
        // startPrepareParticipation's lookup of the LTI context to check freshness under lock
        when(ltiContextRepository.findById(anyLong())).thenReturn(Optional.of(ltiContextEntity));
        // the named-lock GET_LOCK/RELEASE_LOCK native queries acquireRosterSyncLock/
        // releaseRosterSyncLock issue - defaults to "lock acquired" (1) for every call
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);
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
        when(participant.getSource()).thenReturn(ParticipationTypes.AUTO);

        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        verify(participantService, never()).refreshParticipants(anyLong());
        verify(participantService).resetParticipantConsentIfExperimentNotStarted(experiment, this.participant);
        verify(participant).setGroup(any(Group.class));
    }

    // Test handleExperimentParticipant when a student has consented but hasn't been assigned a
    // group: the participant should be repaired directly (an active LTI launch already proves
    // they're enrolled), not via a full roster refresh.
    @Test
    public void testHandleExperimentParticipantConsentedButNoGroup() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(participant.getConsent()).thenReturn(true);
        when(participant.getGroup()).thenReturn(null);
        when(participant.getSource()).thenReturn(ParticipationTypes.AUTO);

        participantService.handleExperimentParticipant(experiment, securedInfo);

        verify(participantService, never()).refreshParticipants(anyLong());
        verify(participantService).resetParticipantConsentIfExperimentNotStarted(experiment, participant);
        verify(participant).setGroup(any(Group.class));
    }

    // Test handleExperimentParticipant when a student has not consented but is marked as
    // dropped: the dropped flag should be cleared directly, not via a full roster refresh.
    @Test
    public void testHandleExperimentParticipantNotConsentedAndDropped() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(participant.getConsent()).thenReturn(false);
        when(participant.getDropped()).thenReturn(true);
        when(participant.getSource()).thenReturn(ParticipationTypes.AUTO);

        participantService.handleExperimentParticipant(experiment, securedInfo);

        verify(participantService, never()).refreshParticipants(anyLong());
        verify(participantService).resetParticipantConsentIfExperimentNotStarted(experiment, participant);
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

    // Test ensureParticipantExists when the participant already exists: it should do nothing.
    @Test
    public void testEnsureParticipantExistsWhenAlreadyPresent() throws ExperimentNotMatchingException, ParticipantNotUpdatedException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);

        participantService.ensureParticipantExists(experiment.getExperimentId(), securedInfo);

        verify(participantService, never()).refreshParticipants(anyLong());
        verify(ltiDataService, never()).findByUserKeyAndPlatformDeployment(anyString(), any());
        verify(participantRepository, never()).save(any(Participant.class));
    }

    // Test ensureParticipantExists when no participant exists yet: it should create one
    // directly from the current LTI launch instead of syncing the entire course roster.
    @Test
    public void testEnsureParticipantExistsCreatesFromLaunch() throws ExperimentNotMatchingException, ParticipantNotUpdatedException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(null);
        when(ltiDataService.findByUserKeyAndPlatformDeployment(USER_ID, platformDeployment)).thenReturn(ltiUserEntity);
        when(ltiDataService.findByUserAndContext(ltiUserEntity, ltiContextEntity)).thenReturn(ltiMembershipEntity);

        participantService.ensureParticipantExists(experiment.getExperimentId(), securedInfo);

        verify(participantService, never()).refreshParticipants(anyLong());

        ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
        verify(participantRepository).save(captor.capture());

        assertEquals(ltiUserEntity, captor.getValue().getLtiUserEntity());
        assertEquals(ltiMembershipEntity, captor.getValue().getLtiMembershipEntity());
    }

    // Test ensureParticipantExists when no participant exists yet and the launch's
    // LtiUserEntity can't be resolved (shouldn't normally happen): it should fall back to a
    // full roster refresh rather than fail outright.
    @Test
    public void testEnsureParticipantExistsFallsBackToRefreshWhenLaunchUserNotFound() throws ExperimentNotMatchingException, ParticipantNotUpdatedException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(null);
        when(ltiDataService.findByUserKeyAndPlatformDeployment(USER_ID, platformDeployment)).thenReturn(null);
        doNothing().when(participantService).refreshParticipants(anyLong());

        participantService.ensureParticipantExists(experiment.getExperimentId(), securedInfo);

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

    // a participant with any recorded submission - even to a single-version assignment, where
    // hasParticipantSubmitted's "started" semantics would say false - must never have their
    // group reset. Doing so would orphan that submission from the "expected" count (computed
    // from current group membership), producing a nonsensical completed/expected ratio like "1/0"
    @Test
    public void testChangeParticipantDoesNotResetGroupAfterSingleVersionSubmission() {
        when(submissionRepository.findByParticipant_Id(anyLong())).thenReturn(List.of(submission));

        participantService.changeParticipant(Collections.singletonMap(participant, participantDto), 1L, securedInfo);

        verify(participant, never()).setGroup(any());
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

    // matching/creating/saving participants for a page is now ParticipantRosterWriteService's
    // job (its own transaction, independent of this method's - see ParticipantRosterWriteServiceImplTest
    // for that behavior); refreshParticipants is only responsible for paginating lms_user_batch
    // and delegating each page.
    @Test
    public void testRefreshParticipantsDelegatesEachPageToRosterWriteService() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        LmsUserBatch batchUser = LmsUserBatch.builder().userKey(USER_ID).email(EMAIL).name(DISPLAY_NAME).build();

        when(lmsUserBatchRepository.findByBatchId(any(UUID.class), any())).thenReturn(List.of(batchUser), Collections.emptyList());

        assertDoesNotThrow(() -> participantService.refreshParticipants(1L));

        verify(participantRosterWriteService).syncParticipantsPage(experiment, List.of(batchUser));
        verify(lmsUserBatchAsyncService).success(any(UUID.class));
    }

    @Test
    public void testRefreshParticipantsDelegatesMultiplePages() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        LmsUserBatch pageOneUser = LmsUserBatch.builder().userKey(USER_ID).email(EMAIL).name(DISPLAY_NAME).build();
        LmsUserBatch pageTwoUser = LmsUserBatch.builder().userKey("new_user_key").email(EMAIL).name(DISPLAY_NAME).build();

        when(lmsUserBatchRepository.findByBatchId(any(UUID.class), any()))
            .thenReturn(List.of(pageOneUser), List.of(pageTwoUser), Collections.emptyList());

        assertDoesNotThrow(() -> participantService.refreshParticipants(1L));

        verify(participantRosterWriteService).syncParticipantsPage(experiment, List.of(pageOneUser));
        verify(participantRosterWriteService).syncParticipantsPage(experiment, List.of(pageTwoUser));
        verify(lmsUserBatchAsyncService).success(any(UUID.class));
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

    // last_participant_sync lives on the LTI context (course), not the experiment, since the
    // LMS roster is shared by every experiment in that course.
    @Test
    public void testRefreshParticipantsIfStaleRefreshesWhenNeverSyncedBefore() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        doNothing().when(participantService).refreshParticipants(anyLong(), any(UUID.class));
        when(participantRepository.countByExperiment_ExperimentId(1L)).thenReturn(1001L);

        participantService.refreshParticipantsIfStale(1L);

        verify(participantService).refreshParticipants(eq(1L), any(UUID.class));
        verify(ltiContextEntity).setLastParticipantSync(any(Instant.class));
        verify(ltiContextRepository).save(ltiContextEntity);
    }

    // a full sync is only expensive enough to be worth throttling for large courses - at or
    // below the configured threshold, last_participant_sync is left null so the course keeps
    // refreshing on every call instead of skipping a sync that would have been cheap anyway
    @Test
    public void testRefreshParticipantsIfStaleSetsNullSyncTimestampWhenParticipantCountAtThreshold() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        doNothing().when(participantService).refreshParticipants(anyLong(), any(UUID.class));
        when(participantRepository.countByExperiment_ExperimentId(1L)).thenReturn(1000L);

        participantService.refreshParticipantsIfStale(1L);

        verify(ltiContextEntity).setLastParticipantSync(null);
        verify(ltiContextRepository).save(ltiContextEntity);
    }

    @Test
    public void testRefreshParticipantsIfStaleSetsNullSyncTimestampWhenParticipantCountBelowThreshold() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        doNothing().when(participantService).refreshParticipants(anyLong(), any(UUID.class));
        when(participantRepository.countByExperiment_ExperimentId(1L)).thenReturn(5L);

        participantService.refreshParticipantsIfStale(1L);

        verify(ltiContextEntity).setLastParticipantSync(null);
        verify(ltiContextRepository).save(ltiContextEntity);
    }

    // last_participant_sync stays permanently null for a course at/below the min-participants
    // threshold, so it alone can't prevent a second, independent staleness check (e.g. the
    // manual-participation page's own refresh=true fetch, moments after the participation-type
    // wizard's own refresh already ran) from starting its own redundant sync and its own
    // LmsUserBatchProcessing row. The debounce against the most recent sync attempt for the
    // course - regardless of participant count - is what actually prevents that second sync.
    @Test
    public void testRefreshParticipantsIfStaleSkipsWhenRecentSyncAttemptExistsForContext() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        // this experiment already has participants, so the course-level freshness check applies
        // instead of being bypassed as a first-ever sync
        when(participantRepository.countByExperiment_ExperimentId(1L)).thenReturn(5L);

        LmsUserBatchProcessing recentAttempt = new LmsUserBatchProcessing();
        recentAttempt.setCreatedAt(Timestamp.from(Instant.now()));
        when(lmsUserBatchProcessingRepository.findFirstByContextIdAndBatchIdNotOrderByCreatedAtDesc(eq(1L), any(UUID.class))).thenReturn(Optional.of(recentAttempt));

        participantService.refreshParticipantsIfStale(1L);

        verify(participantService, never()).refreshParticipants(anyLong(), any());
        verify(ltiContextRepository, never()).save(any(LtiContextEntity.class));
    }

    @Test
    public void testRefreshParticipantsIfStaleRefreshesWhenPriorSyncAttemptOutsideDebounceWindow() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        doNothing().when(participantService).refreshParticipants(anyLong(), any(UUID.class));

        LmsUserBatchProcessing staleAttempt = new LmsUserBatchProcessing();
        staleAttempt.setCreatedAt(Timestamp.from(Instant.now().minusSeconds(301)));
        when(lmsUserBatchProcessingRepository.findFirstByContextIdAndBatchIdNotOrderByCreatedAtDesc(eq(1L), any(UUID.class))).thenReturn(Optional.of(staleAttempt));
        when(participantRepository.countByExperiment_ExperimentId(1L)).thenReturn(5L);

        participantService.refreshParticipantsIfStale(1L);

        verify(participantService).refreshParticipants(eq(1L), any(UUID.class));
    }

    // startPrepareParticipation creates the IN_PROGRESS row and hands its batch ID to
    // prepareParticipationAsync, which reuses it here - the debounce lookup must exclude that
    // exact batch ID, or this method would find its own just-created row (created moments ago,
    // for this very call) and wrongly conclude the context was already synced, skipping the sync
    // it was actually supposed to perform and leaving the batch's message perpetually null
    @Test
    public void testRefreshParticipantsIfStaleIgnoresItsOwnJustCreatedRowInDebounceCheck() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        doNothing().when(participantService).refreshParticipants(anyLong(), any(UUID.class));
        when(participantRepository.countByExperiment_ExperimentId(1L)).thenReturn(5L);

        UUID batchId = UUID.randomUUID();
        // no other row exists for this context once the current batch is excluded
        when(lmsUserBatchProcessingRepository.findFirstByContextIdAndBatchIdNotOrderByCreatedAtDesc(1L, batchId)).thenReturn(Optional.empty());

        participantService.refreshParticipantsIfStale(1L, batchId);

        verify(participantService).refreshParticipants(1L, batchId);
        verify(lmsUserBatchProcessingRepository, never()).findFirstByContextIdOrderByCreatedAtDesc(anyLong());
    }

    // refreshParticipantsIfStale(experimentId, batchId) must reuse the exact batchId a caller
    // (e.g. prepareParticipation) already has a tracking record for, rather than generating a
    // fresh one internally - otherwise the LMS sync ends up recorded under a second,
    // disconnected LmsUserBatchProcessing row for what is logically a single refresh.
    @Test
    public void testRefreshParticipantsIfStaleReusesProvidedBatchId() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        doNothing().when(participantService).refreshParticipants(anyLong(), any(UUID.class));

        UUID batchId = UUID.randomUUID();
        participantService.refreshParticipantsIfStale(1L, batchId);

        verify(participantService).refreshParticipants(1L, batchId);
    }

    // a brand-new experiment sharing a course with another, already-synced experiment must still
    // get its own first sync - last_participant_sync being "fresh" describes the course's LMS
    // roster, not whether this specific experiment has any Participant rows yet
    @Test
    public void testRefreshParticipantsIfStaleRefreshesNewExperimentDespiteFreshCourseSync() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(Instant.now());
        when(participantRepository.countByExperiment_ExperimentId(1L)).thenReturn(0L);
        doNothing().when(participantService).refreshParticipants(anyLong(), any(UUID.class));

        participantService.refreshParticipantsIfStale(1L);

        verify(participantService).refreshParticipants(eq(1L), any(UUID.class));
    }

    @Test
    public void testRefreshParticipantsIfStaleSkipsWhenRecentlySynced() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(Instant.now());
        // this experiment already has participants, so the course-level freshness check applies
        // instead of being bypassed as a first-ever sync
        when(participantRepository.countByExperiment_ExperimentId(1L)).thenReturn(5L);

        participantService.refreshParticipantsIfStale(1L);

        verify(participantService, never()).refreshParticipants(anyLong(), any());
        verify(ltiContextRepository, never()).save(any(LtiContextEntity.class));
    }

    @Test
    public void testRefreshParticipantsIfStaleRefreshesAgainWhenSyncIsStale() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(Instant.now().minus(Duration.ofHours(25)));
        doNothing().when(participantService).refreshParticipants(anyLong(), any(UUID.class));

        participantService.refreshParticipantsIfStale(1L);

        verify(participantService).refreshParticipants(eq(1L), any(UUID.class));
        verify(ltiContextRepository).save(ltiContextEntity);
    }

    // a failed sync must not leave last_participant_sync showing a partial-failure value. Since
    // the field is only ever written on the success path (below the refreshParticipants call),
    // and the named roster-sync lock guarantees nothing else can have changed it out from under
    // this method, a failure simply never reaches that write - correctly leaving whatever value
    // was there before this attempt (including null, if never synced before).
    @Test
    public void testRefreshParticipantsIfStaleLeavesSyncTimestampUntouchedOnFailureWhenNeverSyncedBefore() throws Exception {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        doThrow(new ParticipantNotUpdatedException("failed")).when(participantService).refreshParticipants(anyLong(), any(UUID.class));

        assertThrows(
            ParticipantNotUpdatedException.class,
            () -> participantService.refreshParticipantsIfStale(1L)
        );

        verify(ltiContextEntity, never()).setLastParticipantSync(any(Instant.class));
        verify(ltiContextRepository, never()).save(any(LtiContextEntity.class));
    }

    // covers the exact production scenario: a RuntimeException (lock timeout / duplicate key)
    // bubbling straight out of refreshParticipants, uncaught by its own connector-exception
    // handling - the sync timestamp must still be left untouched (not partially updated).
    @Test
    public void testRefreshParticipantsIfStaleLeavesSyncTimestampUntouchedOnRuntimeExceptionFailure() throws Exception {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        doThrow(new DataIntegrityViolationException("Duplicate entry")).when(participantService).refreshParticipants(anyLong(), any(UUID.class));

        assertThrows(
            DataIntegrityViolationException.class,
            () -> participantService.refreshParticipantsIfStale(1L)
        );

        verify(ltiContextRepository, never()).save(any(LtiContextEntity.class));
    }

    // the freshness pre-check (before acquiring the lock) avoids the DB round trip entirely when
    // clearly fresh; the re-check (after acquiring the lock, via the freshly-refreshed entity) is
    // what actually prevents a double-refresh - covered by the "refreshes when never synced"/
    // "skips when recently synced" tests already exercising isParticipantSyncFresh, plus this
    // explicit check that the named lock is what's used for the real serialization guarantee.
    @Test
    public void testRefreshParticipantsIfStaleAcquiresNamedLockBeforeRefreshing() throws Exception {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        doNothing().when(participantService).refreshParticipants(anyLong(), any(UUID.class));

        participantService.refreshParticipantsIfStale(1L);

        verify(query, atLeastOnce()).setParameter("lockName", "terracotta_participant_roster_sync_context_" + ltiContextEntity.getContextId());
        verify(entityManager).refresh(ltiContextEntity);

        verify(participantService, times(1)).refreshParticipants(eq(1L), any(UUID.class));
    }

    // if another sync for this course is already in progress and holding the lock, skip rather
    // than waiting indefinitely or duplicating its work
    @Test
    public void testRefreshParticipantsIfStaleSkipsWhenLockNotAcquired() throws Exception {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        when(query.getSingleResult()).thenReturn(0);

        participantService.refreshParticipantsIfStale(1L);

        verify(participantService, never()).refreshParticipants(anyLong(), any(UUID.class));
        verify(ltiContextRepository, never()).save(any(LtiContextEntity.class));
    }

    // prepareParticipation should throttle the roster sync (rather than always syncing) and
    // reset consent for existing participants directly either way.
    @Test
    public void testPrepareParticipation() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());
        when(participant.getSource()).thenReturn(ParticipationTypes.AUTO);
        doNothing().when(participantService).refreshParticipants(anyLong(), any(UUID.class));

        UUID batchId = UUID.randomUUID();
        assertDoesNotThrow(() -> participantService.prepareParticipation(1L, securedInfo, batchId));

        // the same batchId passed in must flow through to refreshParticipantsIfStale - not a
        // freshly generated one - otherwise the LMS sync ends up tracked under a second,
        // disconnected batch ID (see LmsUserBatchWriteServiceImplTest for the row-level half of
        // this fix)
        verify(participantService).refreshParticipantsIfStale(1L, batchId);
        verify(participantService).resetParticipantConsentIfExperimentNotStarted(experiment, participant);
    }

    // when the roster was already synced recently, prepareParticipation should skip the sync
    // but still reset consent for existing participants.
    @Test
    public void testPrepareParticipationSkipsSyncWhenRecentlySynced() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());
        when(participant.getSource()).thenReturn(ParticipationTypes.AUTO);
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(Instant.now());
        // this experiment already has participants, so the course-level freshness check applies
        // instead of being bypassed as a first-ever sync
        when(participantRepository.countByExperiment_ExperimentId(1L)).thenReturn(5L);

        assertDoesNotThrow(() -> participantService.prepareParticipation(1L, securedInfo, UUID.randomUUID()));

        verify(participantService, never()).refreshParticipants(anyLong(), any());
        verify(participantService).resetParticipantConsentIfExperimentNotStarted(experiment, participant);
    }

    // startPrepareParticipation must return immediately (no LMS call), leaving a durable
    // IN_PROGRESS marker that the async job and the polling endpoint both key off of - when the
    // roster is actually due for a sync.
    @Test
    public void testStartPrepareParticipationSavesInProgressRecordAndReturnsDtoWhenStale() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);

        LmsUserBatchStatusDto result = participantService.startPrepareParticipation(1L, securedInfo);

        assertNotNull(result.getBatchId());
        assertEquals(LmsUserBatchStatus.IN_PROGRESS, result.getStatus());

        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository).save(captor.capture());
        assertEquals(result.getBatchId(), captor.getValue().getBatchId());
        assertEquals(LmsUserBatchStatus.IN_PROGRESS, captor.getValue().getStatus());
        assertEquals(Long.valueOf(ltiContextEntity.getContextId()), captor.getValue().getContextId());
        verify(participantService, never()).prepareParticipation(anyLong(), any(), any());
    }

    // when the roster isn't due for a sync, prepareParticipation only does a fast, local
    // consent reset anyway - so run it synchronously and report COMPLETED immediately, instead
    // of making the caller wait out a full poll cycle for work that's already done.
    @Test
    public void testStartPrepareParticipationRunsSynchronouslyAndReturnsCompletedWhenNotStale() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(Instant.now());
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(Collections.emptyList());

        LmsUserBatchStatusDto result = participantService.startPrepareParticipation(1L, securedInfo);

        assertNotNull(result.getBatchId());
        assertEquals(LmsUserBatchStatus.COMPLETED, result.getStatus());
        // the batchId passed to prepareParticipation is the same one returned in the DTO - not a
        // second, unrelated batch ID that would otherwise leave a duplicate tracking row
        verify(participantService).prepareParticipation(1L, securedInfo, result.getBatchId());
        verify(lmsUserBatchProcessingRepository, never()).save(any(LmsUserBatchProcessing.class));
    }

    // the freshness pre-check happens under the LTI context's named roster-sync lock (see
    // refreshParticipantsIfStale) rather than the unlocked read that isParticipantSyncStale would
    // otherwise use - so a rapid double-submit or a second overlapping request for the same
    // course serializes here instead of each independently deciding "stale"
    @Test
    public void testStartPrepareParticipationAcquiresNamedLockBeforeDecidingStale() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);

        participantService.startPrepareParticipation(1L, securedInfo);

        verify(query, atLeastOnce()).setParameter("lockName", "terracotta_participant_roster_sync_context_" + ltiContextEntity.getContextId());
        verify(ltiContextRepository).findById(ltiContextEntity.getContextId());
    }

    // a lock acquisition timeout (another sync already in progress for this context) must
    // surface as a clean, typed failure rather than an uncaught DB timeout exception reaching
    // the controller
    @Test
    public void testStartPrepareParticipationThrowsWhenLockNotAcquired() {
        when(query.getSingleResult()).thenReturn(0);

        assertThrows(
            ParticipantNotUpdatedException.class,
            () -> participantService.startPrepareParticipation(1L, securedInfo)
        );
    }

    // a rapid double-submit or a second overlapping request for the same course can each reach
    // startPrepareParticipation before either one's async refresh has had a chance to mark the
    // roster fresh - the second one must reuse the batch already in flight for this LTI context
    // instead of creating a second, independently-tracked LmsUserBatchProcessing row
    @Test
    public void testStartPrepareParticipationReusesInProgressBatchForSameContextRatherThanCreatingNew() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(ltiContextEntity.getLastParticipantSync()).thenReturn(null);
        UUID existingBatchId = UUID.randomUUID();
        LmsUserBatchProcessing inProgress = LmsUserBatchProcessing.builder().batchId(existingBatchId).status(LmsUserBatchStatus.IN_PROGRESS).build();
        when(lmsUserBatchProcessingRepository.findFirstByContextIdAndStatus(ltiContextEntity.getContextId(), LmsUserBatchStatus.IN_PROGRESS)).thenReturn(Optional.of(inProgress));

        LmsUserBatchStatusDto result = participantService.startPrepareParticipation(1L, securedInfo);

        assertEquals(existingBatchId, result.getBatchId());
        assertEquals(LmsUserBatchStatus.IN_PROGRESS, result.getStatus());
        verify(lmsUserBatchProcessingRepository, never()).save(any(LmsUserBatchProcessing.class));
    }

    @Test
    public void testGetPrepareParticipationStatusReturnsDtoWhenFound() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchProcessing lmsUserBatchProcessing = LmsUserBatchProcessing.builder()
            .batchId(batchId)
            .status(LmsUserBatchStatus.COMPLETED)
            .message("done")
            .build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.of(lmsUserBatchProcessing));

        Optional<LmsUserBatchStatusDto> result = participantService.getPrepareParticipationStatus(batchId);

        assertTrue(result.isPresent());
        assertEquals(batchId, result.get().getBatchId());
        assertEquals(LmsUserBatchStatus.COMPLETED, result.get().getStatus());
        assertEquals("done", result.get().getMessage());
    }

    @Test
    public void testGetPrepareParticipationStatusReturnsEmptyWhenNotFound() {
        UUID batchId = UUID.randomUUID();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.empty());

        Optional<LmsUserBatchStatusDto> result = participantService.getPrepareParticipationStatus(batchId);

        assertTrue(result.isEmpty());
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

        verify(participantService, never()).refreshParticipants(anyLong());
        verify(participant).setConsent(true);
        verify(participantRepository).saveAll(List.of(participant));
    }

    @Test
    public void testSetAllToFalse() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());

        assertDoesNotThrow(() -> participantService.setAllToFalse(1L));

        verify(participantService, never()).refreshParticipants(anyLong());
        verify(participant).setConsent(false);
    }

    @Test
    public void testSetAllToNull() throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any())).thenReturn(List.of(participant), Collections.emptyList());

        assertDoesNotThrow(() -> participantService.setAllToNull(1L));

        verify(participantService, never()).refreshParticipants(anyLong());
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
