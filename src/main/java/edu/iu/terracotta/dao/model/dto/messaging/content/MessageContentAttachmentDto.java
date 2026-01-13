package edu.iu.terracotta.dao.model.dto.messaging.content;

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
public class MessageContentAttachmentDto {

    private UUID id;
    private String lmsId;
    private String displayName;
    private String filename;
    private long size;
    private String url;

}
