package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.utils.TextConstants;

public class ParticipantServiceImplTest {

    @Spy
    @InjectMocks
    private ParticipantServiceImpl participantService;

    @Mock private GroupService groupService;

    @Mock private Condition condition;
    @Mock private Experiment experiment;
    @Mock private Group group;
    @Mock private Participant participant;
    @Mock private SecuredInfo securedInfo;

    @BeforeEach
    public void beforeEach() throws ParticipantNotUpdatedException, GroupNotMatchingException, AssignmentNotMatchingException {
        MockitoAnnotations.openMocks(this);

        when(condition.getConditionId()).thenReturn(1L);
        when(condition.getDefaultCondition()).thenReturn(true);
        when(experiment.getConditions()).thenReturn(Collections.singletonList(condition));
        when(experiment.getDistributionType()).thenReturn(DistributionTypes.CUSTOM);
        when(experiment.getExperimentId()).thenReturn(1L);
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.AUTO);
        when(groupService.getUniqueGroupByConditionId(anyLong(), anyString(), anyLong())).thenReturn(group);
        when(groupService.nextGroup(any(Experiment.class))).thenReturn(group);
        when(participant.getConsent()).thenReturn(false);
        when(participant.getDateGiven()).thenReturn(Timestamp.from(Instant.now()));
        when(participant.getDateRevoked()).thenReturn(Timestamp.from(Instant.now()));
        when(participant.getGroup()).thenReturn(group);
        when(securedInfo.getCanvasAssignmentId()).thenReturn("1");
        when(securedInfo.getUserId()).thenReturn("userId");

        doReturn(Collections.singletonList(participant)).when(participantService).refreshParticipants(anyLong(), any(SecuredInfo.class), anyList());
        doReturn(participant).when(participantService).findParticipant(anyList(), anyString());
        doReturn(participant).when(participantService).save(any(Participant.class));
    }

    @Test
    public void testhandleExperimentParticipantAutoParticipation() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException {
        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        assertFalse(participant.getConsent());
        assertNotNull(participant.getDateGiven());
        assertNotNull(participant.getDateRevoked());
        assertNotNull(participant.getGroup());
    }

    @Test
    public void testhandleExperimentParticipantNotAutoParticipation() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException {
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.MANUAL);
        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        assertFalse(participant.getConsent());
        assertNotNull(participant.getDateGiven());
        assertNotNull(participant.getDateRevoked());
        assertNotNull(participant.getGroup());
    }

    @Test
    public void testhandleExperimentParticipantInGroup() throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException {
        when(this.participant.getConsent()).thenReturn(true);
        Participant participant = participantService.handleExperimentParticipant(experiment, securedInfo);

        assertNotNull(participant);
        assertTrue(participant.getConsent());
        assertNotNull(participant.getDateGiven());
        assertNotNull(participant.getDateRevoked());
        assertNotNull(participant.getGroup());
    }

    @Test
    public void testhandleExperimentParticipantNoParticipant() {
        doReturn(null).when(participantService).findParticipant(anyList(),anyString());

        Exception exception = assertThrows(ParticipantNotMatchingException.class, () -> { participantService.handleExperimentParticipant(experiment, securedInfo); });

        assertEquals(TextConstants.PARTICIPANT_NOT_MATCHING, exception.getMessage());
    }

}
