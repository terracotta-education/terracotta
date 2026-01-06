package edu.iu.terracotta.dao.model.dto.messaging.conditional;

import java.util.List;
import java.util.UUID;

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
public class MessageConditionalTextDto {

    private UUID id;
    private UUID contentId;
    private String label;
    private MessageConditionalTextResultDto result;
    private List<MessageConditionalTextRuleSetDto> ruleSets;
    private boolean isNew;

}
