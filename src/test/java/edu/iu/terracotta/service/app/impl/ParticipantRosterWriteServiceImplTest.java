package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.Participant;

public class ParticipantRosterWriteServiceImplTest extends BaseTest {

    private static final String NEW_USER_KEY = "new_user_key";

    @InjectMocks
    private ParticipantRosterWriteServiceImpl participantRosterWriteService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        // @InjectMocks only does constructor injection here, so the separate @PersistenceContext
        // EntityManager field is never populated unless set explicitly
        ReflectionTestUtils.setField(participantRosterWriteService, "entityManager", entityManager);

        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void testSyncParticipantsPageMatchesExistingParticipant() {
        LmsUserBatch batchUser = LmsUserBatch.builder().userKey(USER_ID).email(EMAIL).name(DISPLAY_NAME).build();

        when(participantRepository.findAllByExperiment_ExperimentIdAndLtiUserEntity_UserKeyIn(anyLong(), anyList())).thenReturn(List.of(participant));

        assertDoesNotThrow(() -> participantRosterWriteService.syncParticipantsPage(experiment, List.of(batchUser)));

        verify(participant).setDropped(true);
        verify(participant).setDropped(false);
        verify(participantRepository).save(participant);
        verify(ltiDataService, never()).findAllByUserKeysAndPlatformDeployment(anyList(), any());
        verify(entityManager).flush();
        verify(entityManager).clear();
    }

    // batch-fetches the LtiUserEntity for anyone not already a participant on this page, instead
    // of one query per new user.
    @Test
    public void testSyncParticipantsPageBatchesLookupForUnmatchedUsers() {
        LmsUserBatch batchUser = LmsUserBatch.builder().userKey(NEW_USER_KEY).email(EMAIL).name(DISPLAY_NAME).build();

        when(participantRepository.findAllByExperiment_ExperimentIdAndLtiUserEntity_UserKeyIn(anyLong(), anyList())).thenReturn(Collections.emptyList());
        when(ltiDataService.findAllByUserKeysAndPlatformDeployment(anyList(), any())).thenReturn(List.of(ltiUserEntity));
        when(ltiUserEntity.getUserKey()).thenReturn(NEW_USER_KEY);
        when(ltiDataService.findAllByUsersAndContext(anyList(), any())).thenReturn(List.of(ltiMembershipEntity));
        when(ltiMembershipEntity.getUser()).thenReturn(ltiUserEntity);

        assertDoesNotThrow(() -> participantRosterWriteService.syncParticipantsPage(experiment, List.of(batchUser)));

        ArgumentCaptor<List<String>> userKeysCaptor = ArgumentCaptor.forClass(List.class);
        verify(ltiDataService).findAllByUserKeysAndPlatformDeployment(userKeysCaptor.capture(), any());
        assertEquals(List.of(NEW_USER_KEY), userKeysCaptor.getValue());
        verify(ltiDataService, never()).saveLtiUserEntity(any());
        verify(ltiDataService, never()).findByUserAndContext(any(), any());
        verify(ltiDataService, never()).saveLtiMembershipEntity(any());
    }

    // when a batched LtiUserEntity lookup finds no existing user, one is created (and a new
    // membership created for it, since a brand-new user can't already have one).
    @Test
    public void testSyncParticipantsPageCreatesNewUserAndMembershipWhenNoneExist() {
        LmsUserBatch batchUser = LmsUserBatch.builder().userKey(NEW_USER_KEY).email(EMAIL).name(DISPLAY_NAME).build();
        LtiUserEntity newLtiUserEntity = org.mockito.Mockito.mock(LtiUserEntity.class);
        LtiMembershipEntity newLtiMembershipEntity = org.mockito.Mockito.mock(LtiMembershipEntity.class);

        when(participantRepository.findAllByExperiment_ExperimentIdAndLtiUserEntity_UserKeyIn(anyLong(), anyList())).thenReturn(Collections.emptyList());
        when(ltiDataService.findAllByUserKeysAndPlatformDeployment(anyList(), any())).thenReturn(Collections.emptyList());
        when(ltiDataService.saveLtiUserEntity(any())).thenReturn(newLtiUserEntity);
        when(ltiDataService.findByUserAndContext(newLtiUserEntity, ltiContextEntity)).thenReturn(null);
        when(ltiDataService.saveLtiMembershipEntity(any())).thenReturn(newLtiMembershipEntity);

        assertDoesNotThrow(() -> participantRosterWriteService.syncParticipantsPage(experiment, List.of(batchUser)));

        verify(ltiDataService).saveLtiUserEntity(any());
        verify(ltiDataService).saveLtiMembershipEntity(any());

        ArgumentCaptor<Participant> participantCaptor = ArgumentCaptor.forClass(Participant.class);
        verify(participantRepository, times(2)).save(participantCaptor.capture());
        assertEquals(newLtiUserEntity, participantCaptor.getValue().getLtiUserEntity());
        assertEquals(newLtiMembershipEntity, participantCaptor.getValue().getLtiMembershipEntity());
    }

