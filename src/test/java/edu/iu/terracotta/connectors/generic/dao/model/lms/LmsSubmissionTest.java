package edu.iu.terracotta.connectors.generic.dao.model.lms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LmsSubmissionTest {

    @Test
    public void testBuilderGettersAndSetters() {
        Object user = new Object();

        LmsSubmission lmsSubmission = LmsSubmission.builder()
            .type(LmsSubmission.class)
            .score(87.5)
            .user(user)
            .userId("user-1")
            .userLoginId("login-1")
            .userName("User One")
            .attempt(2L)
            .assignmentId("assignment-1")
            .gradeMatchesCurrentSubmission(true)
            .state("submitted")
            .build();

        assertEquals(LmsSubmission.class, lmsSubmission.getType());
        assertEquals(87.5, lmsSubmission.getScore());
        assertSame(user, lmsSubmission.getUser());
        assertEquals("user-1", lmsSubmission.getUserId());
        assertEquals("login-1", lmsSubmission.getUserLoginId());
        assertEquals("User One", lmsSubmission.getUserName());
        assertEquals(2L, lmsSubmission.getAttempt());
        assertEquals("assignment-1", lmsSubmission.getAssignmentId());
        assertTrue(lmsSubmission.isGradeMatchesCurrentSubmission());
        assertEquals("submitted", lmsSubmission.getState());

        lmsSubmission.setGradeMatchesCurrentSubmission(false);
        lmsSubmission.setState("graded");

        assertFalse(lmsSubmission.isGradeMatchesCurrentSubmission());
        assertEquals("graded", lmsSubmission.getState());
    }

    @Test
    public void testFromReturnsSameInstance() {
        LmsSubmission lmsSubmission = LmsSubmission.builder().assignmentId("assignment-2").build();

        assertSame(lmsSubmission, lmsSubmission.from());
    }

}
