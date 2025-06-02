package edu.iu.terracotta.service.app.messaging;

import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextResult;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextResultDto;

public interface MessageConditionalTextResultService {

    void create(MessageConditionalTextResultDto conditionalTextResultDto, MessageConditionalText conditionalText);
    void update(MessageConditionalTextResultDto conditionalTextResultDto, MessageConditionalText conditionalText);
    void duplicate(MessageConditionalTextResult conditionalTextResult, MessageConditionalText conditionalText);
    MessageConditionalTextResultDto toDto(MessageConditionalTextResult conditionalTextResult);
    MessageConditionalTextResult fromDto(MessageConditionalTextResultDto conditionalTextResultDto, MessageConditionalTextResult conditionalTextResult);

}
