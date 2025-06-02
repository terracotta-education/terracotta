package edu.iu.terracotta.dao.model.dto.messaging.preview;

import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextDto;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextDto;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleSetDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessagePreviewDto {

    private UUID id;
    private String body;
    private List<MessageRecipientRuleSetDto> ruleSets;
    private List<MessageConditionalTextDto> conditionalTexts;
    private MessagePipedTextDto pipedText;

}
