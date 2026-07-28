package edu.iu.terracotta.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.imsglobal.caliper.entities.EntityType;
import org.imsglobal.caliper.entities.agent.CaliperOrganization;
import org.imsglobal.caliper.entities.agent.CourseSection;
import org.imsglobal.caliper.entities.agent.Person;
import org.imsglobal.caliper.entities.agent.SoftwareApplication;
import org.imsglobal.caliper.entities.session.LtiSession;
import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Participant;

/**
 * {@link CaliperUtils} is a Lombok {@code @UtilityClass} (all members effectively static), so no
 * instantiation or mocking framework is required. All collaborators are real, builder-constructed
 * domain objects to verify actual field wiring rather than interaction with mocks.
 */
public class CaliperUtilsTest {

    private static final String APPLICATION_URL = "https://terracotta.example.com";
    private static final String BASE_URL = "https://platform.example.com";
    private static final String LMS_GLOBAL_ID = "lms-global-id-1";

    private PlatformDeployment platformDeployment() {
        return PlatformDeployment.builder()
            .baseUrl(BASE_URL)
            .build();
    }

    private LtiUserEntity ltiUserEntity(long userId, String userKey, PlatformDeployment platformDeployment) {
        LtiUserEntity ltiUserEntity = LtiUserEntity.builder()
            .userKey(userKey)
            .platformDeployment(platformDeployment)
            .build();
        ltiUserEntity.setUserId(userId);

        return ltiUserEntity;
    }

    private LtiContextEntity ltiContextEntity(String title) {
        return LtiContextEntity.builder()
            .title(title)
            .build();
    }

    private LtiMembershipEntity ltiMembershipEntity(LtiUserEntity user, LtiContextEntity context) {
        return LtiMembershipEntity.builder()
            .user(user)
            .context(context)
            .build();
    }

    private Participant participant(LtiMembershipEntity ltiMembershipEntity, long participantId) {
        Participant participant = Participant.builder()
            .ltiMembershipEntity(ltiMembershipEntity)
            .build();
        participant.setParticipantId(participantId);

        return participant;
    }

    // prepareActor

    @Test
    public void testPrepareActor() {
        PlatformDeployment platformDeployment = platformDeployment();
        LtiUserEntity ltiUserEntity = ltiUserEntity(42L, "user-key-1", platformDeployment);
        LtiMembershipEntity ltiMembershipEntity = ltiMembershipEntity(ltiUserEntity, ltiContextEntity("Course Title"));
        Participant participant = participant(ltiMembershipEntity, 5L);

        Person actor = CaliperUtils.prepareActor(participant, LMS_GLOBAL_ID, APPLICATION_URL);

        assertEquals(APPLICATION_URL + "/users/42", actor.getId());
        assertEquals(EntityType.PERSON, actor.getType());
        assertEquals(LMS_GLOBAL_ID, actor.getExtensions().get("lms_global_id"));
        assertEquals("user-key-1", actor.getExtensions().get("lti_id"));
        assertEquals(BASE_URL, actor.getExtensions().get("lti_tenant"));
        assertEquals(5L, actor.getExtensions().get("terracotta_participant_id"));
    }

    // getExtensions(Participant, String)

    @Test
    public void testGetExtensionsFromParticipant() {
        PlatformDeployment platformDeployment = platformDeployment();
        LtiUserEntity ltiUserEntity = ltiUserEntity(7L, "user-key-2", platformDeployment);
        LtiMembershipEntity ltiMembershipEntity = ltiMembershipEntity(ltiUserEntity, ltiContextEntity("Another Course"));
        Participant participant = participant(ltiMembershipEntity, 9L);

        Map<String, Object> extensions = CaliperUtils.getExtensions(participant, LMS_GLOBAL_ID);

        assertEquals(LMS_GLOBAL_ID, extensions.get("lms_global_id"));
        assertEquals("user-key-2", extensions.get("lti_id"));
        assertEquals(BASE_URL, extensions.get("lti_tenant"));
        assertEquals(9L, extensions.get("terracotta_participant_id"));
    }

