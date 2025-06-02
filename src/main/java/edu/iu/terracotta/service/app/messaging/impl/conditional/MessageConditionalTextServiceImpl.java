package edu.iu.terracotta.service.app.messaging.impl.conditional;

import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextResult;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextDto;
import edu.iu.terracotta.dao.repository.messaging.conditional.MessageConditionalTextRepository;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextResultService;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextRuleSetService;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextService;

@Service
public class MessageConditionalTextServiceImpl implements MessageConditionalTextService {

    @Autowired private MessageConditionalTextRepository conditionalTextRepository;
    @Autowired private MessageConditionalTextResultService conditionalTextResultService;
    @Autowired private MessageConditionalTextRuleSetService conditionalTextRuleSetService;

    @Override
    public void create(MessageConditionalTextDto conditionalTextDto, MessageContent content) {
        MessageConditionalText conditionalText = fromDto(
            conditionalTextDto,
            MessageConditionalText.builder()
                .content(content)
                .build()
        );

        conditionalTextRuleSetService.create(conditionalTextDto.getRuleSets(), conditionalText);
        conditionalTextResultService.create(conditionalTextDto.getResult(), conditionalText);

        content.getConditionalTexts().add(conditionalText);
    }

    @Override
    public MessageConditionalTextDto post(MessageConditionalTextDto conditionalTextDto, MessageContent content) {
        create(conditionalTextDto, content);

        return toDto(conditionalTextRepository.save(content.getConditionalTexts().getLast()));
    }

    @Override
    public void update(MessageConditionalTextDto conditionalTextDto, MessageConditionalText conditionalText) {
        fromDto(conditionalTextDto, conditionalText);
        conditionalTextRuleSetService.update(conditionalTextDto.getRuleSets(), conditionalText);
        conditionalTextResultService.update(conditionalTextDto.getResult(), conditionalText);
    }

    @Override
    public MessageConditionalTextDto put(MessageConditionalTextDto conditionalTextDto, MessageConditionalText conditionalText) {
        update(conditionalTextDto, conditionalText);

        return toDto(
            conditionalTextRepository.save(conditionalText)
        );
    }

    @Override
    public void duplicate(List<MessageConditionalText> conditionalTexts, MessageContent content) {
        CollectionUtils.emptyIfNull(conditionalTexts)
            .forEach(
                conditionalText -> duplicate(conditionalText, content)
            );
    }

    @Override
    public void duplicate(MessageConditionalText conditionalText, MessageContent content) {
        MessageConditionalText newMessageConditionalText = MessageConditionalText.builder()
            .content(content)
            .label(conditionalText.getLabel())
            .result(conditionalText.getResult())
            .build();
        newMessageConditionalText.setUuid(UUID.randomUUID());

        conditionalTextRuleSetService.duplicate(conditionalText.getRuleSets(), newMessageConditionalText);
        conditionalTextResultService.duplicate(conditionalText.getResult(), newMessageConditionalText);
        content.getConditionalTexts().add(newMessageConditionalText);
    }

    @Override
    public void delete(MessageConditionalText conditionalText) {
        conditionalTextRepository.delete(conditionalText);
    }

    @Override
    public void upsert(List<MessageConditionalTextDto> conditionalTextDtos, MessageContent content) {
        conditionalTextDtos.forEach(
            conditionalTextDto -> {
                if (conditionalTextDto.getId() == null) {
                    create(conditionalTextDto, content);
                } else {
                    conditionalTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(
                        conditionalTextDto.getId(),
                        content.getUuid(),
                        content.getMessage().getContainer().getOwner().getLmsUserId()
                    )
                    .ifPresentOrElse(
                        conditionalText -> update(conditionalTextDto, conditionalText),
                        () -> create(conditionalTextDto, content)
                    );
                }
            }
        );
    }

    @Override
    public List<MessageConditionalTextDto> toDto(List<MessageConditionalText> conditionalTexts) {
        return conditionalTexts.stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public MessageConditionalTextDto toDto(MessageConditionalText conditionalText) {
        return MessageConditionalTextDto.builder()
            .id(conditionalText.getUuid())
            .contentId(conditionalText.getContent().getUuid())
            .label(conditionalText.getLabel())
            .result(
                conditionalTextResultService.toDto(conditionalText.getResult())
            )
            .ruleSets(
                conditionalTextRuleSetService.toDto(conditionalText.getRuleSets())
            )
            .build();
    }

    @Override
    public MessageConditionalText fromDto(MessageConditionalTextDto conditionalTextDto, MessageConditionalText conditionalText) {
        return fromDto(conditionalTextDto, conditionalText, false, false);
    }

    @Override
    public MessageConditionalText fromDto(MessageConditionalTextDto conditionalTextDto, MessageConditionalText conditionalText, boolean includeResult, boolean includeRuleSets) {
        if (conditionalTextDto == null) {
            return conditionalText;
        }

        conditionalText.setUuid(conditionalTextDto.getId());
        conditionalText.setLabel(conditionalTextDto.getLabel());

        if (includeResult) {
            conditionalText.setResult(
                conditionalTextResultService.fromDto(
                    conditionalTextDto.getResult(),
                    MessageConditionalTextResult.builder()
                        .conditionalText(conditionalText)
                        .build()
                )
            );
        }

        if (includeRuleSets) {
            conditionalText.setRuleSets(
                conditionalTextRuleSetService.fromDto(
                    conditionalTextDto.getRuleSets(),
                    conditionalText,
                    includeRuleSets
                )
            );
        }

        return conditionalText;
    }

    @Override
    public List<MessageConditionalText> fromDto(List<MessageConditionalTextDto> conditionalTextDtos, MessageContent content, boolean includeResult, boolean includeRuleSets) {
        if (CollectionUtils.isEmpty(conditionalTextDtos)) {
            return List.of();
        }

        return conditionalTextDtos.stream()
            .map(
                conditionalTextDto ->
                    fromDto(
                        conditionalTextDto,
                        MessageConditionalText.builder()
                            .content(content)
                            .build(),
                        includeResult,
                        includeRuleSets
                    )
            )
            .toList();
    }

}
