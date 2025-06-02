package edu.iu.terracotta.service.app.messaging.impl.conditional;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRule;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextRuleDto;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextRuleService;
import edu.iu.terracotta.service.app.messaging.MessageRuleAssignmentService;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;

@Service
public class MessageConditionalTextRuleServiceImpl implements MessageConditionalTextRuleService {

    @Autowired private MessageRuleComparisonService messageRuleComparisonService;
    @Autowired private MessageRuleAssignmentService messageRuleAssignmentService;

    @Override
    public void create(MessageConditionalTextRuleDto conditionalTextRuleDto, MessageConditionalTextRuleSet conditionalTextRuleSet) {
        conditionalTextRuleSet.getRules().add(
            fromDto(
                conditionalTextRuleDto,
                MessageConditionalTextRule.builder()
                    .ruleSet(conditionalTextRuleSet)
                    .build()
            )
        );
    }

    @Override
    public void create(List<MessageConditionalTextRuleDto> conditionalTextRuleDtos, MessageConditionalTextRuleSet conditionalTextRuleSet) {
        CollectionUtils.emptyIfNull(conditionalTextRuleDtos).stream()
            .forEach(
                conditionalTextRuleDto -> create(conditionalTextRuleDto, conditionalTextRuleSet)
            );
    }

    @Override
    public void update(MessageConditionalTextRuleDto conditionalTextRuleDto, MessageConditionalTextRule conditionalTextRule) {
        fromDto(conditionalTextRuleDto, conditionalTextRule);
    }

    @Override
    public void update(List<MessageConditionalTextRuleDto> conditionalTextRuleDtos, MessageConditionalTextRuleSet conditionalTextRuleSet) {
        List<MessageConditionalTextRule> existingMessageRules = conditionalTextRuleSet.getRules().stream().toList();

        conditionalTextRuleSet.getRules().clear();
        conditionalTextRuleSet.getRules().addAll(
            CollectionUtils.emptyIfNull(conditionalTextRuleDtos).stream()
                .map(
                    conditionalTextRuleDto -> {
                        Optional<MessageConditionalTextRule> conditionalTextRule = existingMessageRules.stream()
                            .filter(existing -> existing.getUuid().equals(conditionalTextRuleDto.getId()))
                            .findFirst();

                        if (conditionalTextRule.isEmpty()) {
                            create(conditionalTextRuleDto, conditionalTextRuleSet);

                            return conditionalTextRuleSet.getRules().getLast();
                        }

                        update(conditionalTextRuleDto, conditionalTextRule.get());

                        return conditionalTextRule.get();
                    }
                )
                .toList()
        );
    }

    @Override
    public void duplicate(List<MessageConditionalTextRule> conditionalTextRules, MessageConditionalTextRuleSet conditionalTextRuleSet) {
        conditionalTextRuleSet.setRules(
            CollectionUtils.emptyIfNull(conditionalTextRules).stream()
                .map(conditionalTextRule ->
                    MessageConditionalTextRule.builder()
                        .comparison(conditionalTextRule.getComparison())
                        .lmsAssignmentId(conditionalTextRule.getLmsAssignmentId())
                        .operator(conditionalTextRule.getOperator())
                        .ruleSet(conditionalTextRuleSet)
                        .value(conditionalTextRule.getValue())
                        .build()
                )
                .toList()
        );
    }

    @Override
    public List<MessageConditionalTextRuleDto> toDto(List<MessageConditionalTextRule> conditionalTextRules) {
        if (CollectionUtils.isEmpty(conditionalTextRules)) {
            return List.of();
        }

        return conditionalTextRules.stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public MessageConditionalTextRuleDto toDto(MessageConditionalTextRule conditionalTextRule) {
        if (conditionalTextRule == null) {
            return null;
        }

        return MessageConditionalTextRuleDto.builder()
            .comparison(
                messageRuleComparisonService.toDto(conditionalTextRule.getComparison())
            )
            .id(conditionalTextRule.getUuid())
            .lmsAssignmentId(conditionalTextRule.getLmsAssignmentId())
            .operator(conditionalTextRule.getOperator())
            .ruleSetId(conditionalTextRule.getRuleSet().getUuid())
            .value(conditionalTextRule.getValue())
            .build();
    }

    @Override
    public MessageConditionalTextRule fromDto(MessageConditionalTextRuleDto conditionalTextRuleDto, MessageConditionalTextRule conditionalTextRule) {
        if (conditionalTextRuleDto == null) {
            return conditionalTextRule;
        }

        conditionalTextRule.setComparison(
            messageRuleComparisonService.fromDto(conditionalTextRuleDto.getComparison())
        );
        conditionalTextRule.setOperator(conditionalTextRuleDto.getOperator());
        conditionalTextRule.setValue(conditionalTextRuleDto.getValue());

        if (conditionalTextRuleDto.getLmsAssignmentId() != null) {
            conditionalTextRule.setLmsAssignmentId(conditionalTextRuleDto.getLmsAssignmentId());
        } else if (conditionalTextRuleDto.getAssignment() != null) {
            conditionalTextRule.setLmsAssignmentId(messageRuleAssignmentService.fromDto(conditionalTextRuleDto.getAssignment()));
        } else {
            conditionalTextRule.setLmsAssignmentId(null);
        }

        return conditionalTextRule;
    }

    @Override
    public List<MessageConditionalTextRule> fromDto(List<MessageConditionalTextRuleDto> conditionalTextRuleDtos, MessageConditionalTextRuleSet conditionalTextRuleSet) {
        if (CollectionUtils.isEmpty(conditionalTextRuleDtos)) {
            return List.of();
        }

        return conditionalTextRuleDtos.stream()
            .map(
                conditionalTextRuleDto ->
                    fromDto(
                        conditionalTextRuleDto,
                        MessageConditionalTextRule.builder()
                            .ruleSet(conditionalTextRuleSet)
                            .build()
                    )
            )
            .toList();
    }

}
