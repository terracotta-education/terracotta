package edu.iu.terracotta.service.app.messaging.impl.recipient;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRule;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleDto;
import edu.iu.terracotta.service.app.messaging.MessageRuleAssignmentService;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;
import edu.iu.terracotta.service.app.messaging.MessageRecipientRuleService;

@Service
@SuppressWarnings({"PMD.LambdaCanBeMethodReference"})
public class MessageRecipientRuleServiceImpl implements MessageRecipientRuleService {

    @Autowired private MessageRuleAssignmentService recipientRuleAssignmentService;
    @Autowired private MessageRuleComparisonService recipientRuleComparisonService;

    @Override
    public void create(MessageRecipientRuleDto recipientRuleDto, MessageRecipientRuleSet recipientRuleSet) {
        recipientRuleSet.getRules().add(
            fromDto(
                recipientRuleDto,
                MessageRecipientRule.builder()
                    .ruleSet(recipientRuleSet)
                    .build()
            )
        );
    }

    @Override
    public void create(List<MessageRecipientRuleDto> recipientRuleDtos, MessageRecipientRuleSet recipientRuleSet) {
        CollectionUtils.emptyIfNull(recipientRuleDtos)
            .forEach(
                recipientRuleDto -> create(recipientRuleDto, recipientRuleSet)
            );
    }

    @Override
    public void update(MessageRecipientRuleDto recipientRuleDto, MessageRecipientRule recipientRule) {
        fromDto(recipientRuleDto, recipientRule);
    }

    @Override
    public void update(List<MessageRecipientRuleDto> recipientRuleDtos, MessageRecipientRuleSet recipientRuleSet) {
        List<MessageRecipientRule> existingMessageRules = recipientRuleSet.getRules().stream().toList();

        recipientRuleSet.getRules().clear();
        recipientRuleSet.getRules().addAll(
            CollectionUtils.emptyIfNull(recipientRuleDtos).stream()
                .map(
                    recipientRuleDto -> {
                        Optional<MessageRecipientRule> recipientRule = existingMessageRules.stream()
                            .filter(existing -> existing.getUuid().equals(recipientRuleDto.getId()))
                            .findFirst();

                        if (recipientRule.isEmpty()) {
                            create(recipientRuleDto, recipientRuleSet);

                            return recipientRuleSet.getRules().get(recipientRuleSet.getRules().size() - 1);
                        } else {
                            update(recipientRuleDto, recipientRule.get());

                            return recipientRule.get();
                        }
                    }
                )
                .toList()
        );
    }

    @Override
    public void duplicate(List<MessageRecipientRule> recipientRules, MessageRecipientRuleSet recipientRuleSet) {
        recipientRuleSet.setRules(
            CollectionUtils.emptyIfNull(recipientRules).stream()
                .map(recipientRule ->
                    MessageRecipientRule.builder()
                        .lmsAssignmentId(recipientRule.getLmsAssignmentId())
                        .comparison(recipientRule.getComparison())
                        .ruleSet(recipientRuleSet)
                        .operator(recipientRule.getOperator())
                        .value(recipientRule.getValue())
                        .build()
                )
                .toList()
        );
    }

    @Override
    public List<MessageRecipientRuleDto> toDto(List<MessageRecipientRule> recipientRules) {
        return CollectionUtils.emptyIfNull(recipientRules).stream()
            .map(recipientRule -> toDto(recipientRule))
            .toList();
    }

    @Override
    public MessageRecipientRuleDto toDto(MessageRecipientRule recipientRule) {
        if (recipientRule == null) {
            return null;
        }

        return MessageRecipientRuleDto.builder()
            .id(recipientRule.getUuid())
            .comparison(
                recipientRuleComparisonService.toDto(recipientRule.getComparison())
            )
            .operator(recipientRule.getOperator())
            .lmsAssignmentId(recipientRule.getLmsAssignmentId())
            .value(recipientRule.getValue())
            .build();
    }

    @Override
    public MessageRecipientRule fromDto(MessageRecipientRuleDto recipientRuleDto, MessageRecipientRule recipientRule) {
        recipientRule.setComparison(recipientRuleDto.getComparison().getId());
        recipientRule.setOperator(recipientRuleDto.getOperator());
        recipientRule.setValue(recipientRuleDto.getValue());

        if (recipientRuleDto.getLmsAssignmentId() != null) {
            recipientRule.setLmsAssignmentId(recipientRuleDto.getLmsAssignmentId());
        } else if (recipientRuleDto.getAssignment() != null) {
            recipientRule.setLmsAssignmentId(recipientRuleAssignmentService.fromDto(recipientRuleDto.getAssignment()));
        } else {
            recipientRule.setLmsAssignmentId(null);
        }

        return recipientRule;
    }

}
