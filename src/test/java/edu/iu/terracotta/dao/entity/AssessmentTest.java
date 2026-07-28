package edu.iu.terracotta.dao.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationClient;
import edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;

/**
 * {@link Assessment} is a Lombok {@code @Builder} JPA entity. These tests exercise the
 * hand-written {@code getIntegration()}, {@code isIntegration()}, {@code canViewResponses()} and
 * {@code canViewCorrectAnswers()} methods using real, builder-constructed {@link Question} and
 * {@link Integration} collaborators (no mocking needed).
 */
public class AssessmentTest {

    private static final Timestamp FUTURE = Timestamp.from(Instant.now().plusSeconds(3600));
    private static final Timestamp PAST = Timestamp.from(Instant.now().minusSeconds(3600));

    private Integration integration;

    @BeforeEach
    public void setup() {
        IntegrationClient client = IntegrationClient.builder()
            .name("Qualtrics")
            .build();
        IntegrationConfiguration configuration = IntegrationConfiguration.builder()
            .client(client)
            .build();
        integration = Integration.builder()
            .configuration(configuration)
            .build();
    }

    private Question integrationQuestion() {
        return Question.builder()
            .questionType(QuestionTypes.INTEGRATION)
            .integration(integration)
            .build();
    }

    private Question mcQuestion() {
        return Question.builder()
            .questionType(QuestionTypes.MC)
            .build();
    }

    // getIntegration / isIntegration

    @Test
    public void testGetIntegrationReturnsIntegrationFromFirstQuestion() {
        Question question = integrationQuestion();
        Assessment assessment = Assessment.builder()
            .questions(List.of(question))
            .build();

        assertSame(integration, assessment.getIntegration());
    }

    @Test
    public void testIsIntegrationTrueWhenAnyQuestionIsIntegrationType() {
        Assessment assessment = Assessment.builder()
            .questions(List.of(mcQuestion(), integrationQuestion()))
            .build();

        assertTrue(assessment.isIntegration());
    }

    @Test
    public void testIsIntegrationFalseWhenNoQuestionIsIntegrationType() {
        Assessment assessment = Assessment.builder()
            .questions(List.of(mcQuestion()))
            .build();

        assertFalse(assessment.isIntegration());
    }

    @Test
    public void testIsIntegrationFalseWhenQuestionsEmpty() {
        Assessment assessment = Assessment.builder()
            .questions(Collections.emptyList())
            .build();

        assertFalse(assessment.isIntegration());
    }

    // canViewResponses

    @Test
    public void testCanViewResponsesFalseWhenNotAllowed() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewResponses(false)
            .questions(Collections.emptyList())
            .build();

        assertFalse(assessment.canViewResponses());
    }

    @Test
    public void testCanViewResponsesTrueWhenAllowedNoBoundsNotIntegration() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewResponses(true)
            .questions(Collections.emptyList())
            .build();

        assertTrue(assessment.canViewResponses());
    }

    @Test
    public void testCanViewResponsesFalseWhenAfterInFuture() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewResponses(true)
            .studentViewResponsesAfter(FUTURE)
            .questions(Collections.emptyList())
            .build();

        assertFalse(assessment.canViewResponses());
    }

    @Test
    public void testCanViewResponsesTrueWhenAfterInPast() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewResponses(true)
            .studentViewResponsesAfter(PAST)
            .questions(Collections.emptyList())
            .build();

        assertTrue(assessment.canViewResponses());
    }

    @Test
    public void testCanViewResponsesFalseWhenBeforeInPast() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewResponses(true)
            .studentViewResponsesBefore(PAST)
            .questions(Collections.emptyList())
            .build();

        assertFalse(assessment.canViewResponses());
    }

    @Test
    public void testCanViewResponsesTrueWhenBeforeInFuture() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewResponses(true)
            .studentViewResponsesBefore(FUTURE)
            .questions(Collections.emptyList())
            .build();

        assertTrue(assessment.canViewResponses());
    }

    @Test
    public void testCanViewResponsesFalseWhenIntegrationIsQualtrics() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewResponses(true)
            .questions(List.of(integrationQuestion()))
            .build();

        assertFalse(assessment.canViewResponses());
    }

    @Test
    public void testCanViewResponsesFallsThroughToTimestampChecksWhenIntegrationIsNotQualtrics() {
        IntegrationClient otherClient = IntegrationClient.builder()
            .name("OtherVendor")
            .build();
        IntegrationConfiguration otherConfiguration = IntegrationConfiguration.builder()
            .client(otherClient)
            .build();
        Integration otherIntegration = Integration.builder()
            .configuration(otherConfiguration)
            .build();
        Question question = Question.builder()
            .questionType(QuestionTypes.INTEGRATION)
            .integration(otherIntegration)
            .build();

        Assessment assessment = Assessment.builder()
            .allowStudentViewResponses(true)
            .questions(List.of(question))
            .build();

        assertTrue(assessment.canViewResponses());
    }

    // canViewCorrectAnswers

    @Test
    public void testCanViewCorrectAnswersFalseWhenNotAllowed() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewCorrectAnswers(false)
            .build();

        assertFalse(assessment.canViewCorrectAnswers());
    }

    @Test
    public void testCanViewCorrectAnswersTrueWhenAllowedNoBounds() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewCorrectAnswers(true)
            .build();

        assertTrue(assessment.canViewCorrectAnswers());
    }

    @Test
    public void testCanViewCorrectAnswersFalseWhenAfterInFuture() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewCorrectAnswers(true)
            .studentViewCorrectAnswersAfter(FUTURE)
            .build();

        assertFalse(assessment.canViewCorrectAnswers());
    }

    @Test
    public void testCanViewCorrectAnswersTrueWhenAfterInPast() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewCorrectAnswers(true)
            .studentViewCorrectAnswersAfter(PAST)
            .build();

        assertTrue(assessment.canViewCorrectAnswers());
    }

    @Test
    public void testCanViewCorrectAnswersFalseWhenBeforeInPast() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewCorrectAnswers(true)
            .studentViewCorrectAnswersBefore(PAST)
            .build();

        assertFalse(assessment.canViewCorrectAnswers());
    }

    @Test
    public void testCanViewCorrectAnswersTrueWhenBeforeInFuture() {
        Assessment assessment = Assessment.builder()
            .allowStudentViewCorrectAnswers(true)
            .studentViewCorrectAnswersBefore(FUTURE)
            .build();

        assertTrue(assessment.canViewCorrectAnswers());
    }

}
