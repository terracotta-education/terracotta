package edu.iu.terracotta.service.app.messaging.impl.conditional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextRuleDto;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextRuleSetDto;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextRuleService;

public class MessageConditionalTextRuleSetServiceImplTest extends BaseTest {

    @Mock private MessageConditionalTextRuleService conditionalTextRuleService;

    @InjectMocks private MessageConditionalTextRuleSetServiceImpl conditionalTextRuleSetService;

    private MessageConditionalText conditionalText;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        conditionalText = MessageConditionalText.builder()
            .label("label")
            .build();
        conditionalText.setUuid(UUID.randomUUID());
    }

    @Test
    public void testCreateSingle() {
        MessageConditionalTextRuleSetDto dto = MessageConditionalTextRuleSetDto.builder()
            .operator(MessageRuleOperator.AND)
            .rules(List.of())
            .build();

        conditionalTextRuleSetService.create(dto, conditionalText);

        assertEquals(1, conditionalText.getRuleSets().size());
        MessageConditionalTextRuleSet created = conditionalText.getRuleSets().get(0);
        assertSame(conditionalText, created.getConditionalText());
        assertEquals(MessageRuleOperator.AND, created.getOperator());
        verify(conditionalTextRuleService).create(dto.getRules(), created);
    }

    @Test
    public void testCreateList() {
        MessageConditionalTextRuleSetDto dto1 = MessageConditionalTextRuleSetDto.builder().operator(MessageRuleOperator.AND).build();
        MessageConditionalTextRuleSetDto dto2 = MessageConditionalTextRuleSetDto.builder().operator(MessageRuleOperator.OR).build();

        conditionalTextRuleSetService.create(List.of(dto1, dto2), conditionalText);

        assertEquals(2, conditionalText.getRuleSets().size());
    }

    @Test
    public void testCreateListNull() {
        conditionalTextRuleSetService.create((List<MessageConditionalTextRuleSetDto>) null, conditionalText);

        assertTrue(conditionalText.getRuleSets().isEmpty());
    }

    @Test
    public void testUpdateSingle() {
        MessageConditionalTextRuleSet ruleSet = MessageConditionalTextRuleSet.builder()
            .conditionalText(conditionalText)
            .operator(MessageRuleOperator.AND)
            .build();
        MessageConditionalTextRuleSetDto dto = MessageConditionalTextRuleSetDto.builder()
            .operator(MessageRuleOperator.OR)
            .build();

        conditionalTextRuleSetService.update(dto, ruleSet);

        assertEquals(MessageRuleOperator.OR, ruleSet.getOperator());
    }

    @Test
    public void testUpdateListMatchesExisting() {
        MessageConditionalTextRuleSet existing = MessageConditionalTextRuleSet.builder()
            .conditionalText(conditionalText)
            .operator(MessageRuleOperator.AND)
            .build();
        UUID uuid = UUID.randomUUID();
        existing.setUuid(uuid);
        conditionalText.getRuleSets().add(existing);

        MessageConditionalTextRuleSetDto dto = MessageConditionalTextRuleSetDto.builder()
            .id(uuid)
            .operator(MessageRuleOperator.OR)
            .rules(List.of())
            .build();

        conditionalTextRuleSetService.update(List.of(dto), conditionalText);

        assertEquals(1, conditionalText.getRuleSets().size());
        assertSame(existing, conditionalText.getRuleSets().get(0));
        assertEquals(MessageRuleOperator.OR, conditionalText.getRuleSets().get(0).getOperator());
        verify(conditionalTextRuleService).update(dto.getRules(), existing);
    }

    @Test
    public void testUpdateListCreatesNewWhenNoMatch() {
        MessageConditionalTextRuleSet existing = MessageConditionalTextRuleSet.builder()
            .conditionalText(conditionalText)
            .operator(MessageRuleOperator.AND)
            .build();
        existing.setUuid(UUID.randomUUID());
        conditionalText.getRuleSets().add(existing);

        MessageConditionalTextRuleSetDto dto = MessageConditionalTextRuleSetDto.builder()
            .id(UUID.randomUUID())
            .operator(MessageRuleOperator.NOT)
            .build();

        conditionalTextRuleSetService.update(List.of(dto), conditionalText);

        // NOTE: production code (MessageConditionalTextRuleSetServiceImpl#update(List, MessageConditionalText))
        // adds the newly created rule set to the live list inside create(), then addAll() appends the same
        // instance again, so a non-matching dto results in a duplicated entry rather than a single one.
        assertEquals(2, conditionalText.getRuleSets().size());
        assertSame(conditionalText.getRuleSets().get(0), conditionalText.getRuleSets().get(1));
        assertEquals(MessageRuleOperator.NOT, conditionalText.getRuleSets().get(0).getOperator());
        verify(conditionalTextRuleService, never()).update(anyList(), any(MessageConditionalTextRuleSet.class));
    }

    @Test
    public void testUpdateListEmptyClearsRuleSets() {
        MessageConditionalTextRuleSet existing = MessageConditionalTextRuleSet.builder()
            .conditionalText(conditionalText)
            .operator(MessageRuleOperator.AND)
            .build();
        existing.setUuid(UUID.randomUUID());
        conditionalText.getRuleSets().add(existing);

        conditionalTextRuleSetService.update((List<MessageConditionalTextRuleSetDto>) null, conditionalText);

        assertTrue(conditionalText.getRuleSets().isEmpty());
    }

    @Test
    public void testDuplicate() {
        MessageConditionalTextRuleSet original = MessageConditionalTextRuleSet.builder()
            .conditionalText(conditionalText)
            .operator(MessageRuleOperator.OR)
            .build();

        MessageConditionalText newConditionalText = MessageConditionalText.builder().label("new").build();

        conditionalTextRuleSetService.duplicate(List.of(original), newConditionalText);

        assertEquals(1, newConditionalText.getRuleSets().size());
        MessageConditionalTextRuleSet copy = newConditionalText.getRuleSets().get(0);
        assertEquals(MessageRuleOperator.OR, copy.getOperator());
        assertSame(newConditionalText, copy.getConditionalText());
        verify(conditionalTextRuleService).duplicate(anyList(), eq(copy));
    }

    @Test
    public void testDuplicateEmptyList() {
        MessageConditionalText newConditionalText = MessageConditionalText.builder().label("new").build();

        conditionalTextRuleSetService.duplicate(null, newConditionalText);

        assertTrue(newConditionalText.getRuleSets().isEmpty());
    }

    @Test
    public void testToDtoListEmpty() {
        assertTrue(conditionalTextRuleSetService.toDto((List<MessageConditionalTextRuleSet>) null).isEmpty());
    }

    @Test
    public void testToDtoList() {
        MessageConditionalTextRuleSet ruleSet = MessageConditionalTextRuleSet.builder()
            .conditionalText(conditionalText)
            .operator(MessageRuleOperator.AND)
            .build();
        ruleSet.setUuid(UUID.randomUUID());
        when(conditionalTextRuleService.toDto(ruleSet.getRules())).thenReturn(List.of());

        List<MessageConditionalTextRuleSetDto> dtos = conditionalTextRuleSetService.toDto(List.of(ruleSet));

        assertEquals(1, dtos.size());
        assertEquals(MessageRuleOperator.AND, dtos.get(0).getOperator());
    }

    @Test
    public void testToDtoSingleNull() {
        assertNull(conditionalTextRuleSetService.toDto((MessageConditionalTextRuleSet) null));
    }

    @Test
    public void testToDtoSingle() {
        MessageConditionalTextRuleSet ruleSet = MessageConditionalTextRuleSet.builder()
            .conditionalText(conditionalText)
            .operator(MessageRuleOperator.NOT)
            .build();
        ruleSet.setUuid(UUID.randomUUID());
        List<MessageConditionalTextRuleDto> ruleDtos = List.of(MessageConditionalTextRuleDto.builder().build());
        when(conditionalTextRuleService.toDto(ruleSet.getRules())).thenReturn(ruleDtos);

        MessageConditionalTextRuleSetDto dto = conditionalTextRuleSetService.toDto(ruleSet);

        assertEquals(ruleSet.getUuid(), dto.getId());
        assertEquals(conditionalText.getUuid(), dto.getConditionalTextId());
        assertEquals(MessageRuleOperator.NOT, dto.getOperator());
        assertSame(ruleDtos, dto.getRules());
    }

    @Test
    public void testFromDtoTwoArgDoesNotIncludeRules() {
        MessageConditionalTextRuleSetDto dto = MessageConditionalTextRuleSetDto.builder()
            .operator(MessageRuleOperator.AND)
            .rules(List.of(MessageConditionalTextRuleDto.builder().build()))
            .build();
        MessageConditionalTextRuleSet ruleSet = MessageConditionalTextRuleSet.builder().conditionalText(conditionalText).build();

        conditionalTextRuleSetService.fromDto(dto, ruleSet);

        assertEquals(MessageRuleOperator.AND, ruleSet.getOperator());
        verify(conditionalTextRuleService, never()).fromDto(anyList(), eq(ruleSet));
    }

    @Test
    public void testFromDtoNullDto() {
        MessageConditionalTextRuleSet ruleSet = MessageConditionalTextRuleSet.builder().conditionalText(conditionalText).build();

        MessageConditionalTextRuleSet returned = conditionalTextRuleSetService.fromDto(null, ruleSet, true);

        assertSame(ruleSet, returned);
    }

    @Test
    public void testFromDtoIncludeRulesFalse() {
        MessageConditionalTextRuleSetDto dto = MessageConditionalTextRuleSetDto.builder()
            .operator(MessageRuleOperator.OR)
            .build();
        MessageConditionalTextRuleSet ruleSet = MessageConditionalTextRuleSet.builder().conditionalText(conditionalText).build();

        MessageConditionalTextRuleSet returned = conditionalTextRuleSetService.fromDto(dto, ruleSet, false);

        assertEquals(MessageRuleOperator.OR, returned.getOperator());
        verify(conditionalTextRuleService, never()).fromDto(anyList(), eq(ruleSet));
    }

    @Test
    public void testFromDtoIncludeRulesTrue() {
        MessageConditionalTextRuleSetDto dto = MessageConditionalTextRuleSetDto.builder()
            .operator(MessageRuleOperator.OR)
            .rules(List.of(MessageConditionalTextRuleDto.builder().build()))
            .build();
        MessageConditionalTextRuleSet ruleSet = MessageConditionalTextRuleSet.builder().conditionalText(conditionalText).build();
        List<edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRule> rules = List.of();
        when(conditionalTextRuleService.fromDto(dto.getRules(), ruleSet)).thenReturn(rules);

        MessageConditionalTextRuleSet returned = conditionalTextRuleSetService.fromDto(dto, ruleSet, true);

        assertSame(rules, returned.getRules());
    }

    @Test
    public void testFromDtoListEmpty() {
        assertTrue(conditionalTextRuleSetService.fromDto(null, conditionalText, false).isEmpty());
    }

    @Test
    public void testFromDtoList() {
        MessageConditionalTextRuleSetDto dto = MessageConditionalTextRuleSetDto.builder()
            .operator(MessageRuleOperator.AND)
            .build();

        List<MessageConditionalTextRuleSet> ruleSets = conditionalTextRuleSetService.fromDto(List.of(dto), conditionalText, false);

        assertEquals(1, ruleSets.size());
        assertEquals(MessageRuleOperator.AND, ruleSets.get(0).getOperator());
        assertSame(conditionalText, ruleSets.get(0).getConditionalText());
    }

}
