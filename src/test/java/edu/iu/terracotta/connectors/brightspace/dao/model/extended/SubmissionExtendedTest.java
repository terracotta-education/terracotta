package edu.iu.terracotta.connectors.brightspace.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.brightspace.io.model.GradeValue;
import edu.iu.terracotta.connectors.brightspace.io.model.User;
import edu.iu.terracotta.connectors.brightspace.io.model.UserGradeValue;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;

public class SubmissionExtendedTest {

    // ---------- getAttempt / setAttempt ----------

    @Test
    public void testGetAndSetAttemptDelegateToSubmission() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        submissionExtended.setAttempt(3L);

        assertEquals(3L, submissionExtended.getAttempt());
        assertEquals(3L, submissionExtended.getSubmission().getAttempt());
    }

    // ---------- getScore / setScore ----------

    @Test
    public void testGetAndSetScoreDelegateToSubmission() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        submissionExtended.setScore(88.5);

        assertEquals(88.5, submissionExtended.getScore());
        assertEquals(88.5, submissionExtended.getSubmission().getScore());
    }

    // ---------- getUser / setUser ----------

    @Test
    public void testGetAndSetUserDelegateToSubmission() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        User user = User.builder().identifier("77").build();

        submissionExtended.setUser(user);

        assertEquals(user, submissionExtended.getUser());
        assertEquals(user, submissionExtended.getSubmission().getUser());
    }

    // ---------- getUserId / setUserId ----------

    @Test
    public void testGetUserIdDelegatesToSubmission() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        submissionExtended.setUserId("abc");

        assertEquals("abc", submissionExtended.getUserId());
    }

    @Test
    public void testSetUserIdWithNullNestedUserDoesNotCascadeAndDoesNotThrow() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        submissionExtended.setUserId("user-1");

        assertEquals("user-1", submissionExtended.getUserId());
        assertNull(submissionExtended.getSubmission().getUser());
    }

    @Test
    public void testSetUserIdCascadesIdentifierToNestedUserWhenPresent() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setUser(User.builder().identifier("old-id").build());

        submissionExtended.setUserId("new-id");

        assertEquals("new-id", submissionExtended.getUserId());
        assertEquals("new-id", submissionExtended.getSubmission().getUser().getIdentifier());
    }

    // ---------- getUserLoginId / setUserLoginId ----------

    @Test
    public void testGetUserLoginIdWithNullUserReturnsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        assertNull(submissionExtended.getUserLoginId());
    }

    @Test
    public void testGetUserLoginIdReturnsNestedUserUserName() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setUser(User.builder().userName("login-1").build());

        assertEquals("login-1", submissionExtended.getUserLoginId());
    }

    @Test
    public void testSetUserLoginIdWithNullUserDoesNothing() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        submissionExtended.setUserLoginId("login-2");

        assertNull(submissionExtended.getUserLoginId());
    }

    @Test
    public void testSetUserLoginIdSetsNestedUserUserName() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setUser(User.builder().build());

        submissionExtended.setUserLoginId("login-3");

        assertEquals("login-3", submissionExtended.getUserLoginId());
    }

    // ---------- getUserName / setUserName ----------

    @Test
    public void testGetUserNameWithNullUserReturnsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        assertNull(submissionExtended.getUserName());
    }

    @Test
    public void testGetUserNameReturnsNestedUserDisplayName() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setUser(User.builder().displayName("Name One").build());

        assertEquals("Name One", submissionExtended.getUserName());
    }

    @Test
    public void testSetUserNameWithNullUserDoesNothing() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        submissionExtended.setUserName("Name Two");

        assertNull(submissionExtended.getUserName());
    }

    @Test
    public void testSetUserNameSetsNestedUserDisplayName() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setUser(User.builder().build());

        submissionExtended.setUserName("Name Three");

        assertEquals("Name Three", submissionExtended.getUserName());
    }

    // ---------- from() ----------

    @Test
    public void testFromMapsAllFields() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setUser(User.builder().identifier("id-1").userName("login-1").displayName("Name One").build());
        submissionExtended.setUserId("id-1");
        submissionExtended.setAttempt(2L);
        submissionExtended.setScore(55.5);

        LmsSubmission lmsSubmission = submissionExtended.from();

        assertNotNull(lmsSubmission);
        assertEquals(2L, lmsSubmission.getAttempt());
        assertEquals(55.5, lmsSubmission.getScore());
        assertEquals(SubmissionExtended.class, lmsSubmission.getType());
        assertEquals("id-1", lmsSubmission.getUserId());
        assertEquals("login-1", lmsSubmission.getUserLoginId());
        assertEquals("Name One", lmsSubmission.getUserName());
        assertNotNull(lmsSubmission.getUser());
        assertTrue(lmsSubmission.getUser() instanceof User);
        assertEquals("id-1", ((User) lmsSubmission.getUser()).getIdentifier());
    }

    // ---------- of(LmsSubmission) ----------

    @Test
    public void testOfLmsSubmissionWithNullReturnsEmptySubmissionExtended() {
        SubmissionExtended submissionExtended = SubmissionExtended.of(null);

        assertNotNull(submissionExtended);
        assertNull(submissionExtended.getAttempt());
        assertNull(submissionExtended.getScore());
        assertNull(submissionExtended.getUserId());
        assertNull(submissionExtended.getUserLoginId());
        assertNull(submissionExtended.getUserName());
    }

    @Test
    public void testOfLmsSubmissionCopiesFields() {
        User user = User.builder().identifier("orig-id").build();
        LmsSubmission lmsSubmission = LmsSubmission.builder()
            .attempt(9L)
            .score(77.0)
            .user(user)
            .userId("user-9")
            .userLoginId("login-9")
            .userName("Name Nine")
            .build();

        SubmissionExtended submissionExtended = SubmissionExtended.of(lmsSubmission);

        assertNotNull(submissionExtended);
        assertEquals(9L, submissionExtended.getAttempt());
        assertEquals(77.0, submissionExtended.getScore());
        assertEquals("user-9", submissionExtended.getUserId());
        assertEquals("login-9", submissionExtended.getUserLoginId());
        assertEquals("Name Nine", submissionExtended.getUserName());
        assertNotNull(submissionExtended.getUser());
        assertTrue(submissionExtended.getUser() instanceof User);
        // setUserId() cascades and overwrites the nested user's identifier with userId, after setUser() populated it.
        assertEquals("user-9", ((User) submissionExtended.getUser()).getIdentifier());
    }

    // ---------- of(UserGradeValue, String, String) ----------

    @Test
    public void testOfUserGradeValueWithNullUserGradeValueReturnsEmptySubmissionExtended() {
        SubmissionExtended submissionExtended = SubmissionExtended.of(null, "assign-1", "org-1");

        assertNotNull(submissionExtended);
        assertNull(submissionExtended.getAssignmentId());
        assertNull(submissionExtended.getAttempt());
        assertNull(submissionExtended.getScore());
        assertFalse(submissionExtended.isGradeMatchesCurrentSubmission());
        assertNull(submissionExtended.getUser());
    }

    @Test
    public void testOfUserGradeValueWithValidGradeValueExtractsScore() {
        User user = User.builder().identifier("u-1").userName("login-1").displayName("Name One").build();
        GradeValue gradeValue = GradeValue.builder().pointsNumerator(42.0).build();
        UserGradeValue userGradeValue = UserGradeValue.builder().user(user).gradeValue(gradeValue).build();

        SubmissionExtended submissionExtended = SubmissionExtended.of(userGradeValue, "assign-1", "org-1");

        assertNotNull(submissionExtended);
        assertEquals(42.0, submissionExtended.getScore());
        assertEquals("assign-1", submissionExtended.getAssignmentId());
        assertEquals(1L, submissionExtended.getAttempt());
        assertTrue(submissionExtended.isGradeMatchesCurrentSubmission());
        assertNull(submissionExtended.getState());
        assertEquals("u-1", submissionExtended.getUserId());
        assertEquals("login-1", submissionExtended.getUserLoginId());
        assertEquals("Name One", submissionExtended.getUserName());
        assertNotNull(submissionExtended.getUser());
        assertTrue(submissionExtended.getUser() instanceof User);
    }

    @Test
    public void testOfUserGradeValueWithNullGradeValueDefaultsScoreToZero() {
        User user = User.builder().identifier("u-2").build();
        UserGradeValue userGradeValue = UserGradeValue.builder().user(user).gradeValue(null).build();

        SubmissionExtended submissionExtended = SubmissionExtended.of(userGradeValue, "assign-2", "org-2");

        assertEquals(0D, submissionExtended.getScore());
        assertEquals("assign-2", submissionExtended.getAssignmentId());
        assertEquals("u-2", submissionExtended.getUserId());
    }

    @Test
    public void testOfUserGradeValueWithNullPointsNumeratorDefaultsScoreToZero() {
        User user = User.builder().identifier("u-3").build();
        GradeValue gradeValue = GradeValue.builder().pointsNumerator(null).build();
        UserGradeValue userGradeValue = UserGradeValue.builder().user(user).gradeValue(gradeValue).build();

        SubmissionExtended submissionExtended = SubmissionExtended.of(userGradeValue, "assign-3", "org-3");

        assertEquals(0D, submissionExtended.getScore());
    }

}
