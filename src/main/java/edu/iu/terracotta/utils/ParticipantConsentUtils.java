package edu.iu.terracotta.utils;

import java.sql.Timestamp;

import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Participant;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ParticipantConsentUtils {

    /**
     * If experiment hasn't started and participation type has changed, reset the participant's
     * consent. Mutates participant in place; does not persist it - callers are responsible for
     * saving.
     *
     * @param experiment
     * @param participant
     */
    public void resetConsentIfExperimentNotStarted(Experiment experiment, Participant participant) {
        if (experiment.getStarted() != null || experiment.getParticipationType() == participant.getSource()) {
            return;
        }

        switch (participant.getSource()) {
            case NOSET:
                switch (experiment.getParticipationType()) {
                    case MANUAL:
                        participant.setConsent(null);
                        break;
                    case CONSENT:
                        participant.setConsent(false);
                        break;
                    case AUTO:
                        participant.setConsent(true);
                        participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
                        break;
                    default:
                }
                break;
            case AUTO:
                switch (experiment.getParticipationType()) {
                    case MANUAL:
                        participant.setConsent(null);
                        participant.setDateGiven(null);
                        participant.setDateRevoked(null);
                        break;
                    case CONSENT:
                        participant.setConsent(false);
                        participant.setDateGiven(null);
                        participant.setDateRevoked(null);
                        break;
                    case AUTO:
                        break;
                    default:
                }
                break;
            case MANUAL:
                switch (experiment.getParticipationType()) {
                    case MANUAL:
                        break;
                    case CONSENT:
                        participant.setConsent(false);
                        participant.setDateGiven(null);
                        participant.setDateRevoked(null);
                        break;
                    case AUTO:
                        participant.setConsent(true);
                        participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
                        participant.setDateRevoked(null);
                        break;
                    default:
                }
                break;
            case CONSENT:
                switch (experiment.getParticipationType()) {
                    case MANUAL:
                        participant.setConsent(null);
                        participant.setDateGiven(null);
                        participant.setDateRevoked(null);
                        break;
                    case CONSENT:
                        break;
                    case AUTO:
                        participant.setConsent(true);
                        participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
                        participant.setDateRevoked(null);
                        break;
                    default:
                }
                break;
            default:
        }

        participant.setSource(experiment.getParticipationType());
    }

}
