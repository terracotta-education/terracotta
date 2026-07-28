package edu.iu.terracotta.service.app.messaging;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;

public interface MessageSendService {

    List<LtiUserEntity> getRecipients(Message message) throws ApiException, TerracottaConnectorException, IOException;
    String parseMessageBody(Message message, LtiUserEntity recipient, Map<String, List<LmsSubmission>> lmsSubmissions) throws MessageBodyParseException;
    String parseMessageBody(Message message, LtiUserEntity recipient, Map<String, List<LmsSubmission>> lmsSubmissions, boolean isPreview) throws MessageBodyParseException;
    // caches let callers rendering one message for many recipients reuse conditional/piped-text lookups instead of re-querying per recipient
    String parseMessageBody(Message message, LtiUserEntity recipient, Map<String, List<LmsSubmission>> lmsSubmissions, boolean isPreview,
        Map<String, MessageConditionalText> conditionalTextCache, Map<String, MessagePipedTextItem> pipedTextItemCache) throws MessageBodyParseException;

}
