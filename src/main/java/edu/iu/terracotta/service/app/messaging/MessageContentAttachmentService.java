package edu.iu.terracotta.service.app.messaging;

import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.dao.entity.messaging.attachment.MessageContentAttachment;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentAttachmentDto;

public interface MessageContentAttachmentService {

    List<MessageContentAttachmentDto> get(MessageContent content);
    void update(List<MessageContentAttachmentDto> contentAttachmentDtos, MessageContent content);
    void duplicate(List<MessageContentAttachment> contentAttachments, MessageContent content);
    List<MessageContentAttachmentDto> toDto(List<MessageContentAttachment> contentAttachments);
    MessageContentAttachmentDto toDto(MessageContentAttachment contentAttachment);
    List<MessageContentAttachment> fromDto(List<MessageContentAttachmentDto> contentAttachmentDtos, MessageContent content);
    Optional<MessageContentAttachment> fromDto(MessageContentAttachmentDto contentAttachmentDto, MessageContent content);

}
