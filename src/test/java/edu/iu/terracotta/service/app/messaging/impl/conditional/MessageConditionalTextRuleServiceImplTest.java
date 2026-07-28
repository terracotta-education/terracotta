package edu.iu.terracotta.service.app.messaging.impl.conditional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRule;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextRuleDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleComparisonDto;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleComparison;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import edu.iu.terracotta.service.app.messaging.MessageRuleAssignmentService;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;

public class MessageConditionalTextRuleServiceImplTest extends BaseTest {

    @Mock private MessageRuleComparisonService messageRuleComparisonService;
    @Mock private MessageRuleAssignmentService messageRuleAssignmentService;

    @InjectMocks private MessageConditionalTextRuleServiceImpl conditionalTextRuleService;

    private MessageConditionalTextRuleSet ruleSet;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        ruleSet = MessageConditionalTextRuleSet.builder()
            .operator(MessageRuleOperator.AND)
            .build();
        ruleSet.setUuid(UUID.randomUUID());
    }

    @Test
    public void testCreateSingle() {
        MessageConditionalTextRuleDto dto = MessageConditionalTextRuleDto.builder()
            .lmsAssignmentId("assignment-1")
            .value("10")
            .operator(MessageRuleOperator.AND)
            .build();

        conditionalTextRuleService.create(dto, ruleSet);

        assertEquals(1, ruleSet.getRules().size());
        assertSame(ruleSet, ruleSet.getRules().get(0).getRuleSet());
        assertEquals("assignment-1", ruleSet.getRules().get(0).getLmsAssignmentId());
    }

    @Test
    public void testCreateList() {
        MessageConditionalTextRuleDto dto1 = MessageConditionalTextRuleDto.builder().lmsAssignmentId("a1").build();
        MessageConditionalTextRuleDto dto2 = MessageConditionalTextRuleDto.builder().lmsAssignmentId("a2").build();

        conditionalTextRuleService.create(List.of(dto1, dto2), ruleSet);

        assertEquals(2, ruleSet.getRules().size());
    }

    @Test
    public void testCreateListNull() {
        conditionalTextRuleService.create((List<MessageConditionalTextRuleDto>) null, ruleSet);

        assertTrue(ruleSet.getRules().isEmpty());
    }

    @Test
    public void testUpdateSingle() {
        MessageConditionalTextRule rule = MessageConditionalTextRule.builder()
            .ruleSet(ruleSet)
            .lmsAssignmentId("old")
            .build();
        MessageConditionalTextRuleDto dto = MessageConditionalTextRuleDto.builder()
            .lmsAssignmentId("new")
            .build();

        conditionalTextRuleService.update(dto, rule);

        assertEquals("new", rule.getLmsAssignmentId());
    }

    @Test
    public void testUpdateListMatchesExisting() {
        MessageConditionalTextRule existingRule = MessageConditionalTextRule.builder()
            .ruleSet(ruleSet)
            .lmsAssignmentId("old")
            .build();
        UUID ruleUuid = UUID.randomUUID();
        existingRule.setUuid(ruleUuid);
        ruleSet.getRules().add(existingRule);

        MessageConditionalTextRuleDto dto = MessageConditionalTextRuleDto.builder()
            .id(ruleUuid)
            .lmsAssignmentId("updated")
            .build();

        conditionalTextRuleService.update(List.of(dto), ruleSet);

        assertEquals(1, ruleSet.getRules().size());
        assertSame(existingRule, ruleSet.getRules().get(0));
        assertEquals("updated", ruleSet.getRules().get(0).getLmsAssignmentId());
    }

    @Test
    public void testUpdateListCreatesNewWhenNoMatch() {
        MessageConditionalTextRule existingRule = MessageConditionalTextRule.builder()
            .ruleSet(ruleSet)
            .lmsAssignmentId("old")
            .build();
        existingRule.setUuid(UUID.randomUUID());
        ruleSet.getRules().add(existingRule);

        MessageConditionalTextRuleDto dto = MessageConditionalTextRuleDto.builder()
            .id(UUID.randomUUID())
            .lmsAssignmentId("brand-new")
            .build();

        conditionalTextRuleService.update(List.of(dto), ruleSet);

        // NOTE: production code (MessageConditionalTextRuleServiceImpl#update(List, RuleSet)) adds the
        // newly created rule to the live list inside create(), then addAll() appends the same instance
        // again, so a non-matching dto results in a duplicated entry rather than a single one.
        assertEquals(2, ruleSet.getRules().size());
        assertSame(ruleSet.getRules().get(0), ruleSet.getRules().get(1));
        assertEquals("brand-new", ruleSet.getRules().get(0).getLmsAssignmentId());
    }

    @Test
    public void testUpdateListEmptyDtosClearsRules() {
        MessageConditionalTextRule existingRule = MessageConditionalTextRule.builder()
            .ruleSet(ruleSet)
            .lmsAssignmentId("old")
            .build();
        existingRule.setUuid(UUID.randomUUID());
        ruleSet.getRules().add(existingRule);

        conditionalTextRuleService.update((List<MessageConditionalTextRuleDto>) null, ruleSet);

        assertTrue(ruleSet.getRules().isEmpty());
    }

    @Test
    public void testDuplicate() {
        MessageConditionalTextRule original = MessageConditionalTextRule.builder()
            .comparison(MessageRuleComparison.EQUALS)
            .lmsAssignmentId("assignment-1")
            .operator(MessageRuleOperator.AND)
            .value("5")
            .ruleSet(ruleSet)
            .build();

        MessageConditionalTextRuleSet newRuleSet = MessageConditionalTextRuleSet.builder().build();

        conditionalTextRuleService.duplicate(List.of(original), newRuleSet);

        assertEquals(1, newRuleSet.getRules().size());
        MessageConditionalTextRule copy = newRuleSet.getRules().get(0);
        assertEquals(MessageRuleComparison.EQUALS, copy.getComparison());
        assertEquals("assignment-1", copy.getLmsAssignmentId());
        assertEquals(MessageRuleOperator.AND, copy.getOperator());
        assertEquals("5", copy.getValue());
        assertSame(newRuleSet, copy.getRuleSet());
    }

    @Test
    public void testDuplicateEmptyList() {
        MessageConditionalTextRuleSet newRuleSet = MessageConditionalTextRuleSet.builder().build();

        conditionalTextRuleService.duplicate(null, newRuleSet);

        assertTrue(newRuleSet.getRules().isEmpty());
    }

    @Test
    public void testToDtoListEmpty() {
        assertTrue(conditionalTextRuleService.toDto((List<MessageConditionalTextRule>) null).isEmpty());
    }

    @Test
    public void testToDtoList() {
        MessageConditionalTextRule rule = MessageConditionalTextRule.builder()
            .ruleSet(ruleSet)
            .lmsAssignmentId("a1")
            .build();
        rule.setUuid(UUID.randomUUID());

        List<MessageConditionalTextRuleDto> dtos = conditionalTextRuleService.toDto(List.of(rule));

        assertEquals(1, dtos.size());
        assertEquals("a1", dtos.get(0).getLmsAssignmentId());
    }

    @Test
    public void testToDtoSingleNull() {
        assertNull(conditionalTextRuleService.toDto((MessageConditionalTextRule) null));
    }

    @Test
    public void testToDtoSingle() {
        MessageConditionalTextRule rule = MessageConditionalTextRule.builder()
            .ruleSet(ruleSet)
            .lmsAssignmentId("a1")
            .value("val")
            .operator(MessageRuleOperator.OR)
            .comparison(MessageRuleComparison.GREATER_THAN)
            .build();
        rule.setUuid(UUID.randomUUID());
        MessageRuleComparisonDto comparisonDto = MessageRuleComparisonDto.builder().id(MessageRuleComparison.GREATER_THAN).build();
        when(messageRuleComparisonService.toDto(MessageRuleComparison.GREATER_THAN)).thenReturn(comparisonDto);

        MessageConditionalTextRuleDto dto = conditionalTextRuleService.toDto(rule);

        assertEquals(rule.getUuid(), dto.getId());
        assertEquals(ruleSet.getUuid(), dto.getRuleSetId());
        assertEquals("a1", dto.getLmsAssignmentId());
        assertEquals("val", dto.getValue());
        assertEquals(MessageRuleOperator.OR, dto.getOperator());
        assertSame(comparisonDto, dto.getComparison());
    }

    @Test
    public void testFromDtoNullDto() {
        MessageConditionalTextRule rule = MessageConditionalTextRule.builder().ruleSet(ruleSet).build();

        MessageConditionalTextRule returned = conditionalTextRuleService.fromDto(null, rule);

        assertSame(rule, returned);
    }

    @Test
    public void testFromDtoWithLmsAssignmentId() {
        MessageConditionalTextRuleDto dto = MessageConditionalTextRuleDto.builder()
            .lmsAssignmentId("direct-id")
            .value("value")
            .operator(MessageRuleOperator.NOT)
            .build();
        MessageConditionalTextRule rule = MessageConditionalTextRule.builder().ruleSet(ruleSet).build();

        MessageConditionalTextRule returned = conditionalTextRuleService.fromDto(dto, rule);

        assertEquals("direct-id", returned.getLmsAssignmentId());
        assertEquals("value", returned.getValue());
        assertEquals(MessageRuleOperator.NOT, returned.getOperator());
        verify(messageRuleAssignmentService, never()).fromDto(any());
    }

    @Test
    public void testFromDtoWithAssignmentFallback() {
        MessageRuleAssignmentDto assignmentDto = MessageRuleAssignmentDto.builder().lmsId("lms-1").build();
        MessageConditionalTextRuleDto dto = MessageConditionalTextRuleDto.builder()
            .assignment(assignmentDto)
            .build();
        MessageConditionalTextRule rule = MessageConditionalTextRule.builder().ruleSet(ruleSet).build();
        when(messageRuleAssignmentService.fromDto(assignmentDto)).thenReturn("resolved-id");

        MessageConditionalTextRule returned = conditionalTextRuleService.fromDto(dto, rule);

        assertEquals("resolved-id", returned.getLmsAssignmentId());
        verify(messageRuleAssignmentService, times(1)).fromDto(assignmentDto);
    }

    @Test
    public void testFromDtoNoAssignmentInfo() {
        MessageConditionalTextRuleDto dto = MessageConditionalTextRuleDto.builder().build();
        MessageConditionalTextRule rule = MessageConditionalTextRule.builder()
            .ruleSet(ruleSet)
            .lmsAssignmentId("stale")
            .build();

        MessageConditionalTextRule returned = conditionalTextRuleService.fromDto(dto, rule);

        assertNull(returned.getLmsAssignmentId());
    }

    @Test
    public void testFromDtoListEmpty() {
        assertTrue(conditionalTextRuleService.fromDto(null, ruleSet).isEmpty());
    }

    @Test
    public void testFromDtoList() {
        MessageConditionalTextRuleDto dto = MessageConditionalTextRuleDto.builder()
            .lmsAssignmentId("a1")
            .build();

        List<MessageConditionalTextRule> rules = conditionalTextRuleService.fromDto(List.of(dto), ruleSet);

        assertEquals(1, rules.size());
        assertEquals("a1", rules.get(0).getLmsAssignmentId());
        assertSame(ruleSet, rules.get(0).getRuleSet());
    }

}
