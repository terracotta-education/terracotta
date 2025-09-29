package edu.iu.terracotta.service.app.messaging.impl.recipient;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleSetDto;
import edu.iu.terracotta.service.app.messaging.MessageRecipientRuleService;
import edu.iu.terracotta.service.app.messaging.MessageRecipientRuleSetService;

@Service
@SuppressWarnings({"PMD.LambdaCanBeMethodReference"})
public class MessageRecipientRuleSetServiceImpl implements MessageRecipientRuleSetService {

    @Autowired private MessageRecipientRuleService recipientRuleService;

    @Override
    public void create(MessageRecipientRuleSetDto recipientRuleSetDto, Message message) {
        message.getRuleSets().add(
            fromDto(
                recipientRuleSetDto,
                MessageRecipientRuleSet.builder()
                    .message(message)
                    .build()
            )
        );
    }

    @Override
    public void update(MessageRecipientRuleSetDto recipientRuleSetDto, MessageRecipientRuleSet recipientRuleSet) {
        fromDto(recipientRuleSetDto, recipientRuleSet);
    }

    @Override
    public void update(List<MessageRecipientRuleSetDto> recipientRuleSetDtos, Message message) {
        List<MessageRecipientRuleSet> existingRecipientRuleSets = message.getRuleSets().stream().toList();

        message.getRuleSets().clear();
        message.getRuleSets().addAll(
            CollectionUtils.emptyIfNull(recipientRuleSetDtos).stream()
                .map(
                    recipientRuleSetDto -> {
                        Optional<MessageRecipientRuleSet> recipientRuleSet = existingRecipientRuleSets.stream()
                            .filter(existing -> existing.getUuid().equals(recipientRuleSetDto.getId()))
                            .findFirst();

                        if (recipientRuleSet.isEmpty()) {
                            create(recipientRuleSetDto, message);
                            recipientRuleService.create(recipientRuleSetDto.getRules(), message.getRuleSets().getLast());

                            return message.getRuleSets().getLast();
                        } else {
                            update(recipientRuleSetDto, recipientRuleSet.get());
                            recipientRuleService.update(recipientRuleSetDto.getRules(), recipientRuleSet.get());

                            return recipientRuleSet.get();
                        }
                    }
                )
                .toList()
        );
    }

    @Override
    public void duplicate(List<MessageRecipientRuleSet> recipientRuleSets, Message message) {
        message.setRuleSets(
            CollectionUtils.emptyIfNull(recipientRuleSets).stream()
                .map(
                    recipientRuleSet -> {
                        MessageRecipientRuleSet newRuleSet = MessageRecipientRuleSet.builder()
                            .message(message)
                            .operator(recipientRuleSet.getOperator())
                            .build();
                        recipientRuleService.duplicate(recipientRuleSet.getRules(), newRuleSet);

                        return newRuleSet;
                    }
                )
                .toList()
        );
    }

    @Override
    public List<MessageRecipientRuleSetDto> toDto(List<MessageRecipientRuleSet> recipientRuleSets) {
        return CollectionUtils.emptyIfNull(recipientRuleSets).stream()
            .map(recipientRuleSet -> toDto(recipientRuleSet))
            .toList();
    }

    @Override
    public MessageRecipientRuleSetDto toDto(MessageRecipientRuleSet recipientRuleSet) {
        return MessageRecipientRuleSetDto.builder()
            .id(recipientRuleSet.getUuid())
            .messageId(recipientRuleSet.getMessage().getUuid())
            .operator(recipientRuleSet.getOperator())
            .rules(recipientRuleService.toDto(recipientRuleSet.getRules()))
            .build();
    }

    @Override
    public MessageRecipientRuleSet fromDto(MessageRecipientRuleSetDto recipientRuleSetDto, MessageRecipientRuleSet recipientRuleSet) {
        if (recipientRuleSetDto == null) {
            return recipientRuleSet;
        }

        recipientRuleSet.setOperator(recipientRuleSetDto.getOperator());

        return recipientRuleSet;
    }

}
