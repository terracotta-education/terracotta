package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.ksu.canvas.model.User;

public class SubmissionExtendedTest {

    // -- getAssignmentId / setAssignmentId --------------------------------------------------

    @Test
    public void testGetAssignmentIdReturnsNullWhenSubmissionIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().submission(null).build();

        assertNull(submissionExtended.getAssignmentId());
    }

    @Test
    public void testGetAssignmentIdReturnsNullWhenSubmissionAssignmentIdIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        assertNull(submissionExtended.getAssignmentId());
    }

    @Test
    public void testGetAssignmentIdReturnsStringWhenSet() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.getSubmission().setAssignmentId(123L);

        assertEquals("123", submissionExtended.getAssignmentId());
    }

    @Test
    public void testSetAssignmentIdDoesNothingWhenSubmissionIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().submission(null).build();

        // Should not throw despite the underlying submission being null.
        submissionExtended.setAssignmentId("123");

        assertNull(submissionExtended.getAssignmentId());
    }

    @Test
    public void testSetAssignmentIdParsesValidNumericString() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        submissionExtended.setAssignmentId("456");

        assertEquals(Long.valueOf(456L), submissionExtended.getSubmission().getAssignmentId());
        assertEquals("456", submissionExtended.getAssignmentId());
    }

    @Test
    public void testSetAssignmentIdDoesNothingWhenNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.getSubmission().setAssignmentId(9L);

        submissionExtended.setAssignmentId(null);

        assertEquals("9", submissionExtended.getAssignmentId());
    }

    @Test
    public void testSetAssignmentIdDoesNothingWhenBlank() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.getSubmission().setAssignmentId(9L);

        submissionExtended.setAssignmentId("   ");

        assertEquals("9", submissionExtended.getAssignmentId());
    }

    @Test
    public void testSetAssignmentIdThrowsNumberFormatExceptionWhenNonNumeric() {
        // Documenting current (unguarded) behavior: a non-numeric assignment id
        // string results in an uncaught NumberFormatException from Long.parseLong.
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        assertThrows(NumberFormatException.class, () -> submissionExtended.setAssignmentId("not-a-number"));
    }

    // -- getAttempt / setAttempt --------------------------------------------------------------

    @Test
    public void testGetAttemptReturnsNullWhenSubmissionIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().submission(null).build();

        assertNull(submissionExtended.getAttempt());
    }

    @Test
    public void testGetAttemptReturnsZeroWhenSubmissionAttemptIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        assertNull(submissionExtended.getSubmission().getAttempt());
        assertEquals(0L, submissionExtended.getAttempt());
    }

    @Test
    public void testGetAttemptReturnsSetValue() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setAttempt(5L);

        assertEquals(5L, submissionExtended.getAttempt());
    }

    @Test
    public void testSetAttemptDoesNothingWhenSubmissionIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().submission(null).build();

        submissionExtended.setAttempt(5L);

        assertNull(submissionExtended.getAttempt());
    }

    @Test
    public void testSetAttemptNullResetsToZeroDefault() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setAttempt(5L);
        submissionExtended.setAttempt(null);

        assertEquals(0L, submissionExtended.getAttempt());
    }

    // -- isGradeMatchesCurrentSubmission --------------------------------------------------------

    @Test
    public void testIsGradeMatchesCurrentSubmissionReturnsFalseWhenNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        assertNull(submissionExtended.getSubmission().getGradeMatchesCurrentSubmission());
        assertFalse(submissionExtended.isGradeMatchesCurrentSubmission());
    }

    @Test
    public void testIsGradeMatchesCurrentSubmissionReturnsTrueWhenTrue() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setGradeMatchesCurrentSubmission(true);

        assertTrue(submissionExtended.isGradeMatchesCurrentSubmission());
    }

    @Test
    public void testIsGradeMatchesCurrentSubmissionReturnsFalseWhenFalse() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setGradeMatchesCurrentSubmission(false);

        assertFalse(submissionExtended.isGradeMatchesCurrentSubmission());
    }

    @Test
    public void testIsGradeMatchesCurrentSubmissionReturnsFalseWhenSubmissionIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().submission(null).build();

        assertFalse(submissionExtended.isGradeMatchesCurrentSubmission());
    }

    // -- getUserId / setUserId -------------------------------------------------------------------

    @Test
    public void testGetUserIdReturnsNullWhenSubmissionIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().submission(null).build();

        assertNull(submissionExtended.getUserId());
    }

    @Test
    public void testGetUserIdReturnsNullWhenSubmissionUserIdIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        assertNull(submissionExtended.getUserId());
    }

    @Test
    public void testSetUserIdDoesNothingWhenSubmissionIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().submission(null).build();

        submissionExtended.setUserId("123");

        assertNull(submissionExtended.getUserId());
    }

    @Test
    public void testSetUserIdDoesNothingWhenBlank() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        submissionExtended.setUserId("   ");
        assertNull(submissionExtended.getUserId());

        submissionExtended.setUserId(null);
        assertNull(submissionExtended.getUserId());
    }

    @Test
    public void testSetUserIdSetsWhenNoNestedUserPresent() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        assertNull(submissionExtended.getSubmission().getUser());

        submissionExtended.setUserId("55");

        assertEquals("55", submissionExtended.getUserId());
        // No nested user object exists, so there is nothing to cascade into.
        assertNull(submissionExtended.getSubmission().getUser());
    }

    @Test
    public void testSetUserIdCascadesIntoNestedUserWhenPresent() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        User user = new User();
        submissionExtended.getSubmission().setUser(user);

        submissionExtended.setUserId("77");

        assertEquals("77", submissionExtended.getUserId());
        assertEquals(77L, submissionExtended.getSubmission().getUser().getId());
    }

    // -- getUserLoginId / setUserLoginId ----------------------------------------------------------

    @Test
    public void testGetUserLoginIdReturnsNullWhenNestedUserIsAbsent() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        assertNull(submissionExtended.getUserLoginId());
    }

    @Test
    public void testGetUserLoginIdReturnsValueWhenNestedUserIsPresent() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        User user = new User();
        user.setLoginId("jdoe");
        submissionExtended.getSubmission().setUser(user);

        assertEquals("jdoe", submissionExtended.getUserLoginId());
    }

    @Test
    public void testGetUserLoginIdReturnsNullWhenSubmissionIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().submission(null).build();

        assertNull(submissionExtended.getUserLoginId());
    }

    // -- getUserName / setUserName -----------------------------------------------------------------

    @Test
    public void testGetUserNameReturnsNullWhenNestedUserIsAbsent() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        assertNull(submissionExtended.getUserName());
    }

    @Test
    public void testGetUserNameReturnsValueWhenNestedUserIsPresent() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        User user = new User();
        user.setName("Jane Doe");
        submissionExtended.getSubmission().setUser(user);

        assertEquals("Jane Doe", submissionExtended.getUserName());
    }

    // -- from() ----------------------------------------------------------------------------------

    @Test
    public void testFromAggregatesAllFields() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.getSubmission().setAssignmentId(10L);
        submissionExtended.setAttempt(3L);
        submissionExtended.setGradeMatchesCurrentSubmission(true);
        submissionExtended.setScore(9.5);
        submissionExtended.setState("submitted");
        User user = new User();
        user.setLoginId("jdoe");
        user.setName("Jane Doe");
        submissionExtended.getSubmission().setUser(user);
        submissionExtended.setUserId("42");

        LmsSubmission lmsSubmission = submissionExtended.from();

        assertNotNull(lmsSubmission);
        assertEquals("10", lmsSubmission.getAssignmentId());
        assertEquals(3L, lmsSubmission.getAttempt());
        assertTrue(lmsSubmission.isGradeMatchesCurrentSubmission());
        assertEquals(9.5, lmsSubmission.getScore(), 0.0);
        assertEquals("submitted", lmsSubmission.getState());
        assertEquals(SubmissionExtended.class, lmsSubmission.getType());
        assertEquals("42", lmsSubmission.getUserId());
        assertEquals("jdoe", lmsSubmission.getUserLoginId());
        assertEquals("Jane Doe", lmsSubmission.getUserName());
        assertEquals(user, lmsSubmission.getUser());
    }

    // -- of() --------------------------------------------------------------------------------------

    @Test
    public void testOfReturnsDefaultInstanceWhenLmsSubmissionIsNull() {
        SubmissionExtended submissionExtended = SubmissionExtended.of(null);

        assertNotNull(submissionExtended);
        assertNull(submissionExtended.getAssignmentId());
    }

    @Test
    public void testOfAggregatesAllFieldsWhenLmsSubmissionIsNonNull() {
        LmsSubmission lmsSubmission = LmsSubmission.builder()
            .assignmentId("15")
            .attempt(2L)
            .gradeMatchesCurrentSubmission(true)
            .score(8.0)
            .state("graded")
            .userId("99")
            .build();

        SubmissionExtended submissionExtended = SubmissionExtended.of(lmsSubmission);

        assertNotNull(submissionExtended);
        assertEquals("15", submissionExtended.getAssignmentId());
        assertEquals(2L, submissionExtended.getAttempt());
        assertTrue(submissionExtended.isGradeMatchesCurrentSubmission());
        assertEquals(8.0, submissionExtended.getScore(), 0.0);
        assertEquals("graded", submissionExtended.getState());
        assertEquals("99", submissionExtended.getUserId());
    }

    @Test
    public void testOfReturnsNullAssignmentIdWhenLmsSubmissionAssignmentIdIsNull() {
        LmsSubmission lmsSubmission = LmsSubmission.builder().build();

        SubmissionExtended submissionExtended = SubmissionExtended.of(lmsSubmission);

        assertNotNull(submissionExtended);
        assertNull(submissionExtended.getAssignmentId());
    }

}
