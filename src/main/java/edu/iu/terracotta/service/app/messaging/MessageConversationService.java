package edu.iu.terracotta.service.app.messaging;

import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.exceptions.messaging.MessageNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageSendConversationException;

public interface MessageConversationService {

    void send(Message message) throws MessageNotMatchingException, MessageSendConversationException, TerracottaConnectorException;

}
