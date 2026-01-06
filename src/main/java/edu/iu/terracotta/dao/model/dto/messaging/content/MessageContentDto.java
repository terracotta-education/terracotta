package edu.iu.terracotta.dao.model.dto.messaging.content;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextDto;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextDto;

import java.util.List;

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
public class MessageContentDto {

    private UUID id;
    private UUID messageId;
    private String html;
    private List<MessageContentAttachmentDto> attachments;
    private List<MessageConditionalTextDto> conditionalTexts;
    private MessagePipedTextDto pipedText;

}
