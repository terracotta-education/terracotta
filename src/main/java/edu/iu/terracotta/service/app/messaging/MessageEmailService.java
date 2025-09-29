package edu.iu.terracotta.service.app.messaging;

import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.dto.messaging.send.MessageSendTestDto;
import edu.iu.terracotta.exceptions.messaging.MessageNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageSendEmailException;

public interface MessageEmailService {

    void send(Message message) throws MessageNotMatchingException, MessageSendEmailException, TerracottaConnectorException;
    void sendTest(Message message, MessageSendTestDto messageSendTestDto) throws MessageSendEmailException, TerracottaConnectorException;

}
