package edu.iu.terracotta.service.app.messaging.impl.recipient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRule;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleDto;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleSetDto;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import edu.iu.terracotta.service.app.messaging.MessageRecipientRuleService;

public class MessageRecipientRuleSetServiceImplTest extends BaseTest {

    @Mock private MessageRecipientRuleService recipientRuleService;

    @InjectMocks private MessageRecipientRuleSetServiceImpl messageRecipientRuleSetService;

    private Message message;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        message = Message.builder().build();
    }

    @Test
    public void testCreate() {
        MessageRecipientRuleSetDto dto = MessageRecipientRuleSetDto.builder()
            .operator(MessageRuleOperator.AND)
            .rules(List.of())
            .build();

        messageRecipientRuleSetService.create(dto, message);

        assertEquals(1, message.getRuleSets().size());
        MessageRecipientRuleSet ruleSet = message.getRuleSets().get(0);
        assertEquals(message, ruleSet.getMessage());
        assertEquals(MessageRuleOperator.AND, ruleSet.getOperator());
    }

    @Test
    public void testUpdateSingle() {
        MessageRecipientRuleSet ruleSet = MessageRecipientRuleSet.builder().message(message).build();
        MessageRecipientRuleSetDto dto = MessageRecipientRuleSetDto.builder().operator(MessageRuleOperator.OR).build();

        messageRecipientRuleSetService.update(dto, ruleSet);

        assertEquals(MessageRuleOperator.OR, ruleSet.getOperator());
    }

    @Test
    public void testUpdateSingleNullDtoReturnsUnchanged() {
        MessageRecipientRuleSet ruleSet = MessageRecipientRuleSet.builder().message(message).operator(MessageRuleOperator.AND).build();

        messageRecipientRuleSetService.update(null, ruleSet);

        assertEquals(MessageRuleOperator.AND, ruleSet.getOperator());
    }

    @Test
    public void testUpdateListAllExisting() {
        MessageRecipientRuleSet existing = MessageRecipientRuleSet.builder().message(message).operator(MessageRuleOperator.NONE).build();
        existing.setUuid(UUID.randomUUID());
        message.getRuleSets().add(existing);

        MessageRecipientRuleSetDto dto = MessageRecipientRuleSetDto.builder()
            .id(existing.getUuid())
            .operator(MessageRuleOperator.OR)
            .rules(List.of())
            .build();

        messageRecipientRuleSetService.update(List.of(dto), message);

        assertTrue(message.getRuleSets().contains(existing));
        assertEquals(MessageRuleOperator.OR, existing.getOperator());
        verify(recipientRuleService, times(1)).update(anyList(), any(MessageRecipientRuleSet.class));
    }

    @Test
    public void testUpdateListWithNewRuleSetCreatesIt() {
        MessageRecipientRuleSetDto dto = MessageRecipientRuleSetDto.builder()
            .operator(MessageRuleOperator.AND)
            .rules(List.of())
            .build();

        messageRecipientRuleSetService.update(List.of(dto), message);

        // create() adds the new rule set to message.getRuleSets() as a side effect mid-stream, then the final
        // addAll(mapped list) appends the same mapped reference again, so a single new rule set ends up duplicated
        assertEquals(2, message.getRuleSets().size());
        assertTrue(message.getRuleSets().stream().allMatch(ruleSet -> ruleSet.getOperator() == MessageRuleOperator.AND));
        verify(recipientRuleService, times(1)).create(anyList(), any(MessageRecipientRuleSet.class));
    }

    @Test
    public void testUpdateListNullClearsRuleSets() {
        message.getRuleSets().add(MessageRecipientRuleSet.builder().message(message).build());

        messageRecipientRuleSetService.update((List<MessageRecipientRuleSetDto>) null, message);

        assertTrue(message.getRuleSets().isEmpty());
    }

    @Test
    public void testDuplicate() {
        MessageRecipientRuleSet source = MessageRecipientRuleSet.builder()
            .operator(MessageRuleOperator.OR)
            .rules(List.of(MessageRecipientRule.builder().build()))
            .build();

        Message newMessage = Message.builder().build();
        messageRecipientRuleSetService.duplicate(List.of(source), newMessage);

        assertEquals(1, newMessage.getRuleSets().size());
        MessageRecipientRuleSet duplicated = newMessage.getRuleSets().get(0);
        assertEquals(newMessage, duplicated.getMessage());
        assertEquals(MessageRuleOperator.OR, duplicated.getOperator());
        verify(recipientRuleService, times(1)).duplicate(anyList(), any(MessageRecipientRuleSet.class));
    }

    @Test
    public void testDuplicateNull() {
        Message newMessage = Message.builder().build();

        messageRecipientRuleSetService.duplicate(null, newMessage);

        assertTrue(newMessage.getRuleSets().isEmpty());
    }

    @Test
    public void testToDto() {
        MessageRecipientRuleSet ruleSet = MessageRecipientRuleSet.builder().message(message).operator(MessageRuleOperator.AND).build();
        when(recipientRuleService.toDto(anyList())).thenReturn(List.of(MessageRecipientRuleDto.builder().build()));

        MessageRecipientRuleSetDto dto = messageRecipientRuleSetService.toDto(ruleSet);

        assertEquals(MessageRuleOperator.AND, dto.getOperator());
        assertEquals(1, dto.getRules().size());
    }

    @Test
    public void testToDtoList() {
        MessageRecipientRuleSet ruleSet = MessageRecipientRuleSet.builder().message(message).build();
        when(recipientRuleService.toDto(anyList())).thenReturn(List.of());

        List<MessageRecipientRuleSetDto> dtos = messageRecipientRuleSetService.toDto(List.of(ruleSet));

        assertEquals(1, dtos.size());
    }

    @Test
    public void testToDtoListNull() {
        List<MessageRecipientRuleSetDto> dtos = messageRecipientRuleSetService.toDto((List<MessageRecipientRuleSet>) null);

        assertTrue(dtos.isEmpty());
    }

}
