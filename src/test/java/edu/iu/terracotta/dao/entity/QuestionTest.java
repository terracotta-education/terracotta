package edu.iu.terracotta.dao.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.dao.model.enums.QuestionTypes;

/**
 * {@link Question} is a Lombok {@code @Builder} JPA entity. These tests exercise the four
 * hand-written boolean helper methods that compare {@code questionType} against a single
 * {@link QuestionTypes} constant.
 */
public class QuestionTest {

    @Test
    public void testIsMC() {
        Question question = Question.builder()
            .questionType(QuestionTypes.MC)
            .build();

        assertTrue(question.isMC());
        assertFalse(question.isEssay());
        assertFalse(question.isFileSubmission());
        assertFalse(question.isIntegration());
    }

    @Test
    public void testIsEssay() {
        Question question = Question.builder()
            .questionType(QuestionTypes.ESSAY)
            .build();

        assertFalse(question.isMC());
        assertTrue(question.isEssay());
        assertFalse(question.isFileSubmission());
        assertFalse(question.isIntegration());
    }

    @Test
    public void testIsFileSubmission() {
        Question question = Question.builder()
            .questionType(QuestionTypes.FILE)
            .build();

        assertFalse(question.isMC());
        assertFalse(question.isEssay());
        assertTrue(question.isFileSubmission());
        assertFalse(question.isIntegration());
    }

    @Test
    public void testIsIntegration() {
        Question question = Question.builder()
            .questionType(QuestionTypes.INTEGRATION)
            .build();

        assertFalse(question.isMC());
        assertFalse(question.isEssay());
        assertFalse(question.isFileSubmission());
        assertTrue(question.isIntegration());
    }

    @Test
    public void testPageBreakIsNoneOfTheOthers() {
        Question question = Question.builder()
            .questionType(QuestionTypes.PAGE_BREAK)
            .build();

        assertFalse(question.isMC());
        assertFalse(question.isEssay());
        assertFalse(question.isFileSubmission());
        assertFalse(question.isIntegration());
    }

    @Test
    public void testNullQuestionType() {
        Question question = Question.builder()
            .build();

        assertFalse(question.isMC());
        assertFalse(question.isEssay());
        assertFalse(question.isFileSubmission());
        assertFalse(question.isIntegration());
    }

}
