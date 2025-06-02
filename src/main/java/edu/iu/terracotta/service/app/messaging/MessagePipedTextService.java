package edu.iu.terracotta.service.app.messaging;

import org.springframework.web.multipart.MultipartFile;

import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextDto;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextFileUploadException;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextValidationException;

public interface MessagePipedTextService {

    void create(MessagePipedTextDto pipedTextDto, MessageContent content);
    void update(MessagePipedTextDto pipedTextDto, MessagePipedText pipedText);
    void upsert(MessagePipedTextDto pipedTextDto, MessageContent content);
    MessagePipedText processPipedTextCsvFile(Message message, MultipartFile file) throws MessagePipedTextFileUploadException;
    void validatePipedTextFile(Message message, MultipartFile file) throws MessagePipedTextValidationException, MessagePipedTextFileUploadException;
    void duplicate(MessagePipedText pipedText, MessageContent content);
    MessagePipedText fromDto(MessagePipedTextDto pipedTextDto, MessagePipedText pipedText);
    MessagePipedText fromDto(MessagePipedTextDto pipedTextDto, MessagePipedText pipedText, boolean includeItems);
    MessagePipedTextDto toDto(MessagePipedText pipedText);


}
