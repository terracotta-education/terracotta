package edu.iu.terracotta.dao.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;

/**
 * {@link Participant} is a Lombok {@code @Builder} JPA entity extending {@code BaseUuidEntity}.
 * These tests exercise the hand-written {@code @Transient} helper methods: {@code isTestStudent()},
 * and the {@code getParticipantId()}/{@code setParticipantId()} pair that aliases the inherited
 * {@code id} field from {@code BaseUuidEntity}.
 */
public class ParticipantTest {

    @Test
    public void testIsTestStudentTrueWhenLtiUserEntityNull() {
        Participant participant = Participant.builder()
            .ltiUserEntity(null)
            .build();

        assertTrue(participant.isTestStudent());
    }

    @Test
    public void testIsTestStudentTrueWhenDelegateReturnsTrue() {
        LtiUserEntity ltiUserEntity = LtiUserEntity.builder()
            .displayName(LtiUserEntity.TEST_STUDENT_DISPLAY_NAME)
            .build();

        Participant participant = Participant.builder()
            .ltiUserEntity(ltiUserEntity)
            .build();

        assertTrue(participant.isTestStudent());
    }

    @Test
    public void testIsTestStudentFalseWhenDelegateReturnsFalse() {
        LtiUserEntity ltiUserEntity = LtiUserEntity.builder()
            .displayName("Not A Test Student")
            .build();

        Participant participant = Participant.builder()
            .ltiUserEntity(ltiUserEntity)
            .build();

        assertFalse(participant.isTestStudent());
    }

    @Test
    public void testGetSetParticipantIdAliasesInheritedIdField() {
        Participant participant = Participant.builder().build();

        participant.setParticipantId(42L);

        assertEquals(42L, participant.getParticipantId());
        // confirm setParticipantId writes the same field the inherited getId() reads
        assertEquals(42L, participant.getId());

        participant.setId(99L);

        // confirm setId (inherited) writes the same field getParticipantId() reads
        assertEquals(99L, participant.getParticipantId());
    }

}
