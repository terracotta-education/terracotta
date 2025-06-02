package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsConversation;
import edu.ksu.canvas.annotation.CanvasObject;
import edu.ksu.canvas.model.Conversation;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@CanvasObject(postKey = "conversation")
public class ConversationExtended extends LmsConversation {

    @Builder.Default private Conversation conversation = new Conversation();

    @Override
    public String getId() {
        if (conversation == null || conversation.getId() == null) {
            return null;
        }

        return String.valueOf(conversation.getId());
    }

    @Override
    public void setId(String id) {
        if (conversation == null) {
            return;
        }

        conversation.setId(Long.valueOf(id));
    }

    @Override
    public LmsConversation from() {
        LmsConversation lmsConversation = LmsConversation.builder().build();
        lmsConversation.setId(getId());
        lmsConversation.setType(getType());

        return lmsConversation;
    }

    public static ConversationExtended of(LmsConversation lmsConversation) {
        if (lmsConversation == null) {
            return ConversationExtended.builder().build();
        }

        ConversationExtended conversationExtended = ConversationExtended.builder().build();
        conversationExtended.setId(lmsConversation.getId());

        return conversationExtended;
    }

}
