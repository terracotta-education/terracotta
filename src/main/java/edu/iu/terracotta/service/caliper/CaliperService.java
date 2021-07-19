package edu.iu.terracotta.service.caliper;

import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Participant;
import org.imsglobal.caliper.Envelope;

public interface CaliperService {

    void send(Envelope envelope);

    void sendAssignmentStarted(Participant participant, Assessment assessment);

    void sendAssignmentSubmitted(Participant participant, Assessment assessment);

    void sendAssignmentRestarted(Participant participant, Assessment assessment);

    void sendNavigationEvent(Participant participant, String whereTo);

    void sendFeedbackEvent(Participant participant, Assessment assessment);

    void sendViewGradeEvent(Participant participant, Assessment assessment);

    void sendToolUseEvent(LtiMembershipEntity membershipEntity);


}
