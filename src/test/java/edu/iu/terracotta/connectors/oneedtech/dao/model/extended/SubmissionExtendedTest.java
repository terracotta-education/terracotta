package edu.iu.terracotta.connectors.oneedtech.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.oneedtech.dao.model.lms.Assignment;
import edu.iu.terracotta.connectors.oneedtech.dao.model.lms.Submission;

public class SubmissionExtendedTest {

    @Test
    public void testDefaultSubmissionIsUsedWhenNoneProvided() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        assertNotNull(submissionExtended.getSubmission());
    }

    @Test
    public void testBuilderAcceptsExplicitSubmission() {
        Submission submission = Submission.builder().build();

        SubmissionExtended submissionExtended = SubmissionExtended.builder()
            .submission(submission)
            .build();

        assertSame(submission, submissionExtended.getSubmission());
    }

    @Test
    public void testFromSetsTypeToAssignmentAndReturnsSameInstance() {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        LmsSubmission converted = submissionExtended.from();

        assertSame(submissionExtended, converted);
        assertEquals(Assignment.class, converted.getType());
    }

}