    // an existing LtiUserEntity found via the batched lookup may already have a membership in
    // this same context (e.g. from another experiment sharing the course) - must reuse it rather
    // than creating a duplicate.
    @Test
    public void testSyncParticipantsPageReusesExistingMembershipForExistingUser() {
        LmsUserBatch batchUser = LmsUserBatch.builder().userKey(NEW_USER_KEY).email(EMAIL).name(DISPLAY_NAME).build();

        when(participantRepository.findAllByExperiment_ExperimentIdAndLtiUserEntity_UserKeyIn(anyLong(), anyList())).thenReturn(Collections.emptyList());
        when(ltiDataService.findAllByUserKeysAndPlatformDeployment(anyList(), any())).thenReturn(List.of(ltiUserEntity));
        when(ltiUserEntity.getUserKey()).thenReturn(NEW_USER_KEY);
        when(ltiUserEntity.getUserId()).thenReturn(99L);
        when(ltiDataService.findAllByUsersAndContext(anyList(), any())).thenReturn(List.of(ltiMembershipEntity));
        when(ltiMembershipEntity.getUser()).thenReturn(ltiUserEntity);

        assertDoesNotThrow(() -> participantRosterWriteService.syncParticipantsPage(experiment, List.of(batchUser)));

        verify(ltiDataService, never()).saveLtiUserEntity(any());
        verify(ltiDataService, never()).saveLtiMembershipEntity(any());

        ArgumentCaptor<Participant> participantCaptor = ArgumentCaptor.forClass(Participant.class);
        verify(participantRepository, times(2)).save(participantCaptor.capture());
        assertEquals(ltiUserEntity, participantCaptor.getValue().getLtiUserEntity());
        assertEquals(ltiMembershipEntity, participantCaptor.getValue().getLtiMembershipEntity());
    }

    // a roster entry with no usable user identifier (e.g. a pending/placeholder LMS enrollment)
    // must be skipped rather than reaching LtiUserEntity's constructor, which asserts on a blank
    // userKey and would take down this whole page's transaction along with every other
    // participant in it.
    @Test
    public void testSyncParticipantsPageSkipsBlankUserKeysWithoutAffectingOthers() {
        LmsUserBatch validBatchUser = LmsUserBatch.builder().userKey(USER_ID).email(EMAIL).name(DISPLAY_NAME).build();
        LmsUserBatch blankBatchUser = LmsUserBatch.builder().userKey("").email("blank@example.com").name("Blank User").build();

        when(participantRepository.findAllByExperiment_ExperimentIdAndLtiUserEntity_UserKeyIn(anyLong(), anyList())).thenReturn(List.of(participant));

        assertDoesNotThrow(() -> participantRosterWriteService.syncParticipantsPage(experiment, List.of(validBatchUser, blankBatchUser)));

        verify(participant).setDropped(false);
        verify(participantRepository).save(participant);
        verify(ltiDataService, never()).saveLtiUserEntity(any());
    }

    // userKey matching (both against existing participants and the batched LtiUserEntity lookup)
    // must be case-insensitive, same as the single-user queries this replaced.
    @Test
    public void testSyncParticipantsPageMatchesExistingParticipantCaseInsensitively() {
        LmsUserBatch batchUser = LmsUserBatch.builder().userKey(USER_ID.toUpperCase()).email(EMAIL).name(DISPLAY_NAME).build();

        when(participantRepository.findAllByExperiment_ExperimentIdAndLtiUserEntity_UserKeyIn(anyLong(), anyList())).thenReturn(List.of(participant));

        assertDoesNotThrow(() -> participantRosterWriteService.syncParticipantsPage(experiment, List.of(batchUser)));

        verify(participant).setDropped(false);
        verify(ltiDataService, never()).findAllByUserKeysAndPlatformDeployment(anyList(), any());
    }

}
