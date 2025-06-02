package edu.iu.terracotta.service.app.messaging.impl.conditional;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextRuleSetDto;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextRuleService;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextRuleSetService;

@Service
public class MessageConditionalTextRuleSetServiceImpl implements MessageConditionalTextRuleSetService {

    @Autowired private MessageConditionalTextRuleService conditionalTextRuleService;

    @Override
    public void create(MessageConditionalTextRuleSetDto conditionalTextRuleSetDto, MessageConditionalText conditionalText) {
        MessageConditionalTextRuleSet conditionalTextRuleSet = fromDto(
            conditionalTextRuleSetDto,
            MessageConditionalTextRuleSet.builder()
                .conditionalText(conditionalText)
                .build()
        );

        conditionalTextRuleService.create(
            conditionalTextRuleSetDto.getRules(),
            conditionalTextRuleSet
        );

        conditionalText.getRuleSets().add(conditionalTextRuleSet);
    }

    @Override
    public void create(List<MessageConditionalTextRuleSetDto> conditionalTextRuleSetDtos, MessageConditionalText conditionalText) {
        CollectionUtils.emptyIfNull(conditionalTextRuleSetDtos).stream()
            .forEach(
                conditionalTextRuleSetDto -> create(conditionalTextRuleSetDto, conditionalText)
            );
    }

    @Override
    public void update(MessageConditionalTextRuleSetDto conditionalTextRuleSetDto, MessageConditionalTextRuleSet conditionalTextRuleSet) {
        fromDto(conditionalTextRuleSetDto, conditionalTextRuleSet);
    }

    @Override
    public void update(List<MessageConditionalTextRuleSetDto> conditionalTextRuleSetDtos, MessageConditionalText conditionalText) {
        List<MessageConditionalTextRuleSet> existingMessageConditionalTextRuleSets = conditionalText.getRuleSets().stream().toList();

        conditionalText.getRuleSets().clear();
        conditionalText.getRuleSets().addAll(
            CollectionUtils.emptyIfNull(conditionalTextRuleSetDtos).stream()
                .map(
                    conditionalTextRuleSetDto -> {
                        Optional<MessageConditionalTextRuleSet> conditionalTextRuleSet = existingMessageConditionalTextRuleSets.stream()
                            .filter(existing -> existing.getUuid().equals(conditionalTextRuleSetDto.getId()))
                            .findFirst();

                        if (conditionalTextRuleSet.isEmpty()) {
                            create(conditionalTextRuleSetDto, conditionalText);

                            return conditionalText.getRuleSets().getLast();
                        }

                        update(conditionalTextRuleSetDto, conditionalTextRuleSet.get());
                        conditionalTextRuleService.update(conditionalTextRuleSetDto.getRules(), conditionalTextRuleSet.get());

                        return conditionalTextRuleSet.get();
                    }
                )
                .toList()
        );
    }

    @Override
    public void duplicate(List<MessageConditionalTextRuleSet> conditionalTextRuleSets, MessageConditionalText conditionalText) {
        conditionalText.setRuleSets(
            CollectionUtils.emptyIfNull(conditionalTextRuleSets).stream()
                .map(
                    conditionalTextRuleSet -> {
                        MessageConditionalTextRuleSet newMessageConditionalTextRuleSet = MessageConditionalTextRuleSet.builder()
                            .conditionalText(conditionalText)
                            .operator(conditionalTextRuleSet.getOperator())
                            .build();

                        conditionalTextRuleService.duplicate(newMessageConditionalTextRuleSet.getRules(), newMessageConditionalTextRuleSet);

                        return newMessageConditionalTextRuleSet;
                    }
                )
                .toList()
        );
    }

    @Override
    public List<MessageConditionalTextRuleSetDto> toDto(List<MessageConditionalTextRuleSet> conditionalTextRuleSets) {
        if (CollectionUtils.isEmpty(conditionalTextRuleSets)) {
            return List.of();
        }

        return conditionalTextRuleSets.stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public MessageConditionalTextRuleSetDto toDto(MessageConditionalTextRuleSet conditionalTextRuleSet) {
        if (conditionalTextRuleSet == null) {
            return null;
        }

        return MessageConditionalTextRuleSetDto.builder()
            .conditionalTextId(conditionalTextRuleSet.getConditionalText().getUuid())
            .id(conditionalTextRuleSet.getUuid())
            .operator(conditionalTextRuleSet.getOperator())
            .rules(
                conditionalTextRuleService.toDto(conditionalTextRuleSet.getRules())
            )
            .build();
    }

    @Override
    public MessageConditionalTextRuleSet fromDto(MessageConditionalTextRuleSetDto conditionalTextRuleSetDto, MessageConditionalTextRuleSet conditionalTextRuleSet) {
        return fromDto(conditionalTextRuleSetDto, conditionalTextRuleSet, false);
    }

    @Override
    public MessageConditionalTextRuleSet fromDto(MessageConditionalTextRuleSetDto conditionalTextRuleSetDto, MessageConditionalTextRuleSet conditionalTextRuleSet, boolean includeRules) {
        if (conditionalTextRuleSetDto == null) {
            return conditionalTextRuleSet;
        }

        conditionalTextRuleSet.setOperator(conditionalTextRuleSetDto.getOperator());

        if (includeRules) {
            conditionalTextRuleSet.setRules(
                conditionalTextRuleService.fromDto(conditionalTextRuleSetDto.getRules(), conditionalTextRuleSet)
            );
        }

        return conditionalTextRuleSet;
    }

    @Override
    public List<MessageConditionalTextRuleSet> fromDto(List<MessageConditionalTextRuleSetDto> conditionalTextRuleSetDtos, MessageConditionalText conditionalText, boolean includeRules) {
        if (CollectionUtils.isEmpty(conditionalTextRuleSetDtos)) {
            return List.of();
        }

        return conditionalTextRuleSetDtos.stream()
            .map(
                conditionalTextRuleSetDto ->
                    fromDto(
                        conditionalTextRuleSetDto,
                        MessageConditionalTextRuleSet.builder()
                            .conditionalText(conditionalText)
                            .build(),
                        includeRules
                    )
            )
            .toList();
    }

}
