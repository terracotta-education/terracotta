package edu.iu.terracotta.dao.model.dto.messaging.message;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentDto;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleSetDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageDto {

    private UUID id;
    private UUID containerId;
    private MessageConfigurationDto configuration;
    private MessageContentDto content;
    private Timestamp created;
    private long conditionId;
    private String ownerEmail;
    private long exposureGroupConditionId;
    private List<MessageRecipientRuleSetDto> ruleSets;
    private List<String> validationErrors;

}
