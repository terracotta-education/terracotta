package edu.iu.terracotta.service.app.messaging;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextFileUploadException;

public interface MessageService {

    void create(MessageContainer container, long exposureId, boolean single);
    void update(MessageDto messageDto, long exposureId, MessageContainer container, Message message);
    MessageDto put(MessageDto messageDto, long exposureId, MessageContainer container, Message message);
    void duplicate(List<Message> messages, MessageContainer container) throws MessageBodyParseException;
    void duplicate(Message message, MessageContainer container) throws MessageBodyParseException;
    void delete(MessageContainer container);
    List<MessageDto> toDto(List<Message> messages);
    MessageDto toDto(Message message);
    List<MessageRuleAssignmentDto> getAssignments(SecuredInfo securedInfo) throws ApiException, TerracottaConnectorException, DataServiceException;
    void updatePlaceholders(Message message, boolean save);
    MessageDto processPipedTextCsvFile(Message message, MultipartFile file) throws MessagePipedTextFileUploadException;

}
