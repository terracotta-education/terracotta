package edu.iu.terracotta.connectors.generic.dao.model.lms.options;

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
public class LmsGetSingleConversationOptions {

    private String conversationId;
    private boolean autoMarkAsRead;

}
