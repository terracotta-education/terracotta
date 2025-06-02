package edu.iu.terracotta.service.app.messaging;

import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.model.dto.messaging.preview.MessagePreviewDto;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;

public interface MessagePreviewService {

    MessagePreviewDto preview(MessagePreviewDto previewDto, Message message) throws ParticipantNotMatchingException, MessageBodyParseException;

}
