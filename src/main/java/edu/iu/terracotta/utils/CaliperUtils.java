package edu.iu.terracotta.utils;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Participant;
import lombok.experimental.UtilityClass;

import org.imsglobal.caliper.entities.EntityType;
import org.imsglobal.caliper.entities.agent.CaliperOrganization;
import org.imsglobal.caliper.entities.agent.CourseSection;
import org.imsglobal.caliper.entities.agent.Person;
import org.imsglobal.caliper.entities.agent.SoftwareApplication;
import org.imsglobal.caliper.entities.session.LtiSession;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class CaliperUtils {

    public Person prepareActor(Participant participant, String lmsGlobalId, String applicationUrl) {
        return buildActor(applicationUrl, participant.getLtiMembershipEntity(), getExtensions(participant, lmsGlobalId));
    }

    public Map<String, Object> getExtensions(Participant participant, String lmsGlobalId) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.putAll(getExtensions(participant.getLtiMembershipEntity(), lmsGlobalId));
        extensions.put("terracotta_participant_id", participant.getParticipantId());

        return extensions;
    }

    public Person buildActor(String applicationUrl, LtiMembershipEntity ltiMembershipEntity, Map<String, Object> extensions) {
        return Person.builder()
                .id(applicationUrl + "/users/" + ltiMembershipEntity.getUser().getUserId())
                .extensions(extensions)
                .type(EntityType.PERSON)
                .build();
    }

    public Map<String, Object> getExtensions(LtiMembershipEntity ltiMembershipEntity, String lmsGlobalId) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("lms_global_id", lmsGlobalId);
        extensions.put("lti_id", ltiMembershipEntity.getUser().getUserKey());
        extensions.put("lti_tenant", ltiMembershipEntity.getUser().getPlatformDeployment().getBaseUrl());

        return extensions;
    }

    public SoftwareApplication prepareSoftwareApplication(String applicationName, String applicationUrl) {
        return SoftwareApplication.builder()
                .name(applicationName)
                .id(applicationUrl)
                .build();
    }

    public String roleToString(int role) {
        switch (role) {
            case 2:
                return LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN;
            case LtiStrings.ROLE_INSTRUCTOR:
                return LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR;
            case LtiStrings.ROLE_STUDENT:
                return LtiStrings.LTI_ROLE_LEARNER;
            default:
                return null;
        }
    }

    public LtiSession prepareLtiSession(String applicationUrl, SecuredInfo securedInfo, String contextId) {
        Map<String, Object> messageParameters = new HashMap<>();
        messageParameters.put("lms_course_id", securedInfo.getLmsCourseId());

        if (securedInfo.getLmsAssignmentId() != null && !securedInfo.getLmsAssignmentId().startsWith("$")) {
            messageParameters.put("lms_assignment_id", securedInfo.getLmsAssignmentId());
        }

        messageParameters.put("lms_user_id", securedInfo.getLmsUserId());
        messageParameters.put("lms_login_id", securedInfo.getLmsLoginId());
        messageParameters.put("lms_user_global_id", securedInfo.getLmsUserGlobalId());
        messageParameters.put("lms_roles", securedInfo.getRoles());
        messageParameters.put("lms_user_name", securedInfo.getLmsUserName());
        messageParameters.put("lti_context_id", contextId);

        return LtiSession.builder()
            .id("urn:session_id_localized:" + applicationUrl + "/lti/oauth_nonce/" + securedInfo.getNonce())
            .type(EntityType.LTI_SESSION)
            .messageParameters(messageParameters)
            .build();
    }

    public CaliperOrganization prepareGroup(LtiMembershipEntity participant, SecuredInfo securedInfo) {
        String lmsCourseId = participant.getUser().getPlatformDeployment().getBaseUrl() + "/courses/" + securedInfo.getLmsCourseId();

        return CourseSection.builder()
                .name(participant.getContext().getTitle())
                .id(lmsCourseId)
                .type(EntityType.COURSE_OFFERING).build();
    }

}
