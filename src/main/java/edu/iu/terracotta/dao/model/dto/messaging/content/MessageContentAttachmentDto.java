package edu.iu.terracotta.dao.model.dto.messaging.content;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageContentAttachmentDto {

    private UUID id;
    private String lmsId;
    private String displayName;
    private String filename;
    private long size;
    private String url;

}
