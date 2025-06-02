package edu.iu.terracotta.service.app.messaging.impl.conditional;

import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextResult;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextResultDto;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextResultService;

@Service
public class MessageConditionalTextResultServiceImpl implements MessageConditionalTextResultService {

    @Override
    public void create(MessageConditionalTextResultDto conditionalTextResultDto, MessageConditionalText conditionalText) {
        conditionalText.setResult(
            fromDto(
                conditionalTextResultDto,
                MessageConditionalTextResult.builder()
                    .conditionalText(conditionalText)
                    .build()
            )
        );
    }

    @Override
    public void update(MessageConditionalTextResultDto conditionalTextResultDto, MessageConditionalText conditionalText) {
        conditionalText.setResult(
            fromDto(conditionalTextResultDto, conditionalText.getResult())
        );
    }

    @Override
    public void duplicate(MessageConditionalTextResult conditionalTextResult, MessageConditionalText conditionalText) {
        // TODO handle piped text placeholders
        conditionalText.setResult(
            MessageConditionalTextResult.builder()
                .conditionalText(conditionalText)
                .html(conditionalTextResult.getHtml())
                .build()
        );
    }

    @Override
    public MessageConditionalTextResultDto toDto(MessageConditionalTextResult conditionalTextResult) {
        return MessageConditionalTextResultDto.builder()
            .conditionalTextId(conditionalTextResult.getConditionalText().getUuid())
            .html(conditionalTextResult.getHtml())
            .id(conditionalTextResult.getUuid())
            .build();
    }

    @Override
    public MessageConditionalTextResult fromDto(MessageConditionalTextResultDto conditionalTextResultDto, MessageConditionalTextResult conditionalTextResult) {
        if (conditionalTextResultDto == null) {
            return conditionalTextResult;
        }

        conditionalTextResult.setConditionalText(conditionalTextResult.getConditionalText());
        conditionalTextResult.setUuid(conditionalTextResultDto.getId());
        conditionalTextResult.setHtml(conditionalTextResultDto.getHtml());

        return conditionalTextResult;
    }

}
