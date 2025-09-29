package edu.iu.terracotta.service.app.messaging;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;

public interface MessageSendService {

    List<LtiUserEntity> getRecipients(Message message) throws ApiException, TerracottaConnectorException, IOException;
    String parseMessageBody(Message message, LtiUserEntity recipient, Map<String, List<LmsSubmission>> lmsSubmissions) throws MessageBodyParseException;
    String parseMessageBody(Message message, LtiUserEntity recipient, Map<String, List<LmsSubmission>> lmsSubmissions, boolean isPreview) throws MessageBodyParseException;

}
