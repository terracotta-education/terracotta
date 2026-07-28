package edu.iu.terracotta.service.app.messaging.impl.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.enums.WorkflowState;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRule;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRuleSet;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRule;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleComparisonDto;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleComparison;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;

public class MessageRuleComparisonServiceImplTest extends BaseTest {

    @Mock private Message message;

    // constructed manually rather than via @InjectMocks: BaseServiceTest also mocks BrightspaceLmsUtilsImpl
    // (as brightspaceLmsUtils), which implements LmsUtils, so @InjectMocks has two candidates for the
    // LmsUtils constructor parameter and can non-deterministically wire the wrong one
    private MessageRuleComparisonServiceImpl messageRuleComparisonService;

    private MessageContent content;
    private List<MessageRecipientRuleSet> ruleSets;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        messageRuleComparisonService = new MessageRuleComparisonServiceImpl(submissionService, lmsUtils);

        content = MessageContent.builder().build();
        ruleSets = new ArrayList<>();

        when(message.getContent()).thenReturn(content);
        when(message.getRuleSets()).thenReturn(ruleSets);
        when(message.getOwner()).thenReturn(ltiUserEntity);
        when(message.getPlatformDeployment()).thenReturn(platformDeployment);
        when(message.getExperiment()).thenReturn(experiment);
        when(lmsUtils.parseCourseId(any(), any())).thenReturn("course-1");
    }

    private LmsSubmission submission(Long attempt, Double score, boolean gradeMatches, String state) {
        return LmsSubmission.builder()
            .attempt(attempt)
            .score(score)
            .gradeMatchesCurrentSubmission(gradeMatches)
            .state(state)
            .build();
    }

    private MessageRecipientRule recipientRule(String lmsAssignmentId, MessageRuleComparison comparison, MessageRuleOperator operator, String value) {
        return MessageRecipientRule.builder()
            .lmsAssignmentId(lmsAssignmentId)
            .comparison(comparison)
            .operator(operator)
            .value(value)
            .build();
    }

    private MessageRecipientRuleSet ruleSet(MessageRuleOperator operator, MessageRecipientRule... rules) {
        return MessageRecipientRuleSet.builder()
            .operator(operator)
            .rules(List.of(rules))
            .build();
    }

    // ---------- toDto / fromDto ----------

    @Test
    public void testToDtoNull() {
        assertNull(messageRuleComparisonService.toDto(null));
    }

    @Test
    public void testToDto() {
        MessageRuleComparisonDto dto = messageRuleComparisonService.toDto(MessageRuleComparison.GREATER_THAN);

        assertEquals(MessageRuleComparison.GREATER_THAN, dto.getId());
        assertEquals(MessageRuleComparison.GREATER_THAN.getLabel(), dto.getLabel());
        assertTrue(dto.isRequiresValue());
    }

    @Test
    public void testFromDtoNull() {
        assertNull(messageRuleComparisonService.fromDto(null));
    }

    @Test
    public void testFromDto() {
        MessageRuleComparisonDto dto = MessageRuleComparisonDto.builder().id(MessageRuleComparison.IS_SUBMITTED).build();

        assertEquals(MessageRuleComparison.IS_SUBMITTED, messageRuleComparisonService.fromDto(dto));
    }

    // ---------- getLmsSubmissions ----------

    @Test
    public void testGetLmsSubmissionsNoAssignmentIdsSkipsFetch() throws Exception {
        Map<String, List<LmsSubmission>> result = messageRuleComparisonService.getLmsSubmissions(message);

        assertTrue(result.isEmpty());
        verify(submissionService, never()).getAllSubmissionsForMultipleAssignments(any(), anyString(), anyList());
    }

    @Test
    public void testGetLmsSubmissionsFromRecipientRules() throws Exception {
        ruleSets.add(ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "1")));
        when(submissionService.getAllSubmissionsForMultipleAssignments(eq(ltiUserEntity), eq("course-1"), eq(List.of("101")))).thenReturn(Map.of("101", List.of()));

        Map<String, List<LmsSubmission>> result = messageRuleComparisonService.getLmsSubmissions(message);

        assertEquals(1, result.size());
        verify(submissionService, times(1)).getAllSubmissionsForMultipleAssignments(eq(ltiUserEntity), eq("course-1"), eq(List.of("101")));
    }

    @Test
    public void testGetLmsSubmissionsFromConditionalTextRules() throws Exception {
        MessageConditionalTextRuleSet conditionalRuleSet = MessageConditionalTextRuleSet.builder()
            .rules(
                List.of(
                    MessageConditionalTextRule.builder().lmsAssignmentId("202").comparison(MessageRuleComparison.IS_SUBMITTED).operator(MessageRuleOperator.NONE).build()
                )
            )
            .build();
        content.getConditionalTexts().add(
            MessageConditionalText.builder().content(content).ruleSets(List.of(conditionalRuleSet)).build()
        );
        when(submissionService.getAllSubmissionsForMultipleAssignments(eq(ltiUserEntity), eq("course-1"), eq(List.of("202")))).thenReturn(Map.of("202", List.of()));

        Map<String, List<LmsSubmission>> result = messageRuleComparisonService.getLmsSubmissions(message);

        assertEquals(1, result.size());
    }

    @Test
    public void testGetLmsSubmissionsDedupesAssignmentIds() throws Exception {
        ruleSets.add(ruleSet(
            MessageRuleOperator.AND,
            recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "1"),
            recipientRule("101", MessageRuleComparison.GREATER_THAN, MessageRuleOperator.AND, "2")
        ));
        when(submissionService.getAllSubmissionsForMultipleAssignments(eq(ltiUserEntity), eq("course-1"), eq(List.of("101")))).thenReturn(Map.of("101", List.of()));

        messageRuleComparisonService.getLmsSubmissions(message);

        verify(submissionService, times(1)).getAllSubmissionsForMultipleAssignments(eq(ltiUserEntity), eq("course-1"), eq(List.of("101")));
    }

    @Test
    public void testGetLmsSubmissionsWrapsApiException() throws Exception {
        ruleSets.add(ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "1")));
        when(submissionService.getAllSubmissionsForMultipleAssignments(any(), anyString(), anyList())).thenThrow(new ApiException("lms error"));

        ApiException exception = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class, () -> { messageRuleComparisonService.getLmsSubmissions(message); });

        assertEquals("lms error", exception.getMessage());
    }

    // ---------- conditionalTextRuleSetsMatch ----------

    @Test
    public void testConditionalTextRuleSetsMatchAllSubmissionsEmptyReturnsFalse() {
        MessageConditionalText conditionalText = MessageConditionalText.builder().build();

        boolean result = messageRuleComparisonService.conditionalTextRuleSetsMatch(conditionalText, Map.of("101", List.of()));

        assertFalse(result);
    }

    @Test
    public void testConditionalTextRuleSetsMatchTrue() {
        MessageConditionalTextRuleSet ruleSet = MessageConditionalTextRuleSet.builder()
            .operator(MessageRuleOperator.NONE)
            .rules(
                List.of(
                    MessageConditionalTextRule.builder()
                        .lmsAssignmentId("101")
                        .comparison(MessageRuleComparison.IS_SUBMITTED)
                        .operator(MessageRuleOperator.NONE)
                        .build()
                )
            )
            .build();
        MessageConditionalText conditionalText = MessageConditionalText.builder().ruleSets(List.of(ruleSet)).build();
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, 90D, true, WorkflowState.SUBMITTED.state())));

        boolean result = messageRuleComparisonService.conditionalTextRuleSetsMatch(conditionalText, submissions);

        assertTrue(result);
    }

    // ---------- recipientRuleSetsMatch: guard clauses ----------

    @Test
    public void testRecipientRuleSetsMatchEmptyRuleSetsReturnsTrue() {
        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(), Map.of()));
        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(null, Map.of()));
    }

    @Test
    public void testRecipientRuleSetsMatchAllLmsSubmissionsEmptyReturnsFalse() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "1"));

        boolean result = messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), Map.of("101", List.of()));

        assertFalse(result);
    }

    // ---------- recipientRuleSetsMatch: value-based comparisons ----------

    @Test
    public void testRecipientRuleSetsMatchEqualsTrue() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "90"));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, 90D, true, WorkflowState.GRADED.state())));

        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchEqualsFalse() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "90"));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, 50D, true, WorkflowState.GRADED.state())));

        assertFalse(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchGreaterThanTrueAndFalse() {
        MessageRecipientRuleSet rsTrue = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.GREATER_THAN, MessageRuleOperator.NONE, "50"));
        Map<String, List<LmsSubmission>> submissionsTrue = Map.of("101", List.of(submission(1L, 90D, true, WorkflowState.GRADED.state())));
        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rsTrue), submissionsTrue));

        MessageRecipientRuleSet rsFalse = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.GREATER_THAN, MessageRuleOperator.NONE, "50"));
        Map<String, List<LmsSubmission>> submissionsFalse = Map.of("101", List.of(submission(1L, 10D, true, WorkflowState.GRADED.state())));
        assertFalse(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rsFalse), submissionsFalse));
    }

    @Test
    public void testRecipientRuleSetsMatchGreaterThanEqualBoundary() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.GREATER_THAN_EQUAL, MessageRuleOperator.NONE, "50"));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, 50D, true, WorkflowState.GRADED.state())));

        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchLessThanTrueAndFalse() {
        MessageRecipientRuleSet rsTrue = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.LESS_THAN, MessageRuleOperator.NONE, "50"));
        Map<String, List<LmsSubmission>> submissionsTrue = Map.of("101", List.of(submission(1L, 10D, true, WorkflowState.GRADED.state())));
        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rsTrue), submissionsTrue));

        MessageRecipientRuleSet rsFalse = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.LESS_THAN, MessageRuleOperator.NONE, "50"));
        Map<String, List<LmsSubmission>> submissionsFalse = Map.of("101", List.of(submission(1L, 90D, true, WorkflowState.GRADED.state())));
        assertFalse(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rsFalse), submissionsFalse));
    }

    @Test
    public void testRecipientRuleSetsMatchLessThanEqualBoundary() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.LESS_THAN_EQUAL, MessageRuleOperator.NONE, "50"));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, 50D, true, WorkflowState.GRADED.state())));

        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchRequiresValueNoSubmissionsForRuleReturnsFalse() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "50"));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(), "other", List.of(submission(1L, 1D, true, WorkflowState.GRADED.state())));

        assertFalse(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchRequiresValueNoSubmissionMatchesCurrentReturnsFalse() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "50"));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, 50D, false, WorkflowState.GRADED.state())));

        assertFalse(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchRequiresValueNullScoreReturnsFalse() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "50"));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, null, true, WorkflowState.GRADED.state())));

        assertFalse(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    // ---------- recipientRuleSetsMatch: state-based comparisons ----------

    @Test
    public void testRecipientRuleSetsMatchIsNotYetSubmittedTrueWhenNoSubmissions() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.IS_NOT_YET_SUBMITTED, MessageRuleOperator.NONE, null));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(), "other", List.of(submission(1L, 1D, true, WorkflowState.GRADED.state())));

        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchIsNotYetSubmittedTrueWhenUnsubmittedState() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.IS_NOT_YET_SUBMITTED, MessageRuleOperator.NONE, null));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, null, false, WorkflowState.UNSUBMITTED.state())));

        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchIsNotYetSubmittedFalseWhenSubmitted() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.IS_NOT_YET_SUBMITTED, MessageRuleOperator.NONE, null));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, 90D, true, WorkflowState.SUBMITTED.state())));

        assertFalse(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchIsSubmittedTrue() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.IS_SUBMITTED, MessageRuleOperator.NONE, null));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, 90D, true, WorkflowState.GRADED.state())));

        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchIsSubmittedFalseWhenNoSubmissions() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.IS_SUBMITTED, MessageRuleOperator.NONE, null));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(), "other", List.of(submission(1L, 1D, true, WorkflowState.GRADED.state())));

        assertFalse(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchIsSubmittedButNotYetGradedTrue() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.IS_SUBMITTED_BUT_NOT_YET_GRADED, MessageRuleOperator.NONE, null));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(2L, null, true, WorkflowState.PENDING_REVIEW.state()), submission(1L, null, true, WorkflowState.PENDING_REVIEW.state())));

        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchIsSubmittedButNotYetGradedFalseWhenGraded() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.IS_SUBMITTED_BUT_NOT_YET_GRADED, MessageRuleOperator.NONE, null));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, 90D, true, WorkflowState.GRADED.state())));

        assertFalse(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    // ---------- operator combination logic ----------

    @Test
    public void testRecipientRuleSetsMatchOperatorAndAcrossRulesBothMustMatch() {
        MessageRecipientRuleSet rs = ruleSet(
            MessageRuleOperator.NONE,
            recipientRule("101", MessageRuleComparison.IS_SUBMITTED, MessageRuleOperator.NONE, null),
            recipientRule("102", MessageRuleComparison.EQUALS, MessageRuleOperator.AND, "90")
        );
        Map<String, List<LmsSubmission>> submissions = Map.of(
            "101", List.of(submission(1L, 90D, true, WorkflowState.GRADED.state())),
            "102", List.of(submission(1L, 10D, true, WorkflowState.GRADED.state()))
        );

        assertFalse(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchOperatorOrAcrossRulesEitherMatches() {
        MessageRecipientRuleSet rs = ruleSet(
            MessageRuleOperator.NONE,
            recipientRule("101", MessageRuleComparison.IS_SUBMITTED, MessageRuleOperator.NONE, null),
            recipientRule("102", MessageRuleComparison.EQUALS, MessageRuleOperator.OR, "90")
        );
        Map<String, List<LmsSubmission>> submissions = Map.of(
            "101", List.of(submission(1L, 90D, true, WorkflowState.GRADED.state())),
            "102", List.of(submission(1L, 10D, true, WorkflowState.GRADED.state()))
        );

        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchOperatorNotBehavesLikeNone() {
        // MessageRuleOperator.NOT is not explicitly handled in the switch; it falls into the default
        // branch alongside NONE, so it does not negate - it simply returns the rule's own match value
        MessageRecipientRuleSet rs = ruleSet(
            MessageRuleOperator.NONE,
            recipientRule("101", MessageRuleComparison.IS_SUBMITTED, MessageRuleOperator.NONE, null),
            recipientRule("102", MessageRuleComparison.IS_NOT_YET_SUBMITTED, MessageRuleOperator.NOT, null)
        );
        Map<String, List<LmsSubmission>> submissions = Map.of(
            "101", List.of(submission(1L, 90D, true, WorkflowState.GRADED.state())),
            "102", List.of(submission(1L, null, false, WorkflowState.UNSUBMITTED.state()))
        );

        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchMultipleRuleSetsOperatorOr() {
        MessageRecipientRuleSet rsFalse = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "999"));
        MessageRecipientRuleSet rsTrue = ruleSet(MessageRuleOperator.OR, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "90"));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, 90D, true, WorkflowState.GRADED.state())));

        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rsFalse, rsTrue), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchMultipleRuleSetsOperatorAndRequiresAll() {
        MessageRecipientRuleSet rsFalse = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "999"));
        MessageRecipientRuleSet rsTrue = ruleSet(MessageRuleOperator.AND, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "90"));
        Map<String, List<LmsSubmission>> submissions = Map.of("101", List.of(submission(1L, 90D, true, WorkflowState.GRADED.state())));

        assertFalse(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rsFalse, rsTrue), submissions));
    }

    @Test
    public void testRecipientRuleSetsMatchOrdersSubmissionsByAttemptDescending() {
        MessageRecipientRuleSet rs = ruleSet(MessageRuleOperator.NONE, recipientRule("101", MessageRuleComparison.EQUALS, MessageRuleOperator.NONE, "90"));
        // only the highest-attempt submission has gradeMatchesCurrentSubmission=true
        Map<String, List<LmsSubmission>> submissions = Map.of(
            "101", List.of(
                submission(1L, 10D, false, WorkflowState.GRADED.state()),
                submission(3L, 90D, true, WorkflowState.GRADED.state()),
                submission(2L, 50D, false, WorkflowState.GRADED.state())
            )
        );

        assertTrue(messageRuleComparisonService.recipientRuleSetsMatch(List.of(rs), submissions));
    }

}
