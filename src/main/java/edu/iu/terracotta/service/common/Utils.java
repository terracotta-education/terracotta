package edu.iu.terracotta.service.common;

import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.utils.LtiStrings;
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
public class Utils {

    public static Person prepareActor(Participant participant, String canvasGlobalId, String applicationUrl) {
        return buildActor(applicationUrl, participant.getLtiMembershipEntity(), getExtensions(participant, canvasGlobalId));
    }

    public static Map<String, Object> getExtensions(Participant participant, String canvasGlobalId) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.putAll(getExtensions(participant.getLtiMembershipEntity(), canvasGlobalId));
        extensions.put("terracotta_participant_id", participant.getParticipantId());

        return extensions;
    }

    public static Person buildActor(String applicationUrl, LtiMembershipEntity ltiMembershipEntity, Map<String, Object> extensions) {
        return Person.builder()
                .id(applicationUrl + "/users/" + ltiMembershipEntity.getUser().getUserId())
                .extensions(extensions)
                .type(EntityType.PERSON)
                .build();
    }

    public static Map<String, Object> getExtensions(LtiMembershipEntity ltiMembershipEntity, String canvasGlobalId) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("canvas_global_id", canvasGlobalId);
        extensions.put("lti_id", ltiMembershipEntity.getUser().getUserKey());
        extensions.put("lti_tenant", ltiMembershipEntity.getUser().getPlatformDeployment().getBaseUrl());

        return extensions;
    }

    public static SoftwareApplication prepareSoftwareApplication(String applicationName, String applicationUrl) {
        return SoftwareApplication.builder()
                .name(applicationName)
                .id(applicationUrl)
                .build();
    }

    public static String roleToString(int role) {
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

    public static LtiSession prepareLtiSession(String applicationUrl, SecuredInfo securedInfo, String contextId) {
        Map<String, Object> messageParameters = new HashMap<>();
        messageParameters.put("canvas_course_id", securedInfo.getCanvasCourseId());

        if (securedInfo.getCanvasAssignmentId() != null && !securedInfo.getCanvasAssignmentId().startsWith("$")) {
            messageParameters.put("canvas_assignment_id", securedInfo.getCanvasAssignmentId());
        }

        messageParameters.put("canvas_user_id", securedInfo.getCanvasUserId());
        messageParameters.put("canvas_login_id", securedInfo.getCanvasLoginId());
        messageParameters.put("canvas_user_global_id", securedInfo.getCanvasUserGlobalId());
        messageParameters.put("canvas_roles", securedInfo.getRoles());
        messageParameters.put("canvas_user_name", securedInfo.getCanvasUserName());
        messageParameters.put("lti_context_id", contextId);

        return LtiSession.builder()
            .id("urn:session_id_localized:" + applicationUrl + "/lti/oauth_nonce/" + securedInfo.getNonce())
            .type(EntityType.LTI_SESSION)
            .messageParameters(messageParameters)
            .build();
    }

    public static CaliperOrganization prepareGroup(LtiMembershipEntity participant, SecuredInfo securedInfo) {
        String canvasCourseId = participant.getUser().getPlatformDeployment().getBaseUrl() + "/courses/" + securedInfo.getCanvasCourseId();

        return CourseSection.builder()
                .name(participant.getContext().getTitle())
                .id(canvasCourseId)
                .type(EntityType.COURSE_OFFERING).build();
    }

}
