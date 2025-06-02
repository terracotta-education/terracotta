package edu.iu.terracotta.connectors.canvas.service.extended;

import java.io.IOException;
import java.util.Optional;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.ConversationExtended;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.requestOptions.GetSingleConversationOptions;

public interface ConversationReaderExtended extends CanvasReader<ConversationExtended, ConversationReaderExtended> {

    Optional<ConversationExtended> getSingleConversation(GetSingleConversationOptions getSingleConversationOptions) throws IOException;

}
