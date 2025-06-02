package edu.iu.terracotta.connectors.canvas.service.extended;

import java.io.IOException;
import java.util.List;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.ConversationExtended;
import edu.ksu.canvas.interfaces.CanvasWriter;
import edu.ksu.canvas.requestOptions.CreateConversationOptions;

public interface ConversationWriterExtended extends CanvasWriter<ConversationExtended, ConversationWriterExtended> {

    List<ConversationExtended> createConversation(CreateConversationOptions createonversationOptions) throws IOException;

}
