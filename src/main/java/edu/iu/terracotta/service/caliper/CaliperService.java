package edu.iu.terracotta.service.caliper;

import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.imsglobal.caliper.Envelope;

public interface CaliperService {

    void send(Envelope envelope);

    void sendAssignmentStarted(Submission submission, SecuredInfo securedInfo);

    void sendAssignmentSubmitted(Submission submission, SecuredInfo securedInfo);

    void sendAssignmentRestarted(Submission submission, SecuredInfo securedInfo);

    void sendNavigationEvent(Participant participant, String whereTo, SecuredInfo securedInfo);

    void sendFeedbackEvent(Participant participant, Assessment assessment, SecuredInfo securedInfo);

    void sendViewGradeEvent(Participant participant, Assessment assessment, SecuredInfo securedInfo);

    void sendToolUseEvent(LtiMembershipEntity membershipEntity, String canvasUserGlobalId, String canvasCourseId);


}
