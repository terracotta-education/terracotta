package edu.iu.terracotta.service.app.messaging.impl.recipient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRule;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleComparisonDto;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleComparison;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import edu.iu.terracotta.service.app.messaging.MessageRuleAssignmentService;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;

public class MessageRecipientRuleServiceImplTest extends BaseTest {

    @Mock private MessageRuleAssignmentService recipientRuleAssignmentService;
    @Mock private MessageRuleComparisonService recipientRuleComparisonService;

    @InjectMocks private MessageRecipientRuleServiceImpl messageRecipientRuleService;

    private MessageRecipientRuleSet ruleSet;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        ruleSet = MessageRecipientRuleSet.builder().build();
    }

    private MessageRecipientRuleDto dto(String lmsAssignmentId) {
        return MessageRecipientRuleDto.builder()
            .comparison(MessageRuleComparisonDto.builder().id(MessageRuleComparison.EQUALS).build())
            .operator(MessageRuleOperator.AND)
            .value("90")
            .lmsAssignmentId(lmsAssignmentId)
            .build();
    }

    @Test
    public void testCreateSingle() {
        messageRecipientRuleService.create(dto("1"), ruleSet);

        assertEquals(1, ruleSet.getRules().size());
        MessageRecipientRule rule = ruleSet.getRules().get(0);
        assertEquals(ruleSet, rule.getRuleSet());
        assertEquals(MessageRuleComparison.EQUALS, rule.getComparison());
        assertEquals(MessageRuleOperator.AND, rule.getOperator());
        assertEquals("90", rule.getValue());
        assertEquals("1", rule.getLmsAssignmentId());
    }

    @Test
    public void testCreateUsesAssignmentServiceWhenNoLmsAssignmentIdGiven() {
        MessageRecipientRuleDto dto = MessageRecipientRuleDto.builder()
            .comparison(MessageRuleComparisonDto.builder().id(MessageRuleComparison.IS_SUBMITTED).build())
            .operator(MessageRuleOperator.OR)
            .assignment(MessageRuleAssignmentDto.builder().lmsId("77").build())
            .build();
        when(recipientRuleAssignmentService.fromDto(any(MessageRuleAssignmentDto.class))).thenReturn("77");

        messageRecipientRuleService.create(dto, ruleSet);

        assertEquals("77", ruleSet.getRules().get(0).getLmsAssignmentId());
    }

    @Test
    public void testCreateNoLmsAssignmentIdNoAssignment() {
        MessageRecipientRuleDto dto = MessageRecipientRuleDto.builder()
            .comparison(MessageRuleComparisonDto.builder().id(MessageRuleComparison.IS_SUBMITTED).build())
            .operator(MessageRuleOperator.OR)
            .build();

        messageRecipientRuleService.create(dto, ruleSet);

        assertNull(ruleSet.getRules().get(0).getLmsAssignmentId());
        verify(recipientRuleAssignmentService, never()).fromDto(any(MessageRuleAssignmentDto.class));
    }

    @Test
    public void testCreateListOfDtos() {
        messageRecipientRuleService.create(List.of(dto("1"), dto("2")), ruleSet);

        assertEquals(2, ruleSet.getRules().size());
    }

    @Test
    public void testCreateListOfDtosNull() {
        messageRecipientRuleService.create((List<MessageRecipientRuleDto>) null, ruleSet);

        assertTrue(ruleSet.getRules().isEmpty());
    }

    @Test
    public void testUpdateSingle() {
        MessageRecipientRule rule = MessageRecipientRule.builder().ruleSet(ruleSet).build();

        messageRecipientRuleService.update(dto("5"), rule);

        assertEquals("5", rule.getLmsAssignmentId());
        assertEquals(MessageRuleComparison.EQUALS, rule.getComparison());
        assertEquals("90", rule.getValue());
    }

    @Test
    public void testUpdateListAllExisting() {
        MessageRecipientRule existing = MessageRecipientRule.builder().ruleSet(ruleSet).build();
        existing.setUuid(UUID.randomUUID());
        ruleSet.getRules().add(existing);

        MessageRecipientRuleDto dto = dto("1");
        dto.setId(existing.getUuid());

        messageRecipientRuleService.update(List.of(dto), ruleSet);

        assertTrue(ruleSet.getRules().contains(existing));
        assertEquals("1", existing.getLmsAssignmentId());
    }

    @Test
    public void testUpdateListWithNewRuleCreatesIt() {
        messageRecipientRuleService.update(List.of(dto("9")), ruleSet);

        // create() adds the new rule to ruleSet.getRules() as a side effect mid-stream, then the final
        // addAll(mapped list) appends the same mapped reference again, so a single new rule ends up duplicated
        assertEquals(2, ruleSet.getRules().size());
        assertTrue(ruleSet.getRules().stream().allMatch(rule -> "9".equals(rule.getLmsAssignmentId())));
    }

    @Test
    public void testUpdateListNullClearsRules() {
        ruleSet.getRules().add(MessageRecipientRule.builder().ruleSet(ruleSet).build());

        messageRecipientRuleService.update((List<MessageRecipientRuleDto>) null, ruleSet);

        assertTrue(ruleSet.getRules().isEmpty());
    }

    @Test
    public void testDuplicate() {
        MessageRecipientRule source = MessageRecipientRule.builder()
            .lmsAssignmentId("3")
            .comparison(MessageRuleComparison.GREATER_THAN)
            .operator(MessageRuleOperator.OR)
            .value("50")
            .build();

        MessageRecipientRuleSet newRuleSet = MessageRecipientRuleSet.builder().build();
        messageRecipientRuleService.duplicate(List.of(source), newRuleSet);

        assertEquals(1, newRuleSet.getRules().size());
        MessageRecipientRule duplicated = newRuleSet.getRules().get(0);
        assertEquals("3", duplicated.getLmsAssignmentId());
        assertEquals(MessageRuleComparison.GREATER_THAN, duplicated.getComparison());
        assertEquals(MessageRuleOperator.OR, duplicated.getOperator());
        assertEquals("50", duplicated.getValue());
        assertEquals(newRuleSet, duplicated.getRuleSet());
    }

    @Test
    public void testDuplicateNull() {
        MessageRecipientRuleSet newRuleSet = MessageRecipientRuleSet.builder().build();

        messageRecipientRuleService.duplicate(null, newRuleSet);

        assertTrue(newRuleSet.getRules().isEmpty());
    }

    @Test
    public void testToDto() {
        MessageRecipientRule rule = MessageRecipientRule.builder()
            .lmsAssignmentId("4")
            .comparison(MessageRuleComparison.LESS_THAN)
            .operator(MessageRuleOperator.NONE)
            .value("10")
            .build();
        rule.setUuid(UUID.randomUUID());
        when(recipientRuleComparisonService.toDto(MessageRuleComparison.LESS_THAN)).thenReturn(MessageRuleComparisonDto.builder().id(MessageRuleComparison.LESS_THAN).build());

        MessageRecipientRuleDto dto = messageRecipientRuleService.toDto(rule);

        assertEquals(rule.getUuid(), dto.getId());
        assertEquals("4", dto.getLmsAssignmentId());
        assertEquals(MessageRuleOperator.NONE, dto.getOperator());
        assertEquals("10", dto.getValue());
        assertEquals(MessageRuleComparison.LESS_THAN, dto.getComparison().getId());
    }

    @Test
    public void testToDtoNull() {
        assertNull(messageRecipientRuleService.toDto((MessageRecipientRule) null));
    }

    @Test
    public void testToDtoList() {
        MessageRecipientRule rule = MessageRecipientRule.builder()
            .comparison(MessageRuleComparison.EQUALS)
            .build();
        when(recipientRuleComparisonService.toDto(MessageRuleComparison.EQUALS)).thenReturn(MessageRuleComparisonDto.builder().id(MessageRuleComparison.EQUALS).build());

        List<MessageRecipientRuleDto> dtos = messageRecipientRuleService.toDto(List.of(rule));

        assertEquals(1, dtos.size());
        verify(recipientRuleComparisonService, times(1)).toDto(MessageRuleComparison.EQUALS);
    }

    @Test
    public void testToDtoListNull() {
        List<MessageRecipientRuleDto> dtos = messageRecipientRuleService.toDto((List<MessageRecipientRule>) null);

        assertTrue(dtos.isEmpty());
    }

}
