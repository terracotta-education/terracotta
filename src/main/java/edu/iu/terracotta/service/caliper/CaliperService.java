package edu.iu.terracotta.service.caliper;

import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.media.MediaEventDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.imsglobal.caliper.Envelope;

import java.util.List;

public interface CaliperService {

    void send(Envelope envelope, PlatformDeployment platformDeployment);

    void sendAssignmentStarted(Submission submission, SecuredInfo securedInfo);

    void sendAssignmentSubmitted(Submission submission, SecuredInfo securedInfo);

    void sendAssignmentRestarted(Submission submission, SecuredInfo securedInfo);

    void sendMediaEvent(MediaEventDto mediaEventDto, Participant participant, SecuredInfo securedInfo, Submission submission, Long questionId);

    void sendNavigationEvent(Participant participant, String whereTo, SecuredInfo securedInfo);

    void sendFeedbackEvent(Participant participant, Assessment assessment, SecuredInfo securedInfo);

    void sendViewGradeEvent(Submission submission, SecuredInfo securedInfo);

    void sendToolUseEvent(LtiMembershipEntity membershipEntity,
                          String canvasUserGlobalId,
                          String canvasCourseId,
                          String canvasUserId,
                          String canvasLoginId,
                          List<String> canvasRoles,
                          String canvasUserName);


}
