package edu.iu.terracotta.dao.model.dto.messaging.preview;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextDto;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextDto;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleSetDto;
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
public class MessagePreviewDto {

    private UUID id;
    private String body;
    private List<MessageRecipientRuleSetDto> ruleSets;
    private List<MessageConditionalTextDto> conditionalTexts;
    private MessagePipedTextDto pipedText;

}
