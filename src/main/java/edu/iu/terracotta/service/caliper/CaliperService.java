package edu.iu.terracotta.service.caliper;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.model.dto.media.MediaEventDto;

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
    void sendToolUseEvent(LtiMembershipEntity membershipEntity, String lmsUserGlobalId, String lmsCourseId, String lmsUserId, String lmsLoginId, List<String> lmsRoles, String lmsUserName);

}
