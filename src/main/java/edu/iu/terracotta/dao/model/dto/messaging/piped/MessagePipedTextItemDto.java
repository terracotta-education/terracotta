package edu.iu.terracotta.dao.model.dto.messaging.piped;

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
public class MessagePipedTextItemDto {

    private UUID id;
    private UUID pipedTextId;
    private String key;
    private List<MessagePipedTextItemValueDto> values;

}
