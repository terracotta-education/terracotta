package edu.iu.terracotta.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;

public class ParticipantConsentUtilsTest {

    @Test
    public void testNoopWhenExperimentAlreadyStarted() {
        Experiment experiment = Experiment.builder().started(new Timestamp(System.currentTimeMillis())).participationType(ParticipationTypes.AUTO).build();
        Participant participant = Participant.builder().source(ParticipationTypes.NOSET).build();

        ParticipantConsentUtils.resetConsentIfExperimentNotStarted(experiment, participant);

        assertEquals(ParticipationTypes.NOSET, participant.getSource());
        assertNull(participant.getConsent());
    }

    @Test
    public void testNoopWhenSourceAlreadyMatchesParticipationType() {
        Experiment experiment = Experiment.builder().participationType(ParticipationTypes.AUTO).build();
        Participant participant = Participant.builder().source(ParticipationTypes.AUTO).consent(true).build();

        ParticipantConsentUtils.resetConsentIfExperimentNotStarted(experiment, participant);

        assertEquals(ParticipationTypes.AUTO, participant.getSource());
        assertEquals(true, participant.getConsent());
    }

    @Test
    public void testNosetToAutoSetsConsentTrueAndDateGiven() {
        Experiment experiment = Experiment.builder().participationType(ParticipationTypes.AUTO).build();
        Participant participant = Participant.builder().source(ParticipationTypes.NOSET).build();

        ParticipantConsentUtils.resetConsentIfExperimentNotStarted(experiment, participant);

        assertEquals(ParticipationTypes.AUTO, participant.getSource());
        assertEquals(true, participant.getConsent());
        assertEquals(true, participant.getDateGiven() != null);
    }

    @Test
    public void testNosetToConsentSetsConsentFalse() {
        Experiment experiment = Experiment.builder().participationType(ParticipationTypes.CONSENT).build();
        Participant participant = Participant.builder().source(ParticipationTypes.NOSET).build();

        ParticipantConsentUtils.resetConsentIfExperimentNotStarted(experiment, participant);

        assertEquals(ParticipationTypes.CONSENT, participant.getSource());
        assertEquals(false, participant.getConsent());
    }

    @Test
    public void testAutoToManualClearsConsentAndDates() {
        Experiment experiment = Experiment.builder().participationType(ParticipationTypes.MANUAL).build();
        Participant participant = Participant.builder()
            .source(ParticipationTypes.AUTO)
            .consent(true)
            .dateGiven(new Timestamp(System.currentTimeMillis()))
            .dateRevoked(new Timestamp(System.currentTimeMillis()))
            .build();

        ParticipantConsentUtils.resetConsentIfExperimentNotStarted(experiment, participant);

        assertEquals(ParticipationTypes.MANUAL, participant.getSource());
        assertNull(participant.getConsent());
        assertNull(participant.getDateGiven());
        assertNull(participant.getDateRevoked());
    }

    @Test
    public void testConsentToAutoSetsConsentTrueAndClearsDateRevoked() {
        Experiment experiment = Experiment.builder().participationType(ParticipationTypes.AUTO).build();
        Participant participant = Participant.builder()
            .source(ParticipationTypes.CONSENT)
            .consent(false)
            .dateRevoked(new Timestamp(System.currentTimeMillis()))
            .build();

        ParticipantConsentUtils.resetConsentIfExperimentNotStarted(experiment, participant);

        assertEquals(ParticipationTypes.AUTO, participant.getSource());
        assertEquals(true, participant.getConsent());
        assertNull(participant.getDateRevoked());
    }

}
