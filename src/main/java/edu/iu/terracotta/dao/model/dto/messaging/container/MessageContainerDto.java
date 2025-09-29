package edu.iu.terracotta.dao.model.dto.messaging.container;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.dto.messaging.message.MessageDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageContainerDto {

    public static final String MY_FILES_URL = "%s/files/folder/users_%s/conversation attachments";

    private UUID id;
    private long exposureId;
    private long ownerId;
    private List<MessageDto> messages;
    private MessageContainerConfigurationDto configuration;
    private String ownerEmail;
    private List<MessageRuleAssignmentDto> assignments;
    private String myFilesUrl;

}