    @Test
    public void testGetExtensionsFromParticipantWithNullLmsGlobalId() {
        PlatformDeployment platformDeployment = platformDeployment();
        LtiUserEntity ltiUserEntity = ltiUserEntity(8L, "user-key-3", platformDeployment);
        LtiMembershipEntity ltiMembershipEntity = ltiMembershipEntity(ltiUserEntity, ltiContextEntity("Course"));
        Participant participant = participant(ltiMembershipEntity, 10L);

        Map<String, Object> extensions = CaliperUtils.getExtensions(participant, null);

        assertTrue(extensions.containsKey("lms_global_id"));
        assertNull(extensions.get("lms_global_id"));
        assertEquals("user-key-3", extensions.get("lti_id"));
    }

    @Test
    public void testGetExtensionsFromParticipantWithNullLtiMembershipEntityThrows() {
        Participant participant = participant(null, 11L);

        // CaliperUtils.getExtensions(Participant, String) does not null-check the LtiMembershipEntity;
        // it will NPE when it tries to read ltiMembershipEntity.getUser() via the delegate overload.
        assertThrows(NullPointerException.class, () -> CaliperUtils.getExtensions(participant, LMS_GLOBAL_ID));
    }

    // buildActor

    @Test
    public void testBuildActor() {
        PlatformDeployment platformDeployment = platformDeployment();
        LtiUserEntity ltiUserEntity = ltiUserEntity(100L, "user-key-4", platformDeployment);
        LtiMembershipEntity ltiMembershipEntity = ltiMembershipEntity(ltiUserEntity, ltiContextEntity("Course"));
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("custom_key", "custom_value");

        Person actor = CaliperUtils.buildActor(APPLICATION_URL, ltiMembershipEntity, extensions);

        assertEquals(APPLICATION_URL + "/users/100", actor.getId());
        assertEquals(EntityType.PERSON, actor.getType());
        assertEquals("custom_value", actor.getExtensions().get("custom_key"));
    }

    // getExtensions(LtiMembershipEntity, String)

    @Test
    public void testGetExtensionsFromLtiMembershipEntity() {
        PlatformDeployment platformDeployment = platformDeployment();
        LtiUserEntity ltiUserEntity = ltiUserEntity(55L, "user-key-5", platformDeployment);
        LtiMembershipEntity ltiMembershipEntity = ltiMembershipEntity(ltiUserEntity, ltiContextEntity("Course"));

        Map<String, Object> extensions = CaliperUtils.getExtensions(ltiMembershipEntity, LMS_GLOBAL_ID);

        assertEquals(3, extensions.size());
        assertEquals(LMS_GLOBAL_ID, extensions.get("lms_global_id"));
        assertEquals("user-key-5", extensions.get("lti_id"));
        assertEquals(BASE_URL, extensions.get("lti_tenant"));
    }

    @Test
    public void testGetExtensionsFromLtiMembershipEntityWithNullLmsGlobalId() {
        PlatformDeployment platformDeployment = platformDeployment();
        LtiUserEntity ltiUserEntity = ltiUserEntity(56L, "user-key-6", platformDeployment);
        LtiMembershipEntity ltiMembershipEntity = ltiMembershipEntity(ltiUserEntity, ltiContextEntity("Course"));

        Map<String, Object> extensions = CaliperUtils.getExtensions(ltiMembershipEntity, null);

        assertTrue(extensions.containsKey("lms_global_id"));
        assertNull(extensions.get("lms_global_id"));
    }

    // prepareSoftwareApplication

    @Test
    public void testPrepareSoftwareApplication() {
        SoftwareApplication application = CaliperUtils.prepareSoftwareApplication("Terracotta", APPLICATION_URL);

        assertEquals("Terracotta", application.getName());
        assertEquals(APPLICATION_URL, application.getId());
    }

    // roleToString

    @Test
    public void testRoleToStringAdmin() {
        // NOTE: `2` here is a magic number in CaliperUtils.roleToString - see final report.
        assertEquals(LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN, CaliperUtils.roleToString(2));
    }

    @Test
    public void testRoleToStringInstructor() {
        assertEquals(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR, CaliperUtils.roleToString(LtiStrings.ROLE_INSTRUCTOR));
    }

    @Test
    public void testRoleToStringStudent() {
        assertEquals(LtiStrings.LTI_ROLE_LEARNER, CaliperUtils.roleToString(LtiStrings.ROLE_STUDENT));
    }

    @Test
    public void testRoleToStringUnmatchedReturnsNull() {
        assertNull(CaliperUtils.roleToString(999));
    }

