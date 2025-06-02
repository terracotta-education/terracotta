package edu.iu.terracotta.connectors.generic.dao.model.lms.options;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LmsGetSingleConversationOptions {

    private String conversationId;
    private boolean autoMarkAsRead;

}
