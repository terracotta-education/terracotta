package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.ParticipantAlreadyStartedException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import edu.iu.terracotta.model.membership.CourseUser;
import edu.iu.terracotta.model.membership.CourseUsers;
import edu.iu.terracotta.model.oauth2.Roles;
import edu.iu.terracotta.utils.TextConstants;

public class ParticipantServiceImplTest extends BaseTest {

    @Spy
    @InjectMocks
    private ParticipantServiceImpl participantService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
        clearInvocations(participant);

        when(condition.getDefaultCondition()).thenReturn(true);
        when(experiment.getDistributionType()).thenReturn(DistributionTypes.CUSTOM);
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.AUTO);
        when(participant.getDateGiven()).thenReturn(Timestamp.from(Instant.now()));
        when(participant.getDateRevoked()).thenReturn(Timestamp.from(Instant.now()));
    }

    @Test
    public void testhandleExperimentParticipantAutoParticipation() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException {
        doReturn(Collections.singletonList(participant)).when(participantService).refreshParticipants(anyLong(),
                anyList());
        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        verify(participant, never()).setGroup(any(Group.class));
    }

    @Test
    public void testhandleExperimentParticipantNotAutoParticipation() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException {
        doReturn(Collections.singletonList(participant)).when(participantService).refreshParticipants(anyLong(),
                anyList());
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.MANUAL);
        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        verify(participant, never()).setGroup(any(Group.class));
    }

    @Test
    public void testhandleExperimentParticipantInGroup() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException {
        doReturn(Collections.singletonList(participant)).when(participantService).refreshParticipants(anyLong(),
                anyList());
        when(this.participant.getConsent()).thenReturn(true);

        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        verify(participant, never()).setGroup(any(Group.class));
    }

    @Test
    public void testhandleExperimentParticipantNotInAGroup() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException {
        doReturn(Collections.singletonList(participant)).when(participantService).refreshParticipants(anyLong(),
                anyList());
        when(this.participant.getConsent()).thenReturn(true);
        when(participant.getGroup()).thenReturn(null);

        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        verify(participant).setGroup(any(Group.class));
    }

    @Test
    public void testhandleExperimentParticipantNoParticipant() throws ParticipantNotUpdatedException, ExperimentNotMatchingException {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(null);
        doReturn(Collections.singletonList(participant)).when(participantService).refreshParticipants(anyLong(), anyList());
        doReturn(null).when(participantService).findParticipant(anyList(),anyString());

        Exception exception = assertThrows(ParticipantNotMatchingException.class, () -> { participantService.handleExperimentParticipant(experiment, securedInfo); });

        assertEquals(TextConstants.PARTICIPANT_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testRefreshParticipantsNoAddsNoDrops() throws ConnectionException, ParticipantNotUpdatedException, ExperimentNotMatchingException {
        PlatformDeployment platformDeployment = new PlatformDeployment();
        when(experiment.getStarted()).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(experiment.getPlatformDeployment()).thenReturn(platformDeployment);
        LtiContextEntity context = new LtiContextEntity();
        when(experiment.getLtiContextEntity()).thenReturn(context);
        when(experimentRepository.findById(experiment.getExperimentId())).thenReturn(Optional.of(experiment));

        // LTI Course Roster
        CourseUsers courseUsers = new CourseUsers();
        CourseUser courseUser1 = new CourseUser();
        courseUser1.setUserId("userKey1");
        courseUser1.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser1);
        CourseUser courseUser2 = new CourseUser();
        courseUser2.setUserId("userKey2");
        courseUser2.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser2);
        CourseUser courseUser3 = new CourseUser();
        courseUser3.setUserId("userKey3");
        courseUser3.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser3);
        when(advantageMembershipService.callMembershipService(any(), eq(context))).thenReturn(courseUsers);

        // Current Participant list
        List<Participant> currentParticipants = new ArrayList<>();
        Participant participant1 = new Participant();
        participant1.setParticipantId(1L);
        participant1.setDropped(false);
        participant1.setLtiUserEntity(new LtiUserEntity("userKey1", null, null));
        currentParticipants.add(participant1);
        Participant participant2 = new Participant();
        participant2.setParticipantId(2L);
        participant2.setDropped(false);
        participant2.setLtiUserEntity(new LtiUserEntity("userKey2", null, null));
        currentParticipants.add(participant2);
        Participant participant3 = new Participant();
        participant3.setParticipantId(3L);
        participant3.setDropped(false);
        participant3.setLtiUserEntity(new LtiUserEntity("userKey3", null, null));
        currentParticipants.add(participant3);

        List<Participant> refreshedParticipants = participantService.refreshParticipants(experiment.getExperimentId(),
                currentParticipants);

        assertEquals(refreshedParticipants.size(), currentParticipants.size());
        verify(participantRepository).flush();
        // make sure that save was never called
        verifyNoMoreInteractions(participantRepository);

    }

    // Test refreshParticipants that when a student adds the course
    @Test
    public void testRefreshParticipantsWithAddedStudent() throws ConnectionException, ParticipantNotUpdatedException, ExperimentNotMatchingException {

        PlatformDeployment platformDeployment = new PlatformDeployment();
        when(experiment.getStarted()).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(experiment.getPlatformDeployment()).thenReturn(platformDeployment);
        LtiContextEntity context = new LtiContextEntity();
        when(experiment.getLtiContextEntity()).thenReturn(context);
        when(experimentRepository.findById(experiment.getExperimentId())).thenReturn(Optional.of(experiment));

        // LTI Course Roster
        CourseUsers courseUsers = new CourseUsers();
        CourseUser courseUser1 = new CourseUser();
        courseUser1.setUserId("userKey1");
        courseUser1.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser1);
        CourseUser courseUser2 = new CourseUser();
        courseUser2.setUserId("userKey2");
        courseUser2.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser2);
        CourseUser courseUser3 = new CourseUser();
        courseUser3.setUserId("userKey3");
        courseUser3.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser3);
        // NEW CourseUser, not in participant list
        CourseUser courseUser4 = new CourseUser();
        courseUser4.setUserId("userKey4");
        courseUser4.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser4);
        when(advantageMembershipService.callMembershipService(any(), eq(context))).thenReturn(courseUsers);

        when(ltiDataService.findByUserKeyAndPlatformDeployment(courseUser4.getUserId(),
                platformDeployment)).thenReturn(null);
        when(ltiDataService.saveLtiUserEntity(argThat(u -> u.getUserKey().equals(courseUser4.getUserId()))))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.save(argThat(p -> "userKey4".equals(p.getLtiUserEntity().getUserKey()))))
                .thenAnswer(inv -> inv.getArgument(0));

        // Current Participant list
        List<Participant> currentParticipants = new ArrayList<>();
        Participant participant1 = new Participant();
        participant1.setParticipantId(1L);
        participant1.setDropped(false);
        participant1.setLtiUserEntity(new LtiUserEntity("userKey1", null, null));
        currentParticipants.add(participant1);
        Participant participant2 = new Participant();
        participant2.setParticipantId(2L);
        participant2.setDropped(false);
        participant2.setLtiUserEntity(new LtiUserEntity("userKey2", null, null));
        currentParticipants.add(participant2);
        Participant participant3 = new Participant();
        participant3.setParticipantId(3L);
        participant3.setDropped(false);
        participant3.setLtiUserEntity(new LtiUserEntity("userKey3", null, null));
        currentParticipants.add(participant3);

        List<Participant> refreshedParticipants = participantService.refreshParticipants(experiment.getExperimentId(),
                currentParticipants);

        assertEquals(4, refreshedParticipants.size());
        Optional<Participant> newParticipant = refreshedParticipants.stream()
                .filter(p -> "userKey4".equals(p.getLtiUserEntity().getUserKey())).findFirst();
        assertTrue(newParticipant.isPresent());
        verify(participantRepository).flush();
        verify(participantRepository).save(argThat(p -> "userKey4".equals(p.getLtiUserEntity().getUserKey())));
        // make sure that save was never called
        verifyNoMoreInteractions(participantRepository);

    }

    // Test refreshParticipants that when a student drops the course
    @Test
    public void testRefreshParticipantsWithDroppedStudent() throws ConnectionException, ParticipantNotUpdatedException, ExperimentNotMatchingException {

        PlatformDeployment platformDeployment = new PlatformDeployment();
        when(experiment.getStarted()).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(experiment.getPlatformDeployment()).thenReturn(platformDeployment);
        LtiContextEntity context = new LtiContextEntity();
        when(experiment.getLtiContextEntity()).thenReturn(context);
        when(experimentRepository.findById(experiment.getExperimentId())).thenReturn(Optional.of(experiment));

        // LTI Course Roster
        CourseUsers courseUsers = new CourseUsers();
        CourseUser courseUser1 = new CourseUser();
        courseUser1.setUserId("userKey1");
        courseUser1.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser1);
        CourseUser courseUser2 = new CourseUser();
        courseUser2.setUserId("userKey2");
        courseUser2.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser2);
        // This is the dropped student
        // CourseUser courseUser3 = new CourseUser();
        // courseUser3.setUserId("userKey3");
        // courseUser3.setRoles(Arrays.asList(Roles.LEARNER));
        // courseUsers.getCourseUserList().add(courseUser3);
        when(advantageMembershipService.callMembershipService(any(), eq(context))).thenReturn(courseUsers);

        when(participantRepository
                .save(argThat(p -> "userKey3".equals(p.getLtiUserEntity().getUserKey()) && p.getDropped())))
                .thenAnswer(inv -> inv.getArgument(0));

        // Current Participant list
        List<Participant> currentParticipants = new ArrayList<>();
        Participant participant1 = new Participant();
        participant1.setParticipantId(1L);
        participant1.setDropped(false);
        participant1.setLtiUserEntity(new LtiUserEntity("userKey1", null, null));
        currentParticipants.add(participant1);
        Participant participant2 = new Participant();
        participant2.setParticipantId(2L);
        participant2.setDropped(false);
        participant2.setLtiUserEntity(new LtiUserEntity("userKey2", null, null));
        currentParticipants.add(participant2);
        Participant participant3 = new Participant();
        participant3.setParticipantId(3L);
        participant3.setDropped(false);
        participant3.setLtiUserEntity(new LtiUserEntity("userKey3", null, null));
        currentParticipants.add(participant3);

        List<Participant> refreshedParticipants = participantService.refreshParticipants(experiment.getExperimentId(),
                currentParticipants);

        assertEquals(3, refreshedParticipants.size());
        verify(participantRepository)
                .save(argThat(p -> "userKey3".equals(p.getLtiUserEntity().getUserKey()) && p.getDropped()));
        verify(participantRepository).flush();
        // make sure that save was never called
        verifyNoMoreInteractions(participantRepository);

    }

    // Test refreshParticipants resets consent of all participants when
    // experiment is not started
    @Test
    public void testRefreshParticipantsResetsConsent() throws ConnectionException, ParticipantNotUpdatedException, ExperimentNotMatchingException {

        // Simulate switching from AUTO to CONSENT
        PlatformDeployment platformDeployment = new PlatformDeployment();
        when(experiment.getStarted()).thenReturn(null);
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.CONSENT);
        when(experiment.getPlatformDeployment()).thenReturn(platformDeployment);
        LtiContextEntity context = new LtiContextEntity();
        when(experiment.getLtiContextEntity()).thenReturn(context);
        when(experimentRepository.findById(experiment.getExperimentId())).thenReturn(Optional.of(experiment));

        // LTI Course Roster
        CourseUsers courseUsers = new CourseUsers();
        CourseUser courseUser1 = new CourseUser();
        courseUser1.setUserId("userKey1");
        courseUser1.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser1);
        CourseUser courseUser2 = new CourseUser();
        courseUser2.setUserId("userKey2");
        courseUser2.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser2);
        CourseUser courseUser3 = new CourseUser();
        courseUser3.setUserId("userKey3");
        courseUser3.setRoles(Arrays.asList(Roles.LEARNER));
        courseUsers.getCourseUserList().add(courseUser3);
        when(advantageMembershipService.callMembershipService(any(), eq(context))).thenReturn(courseUsers);

        // Current Participant list
        List<Participant> currentParticipants = new ArrayList<>();
        Participant participant1 = new Participant();
        participant1.setParticipantId(1L);
        participant1.setDropped(false);
        participant1.setLtiUserEntity(new LtiUserEntity("userKey1", null, null));
        participant1.setConsent(true);
        participant1.setDateGiven(new Timestamp(System.currentTimeMillis()));
        participant1.setDateRevoked(null);
        participant1.setSource(ParticipationTypes.AUTO);
        currentParticipants.add(participant1);
        Participant participant2 = new Participant();
        participant2.setParticipantId(2L);
        participant2.setDropped(false);
        participant2.setLtiUserEntity(new LtiUserEntity("userKey2", null, null));
        participant2.setConsent(true);
        participant2.setDateGiven(new Timestamp(System.currentTimeMillis()));
        participant2.setDateRevoked(null);
        participant2.setSource(ParticipationTypes.AUTO);
        currentParticipants.add(participant2);
        Participant participant3 = new Participant();
        participant3.setParticipantId(3L);
        participant3.setDropped(false);
        participant3.setLtiUserEntity(new LtiUserEntity("userKey3", null, null));
        participant3.setConsent(true);
        participant3.setDateGiven(new Timestamp(System.currentTimeMillis()));
        participant3.setDateRevoked(null);
        participant3.setSource(ParticipationTypes.AUTO);
        currentParticipants.add(participant3);

        List<Participant> refreshedParticipants = participantService.refreshParticipants(experiment.getExperimentId(),
                currentParticipants);

        assertTrue(refreshedParticipants.stream().allMatch(p -> p.getConsent() == false));
        assertTrue(refreshedParticipants.stream().allMatch(p -> p.getDateGiven() == null));
        assertTrue(refreshedParticipants.stream().allMatch(p -> p.getDateRevoked() == null));
        assertTrue(refreshedParticipants.stream().allMatch(p -> p.getSource() == ParticipationTypes.CONSENT));

        assertEquals(refreshedParticipants.size(), currentParticipants.size());
        verify(participantRepository, times(3)).save(any(Participant.class));
        verify(participantRepository).flush();
    }

    // Test handleExperimentParticipant when a student has no participant
    // record yet. refreshedParticipants should be called
    @Test
    public void testHandleExperimentParticipantAddedStudent() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException {

        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(null);
        doReturn(Collections.singletonList(participant)).when(participantService).refreshParticipants(anyLong(),
                anyList());

        participantService.handleExperimentParticipant(experiment, securedInfo);

        verify(participantService).refreshParticipants(anyLong(), anyList());
    }

    // Test handleExperimentParticipant when a student has consented but
    // hasn't been assigned a group (refreshParticipants should be called)
    @Test
    public void testHandleExperimentParticipantConsentedButNoGroup() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException {

        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(participant.getConsent()).thenReturn(true);
        when(participant.getGroup()).thenReturn(null);
        doReturn(Collections.singletonList(participant)).when(participantService).refreshParticipants(anyLong(),
                anyList());

        participantService.handleExperimentParticipant(experiment, securedInfo);

        verify(participantService).refreshParticipants(anyLong(), anyList());
    }

    // Test handleExperimentParticipant when a student has consented and been
    // assigned to a group (refreshParticipants should NOT be called)
    @Test
    public void testHandleExperimentParticipantConsentedAndHasGroup() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException {

        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(participant.getConsent()).thenReturn(true);
        when(participant.getGroup()).thenReturn(new Group());

        participantService.handleExperimentParticipant(experiment, securedInfo);

        verify(participantService, never()).refreshParticipants(anyLong(), anyList());
    }

    // Test handleExperimentParticipant when a student has not consented but
    // is marked as dropped (refreshParticipants should be called)
    @Test
    public void testHandleExperimentParticipantNotConsentedAndDropped() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException {

        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(participant.getConsent()).thenReturn(false);
        when(participant.getDropped()).thenReturn(true);
        doReturn(Collections.singletonList(participant)).when(participantService).refreshParticipants(anyLong(),
                anyList());

        participantService.handleExperimentParticipant(experiment, securedInfo);

        verify(participantService).refreshParticipants(anyLong(), anyList());
    }

    // Test handleExperimentParticipant when a student has not consented and
    // is marked as dropped (refreshParticipants should NOT be called)
    @Test
    public void testHandleExperimentParticipantNotConsentedAndNotDropped() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException {

        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(participant.getConsent()).thenReturn(false);
        when(participant.getDropped()).thenReturn(false);

        participantService.handleExperimentParticipant(experiment, securedInfo);

        verify(participantService, never()).refreshParticipants(anyLong(), anyList());
    }

    @Test
    public void testGetParticpants() {
        List<ParticipantDto> retVal = participantService.getParticipants(Arrays.asList(participant, participant), 1l, USER_ID, false, securedInfo);

        assertEquals(2, retVal.size());
    }

    @Test
    public void testGetParticpantsStudent() {
        List<ParticipantDto> retVal = participantService.getParticipants(Arrays.asList(participant, participant), 1l, USER_ID, true, securedInfo);

        assertEquals(1, retVal.size());
    }

    @Test
    public void testGetParticipant() throws InvalidUserException {
        Participant retVal = participantService.getParticipant(1l, 1l, USER_ID, false);

        assertNotNull(retVal);
    }

    @Test
    public void testGetParticipantStudent() throws InvalidUserException {
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
    public void testChangeConsent() throws ParticipantAlreadyStartedException, ExperimentNotMatchingException {
        when(securedInfo.getConsent()).thenReturn(true);

        boolean retVal = participantService.changeConsent(participantDto, securedInfo, 1L);

        assertTrue(retVal);
    }

    @Test
    public void testChangeConsentStarted() throws ParticipantAlreadyStartedException, ExperimentNotMatchingException {
        when(securedInfo.getConsent()).thenReturn(true);
        when(participant.getConsent()).thenReturn(false);
        when(participantDto.getConsent()).thenReturn(true);
        when(experiment.isSingleCondition()).thenReturn(false);
        when(treatmentRepository.findByAssignment_AssignmentId(anyLong())).thenReturn(Arrays.asList(treatment, treatment));
        when(treatmentRepository.findByCondition_Experiment_ExperimentId(anyLong())).thenReturn(Collections.emptyList());

        Exception exception = assertThrows(ParticipantAlreadyStartedException.class, () -> { participantService.changeConsent(participantDto, securedInfo, 1L); });

        assertEquals("Participant has already started experiment, consent cannot be changed to given", exception.getMessage());
    }

    @Test
    public void testFindParticipant() {
        Participant retVal = participantService.findParticipant(Collections.singletonList(participant), USER_ID);

        assertNotNull(retVal);
    }

    @Test
    public void testSetAllToNull() {
        assertDoesNotThrow(() -> {
            participantService.setAllToNull(1L, securedInfo);
        });
    }

    @Test
    public void testSetAllToTrue() {
        assertDoesNotThrow(() -> {
            participantService.setAllToTrue(1L, securedInfo);
        });
    }

    @Test
    public void testSetAllToFalse() {
        assertDoesNotThrow(() -> {
            participantService.setAllToFalse(1L, securedInfo);
        });
    }

    @Test
    public void testPostConsentSubmission() {
        assertDoesNotThrow(() -> {
            participantService.postConsentSubmission(participant, securedInfo);
        });
    }

    @Test
    public void testPostConsentSubmissionNoLineItem() {
        when(consentDocument.getResourceLinkId()).thenReturn("1");

        assertDoesNotThrow(() -> {
            participantService.postConsentSubmission(participant, securedInfo);
        });
    }
}