    // prepareLtiSession

    @Test
    public void testPrepareLtiSession() {
        SecuredInfo securedInfo = SecuredInfo.builder()
            .lmsCourseId("course-1")
            .lmsAssignmentId("assignment-1")
            .lmsUserId("lms-user-1")
            .lmsLoginId("login-1")
            .lmsUserGlobalId("global-1")
            .roles(List.of("Instructor"))
            .lmsUserName("Jane Doe")
            .nonce("nonce-123")
            .build();

        LtiSession session = CaliperUtils.prepareLtiSession(APPLICATION_URL, securedInfo, "context-1");

        assertEquals("urn:session_id_localized:" + APPLICATION_URL + "/lti/oauth_nonce/nonce-123", session.getId());
        assertEquals(EntityType.LTI_SESSION, session.getType());

        @SuppressWarnings("unchecked")
        Map<String, Object> messageParameters = (Map<String, Object>) session.getMessageParameters();

        assertEquals("course-1", messageParameters.get("lms_course_id"));
        assertEquals("assignment-1", messageParameters.get("lms_assignment_id"));
        assertEquals("lms-user-1", messageParameters.get("lms_user_id"));
        assertEquals("login-1", messageParameters.get("lms_login_id"));
        assertEquals("global-1", messageParameters.get("lms_user_global_id"));
        assertEquals(List.of("Instructor"), messageParameters.get("lms_roles"));
        assertEquals("Jane Doe", messageParameters.get("lms_user_name"));
        assertEquals("context-1", messageParameters.get("lti_context_id"));
    }

    @Test
    public void testPrepareLtiSessionWithDollarPrefixedAssignmentIdIsExcluded() {
        SecuredInfo securedInfo = SecuredInfo.builder()
            .lmsCourseId("course-1")
            .lmsAssignmentId("$Canvas.assignment.id")
            .lmsUserId("lms-user-1")
            .lmsLoginId("login-1")
            .lmsUserGlobalId("global-1")
            .roles(List.of("Instructor"))
            .lmsUserName("Jane Doe")
            .nonce("nonce-123")
            .build();

        LtiSession session = CaliperUtils.prepareLtiSession(APPLICATION_URL, securedInfo, "context-1");

        @SuppressWarnings("unchecked")
        Map<String, Object> messageParameters = (Map<String, Object>) session.getMessageParameters();

        assertFalse(messageParameters.containsKey("lms_assignment_id"));
    }

    @Test
    public void testPrepareLtiSessionWithNullAssignmentIdIsExcluded() {
        SecuredInfo securedInfo = SecuredInfo.builder()
            .lmsCourseId("course-1")
            .lmsAssignmentId(null)
            .lmsUserId("lms-user-1")
            .lmsLoginId("login-1")
            .lmsUserGlobalId("global-1")
            .roles(List.of("Instructor"))
            .lmsUserName("Jane Doe")
            .nonce("nonce-123")
            .build();

        LtiSession session = CaliperUtils.prepareLtiSession(APPLICATION_URL, securedInfo, "context-1");

        @SuppressWarnings("unchecked")
        Map<String, Object> messageParameters = (Map<String, Object>) session.getMessageParameters();

        assertFalse(messageParameters.containsKey("lms_assignment_id"));
    }

    // prepareGroup

    @Test
    public void testPrepareGroup() {
        PlatformDeployment platformDeployment = platformDeployment();
        LtiUserEntity ltiUserEntity = ltiUserEntity(1L, "user-key-7", platformDeployment);
        LtiContextEntity context = ltiContextEntity("My Course Title");
        LtiMembershipEntity ltiMembershipEntity = ltiMembershipEntity(ltiUserEntity, context);
        SecuredInfo securedInfo = SecuredInfo.builder()
            .lmsCourseId("course-42")
            .build();

        CaliperOrganization group = CaliperUtils.prepareGroup(ltiMembershipEntity, securedInfo);

        assertTrue(group instanceof CourseSection);
        CourseSection courseSection = (CourseSection) group;
        assertEquals(BASE_URL + "/courses/course-42", courseSection.getId());
        assertEquals("My Course Title", courseSection.getName());
        assertEquals(EntityType.COURSE_OFFERING, courseSection.getType());
    }

}
