package edu.iu.terracotta.dao.model.dto.messaging.piped;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessagePipedTextItemValueDto {

    private UUID id;
    private UUID pipedTextItemId;
    private long userId;
    private String value;

}
