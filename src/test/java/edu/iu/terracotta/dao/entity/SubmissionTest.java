package edu.iu.terracotta.dao.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationClient;
import edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationToken;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;

/**
 * {@link Submission} is a Lombok {@code @Builder} JPA entity. These tests exercise the
 * hand-written transient delegation methods, wiring a real {@link Assessment} (and its real
 * {@link Question}/{@link Integration} collaborators) rather than mocking, since {@code Assessment}
 * is already exercised the same way in {@code AssessmentTest}.
 */
public class SubmissionTest {

    private Integration integration() {
        IntegrationClient client = IntegrationClient.builder()
            .name("Qualtrics")
            .build();
        IntegrationConfiguration configuration = IntegrationConfiguration.builder()
            .client(client)
            .build();

        return Integration.builder()
            .configuration(configuration)
            .build();
    }

    // getIntegration / isIntegration

    @Test
    public void testGetIntegrationDelegatesToAssessment() {
        Integration integration = integration();
        Question question = Question.builder()
            .questionType(QuestionTypes.INTEGRATION)
            .integration(integration)
            .build();
        Assessment assessment = Assessment.builder()
            .questions(List.of(question))
            .build();
        Submission submission = Submission.builder()
            .assessment(assessment)
            .build();

        assertSame(integration, submission.getIntegration());
    }

    @Test
    public void testIsIntegrationTrueDelegatesToAssessment() {
        Question question = Question.builder()
            .questionType(QuestionTypes.INTEGRATION)
            .integration(integration())
            .build();
        Assessment assessment = Assessment.builder()
            .questions(List.of(question))
            .build();
        Submission submission = Submission.builder()
            .assessment(assessment)
            .build();

        assertTrue(submission.isIntegration());
    }

    @Test
    public void testIsIntegrationFalseDelegatesToAssessment() {
        Question question = Question.builder()
            .questionType(QuestionTypes.MC)
            .build();
        Assessment assessment = Assessment.builder()
            .questions(List.of(question))
            .build();
        Submission submission = Submission.builder()
            .assessment(assessment)
            .build();

        assertFalse(submission.isIntegration());
    }

    // isIntegrationFeedbackEnabled

    @Test
    public void testIsIntegrationFeedbackEnabledTrueDelegatesToAssessmentCanViewResponses() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewResponses(true)
            .questions(Collections.emptyList())
            .build();
        Submission submission = Submission.builder()
            .assessment(assessment)
            .build();

        assertTrue(submission.isIntegrationFeedbackEnabled());
    }

    @Test
    public void testIsIntegrationFeedbackEnabledFalseDelegatesToAssessmentCanViewResponses() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewResponses(false)
            .questions(Collections.emptyList())
            .build();
        Submission submission = Submission.builder()
            .assessment(assessment)
            .build();

        assertFalse(submission.isIntegrationFeedbackEnabled());
    }

    // getIntegrationTokenLaunchedAt

    @Test
    public void testGetIntegrationTokenLaunchedAtNullWhenNoToken() {
        Submission submission = Submission.builder()
            .integrationToken(null)
            .build();

        assertNull(submission.getIntegrationTokenLaunchedAt());
    }

    @Test
    public void testGetIntegrationTokenLaunchedAtReturnsTokenValue() {
        Timestamp lastLaunchedAt = Timestamp.from(Instant.now().minusSeconds(60));
        IntegrationToken integrationToken = IntegrationToken.builder()
            .lastLaunchedAt(lastLaunchedAt)
            .build();
        Submission submission = Submission.builder()
            .integrationToken(integrationToken)
            .build();

        assertEquals(lastLaunchedAt, submission.getIntegrationTokenLaunchedAt());
    }

    // isSubmitted

    @Test
    public void testIsSubmittedFalseWhenDateSubmittedNull() {
        Submission submission = Submission.builder()
            .dateSubmitted(null)
            .build();

        assertFalse(submission.isSubmitted());
    }

    @Test
    public void testIsSubmittedTrueWhenDateSubmittedSet() {
        Submission submission = Submission.builder()
            .dateSubmitted(Timestamp.from(Instant.now()))
            .build();

        assertTrue(submission.isSubmitted());
    }

}
