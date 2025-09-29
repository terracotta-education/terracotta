package edu.iu.terracotta.service.app.messaging;

import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentDto;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;

public interface MessageContentService {

    void create(Message message);
    void update(MessageContentDto contentDto, Message message);
    void duplicate(MessageContent content, Message message) throws MessageBodyParseException;
    MessageContentDto toDto(MessageContent content);
    MessageContent fromDto(MessageContentDto contentDto, MessageContent content);
    void updatePlaceholders(MessageContent content, boolean save) throws MessageBodyParseException;
    MessageContentDto updatePlaceholders(MessageContent content, MessageContentDto contentDto) throws MessageBodyParseException;
    String prepareBodyHtmlForExport(String body);

}
