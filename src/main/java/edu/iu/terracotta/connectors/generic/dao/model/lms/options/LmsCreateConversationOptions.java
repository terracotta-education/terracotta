package edu.iu.terracotta.connectors.generic.dao.model.lms.options;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LmsCreateConversationOptions {

    private String lmsUserId;
    private List<String> attachmentIds;
    private boolean forceNew;
    private boolean groupConversation;
    private List<String> lmsUserIds; // recipient LMS user IDs
    private String subject;
    private String body;

}
