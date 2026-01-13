package edu.iu.terracotta.dao.entity.messaging.attachment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "terr_messaging_content_attachment")
public class MessageContentAttachment extends BaseMessageEntity {

    @ManyToOne
    @JoinColumn(name = "content_id")
    private MessageContent content;

    @Column private String lmsId;
    @Column private String displayName;
    @Column private String filename;
    @Column private long size;
    @Column private String url;

}
