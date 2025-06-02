package edu.iu.terracotta.connectors.generic.dao.model.lms.options;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LmsCreateConversationOptions {

    private String lmsUserId;
    private List<String> attachmentIds;
    private boolean forceNew;
    private boolean groupConversation;
    private List<String> lmsUserIds; // recipient LMS user IDs
    private String subject;
    private String body;

}
