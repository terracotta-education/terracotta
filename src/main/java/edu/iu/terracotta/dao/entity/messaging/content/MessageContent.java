package edu.iu.terracotta.dao.entity.messaging.content;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.entity.messaging.attachment.MessageContentAttachment;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
@Table(name = "terr_messaging_content")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageContent extends BaseMessageEntity {

    @OneToOne
    @JoinColumn(name = "message_id")
    private Message message;

    @Lob
    @Column
    private String html;

    @Builder.Default
    @OneToMany(
        mappedBy = "content",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<MessageContentAttachment> attachments = new ArrayList<>();

    @Builder.Default
    @OneToMany(
        mappedBy = "content",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<MessageConditionalText> conditionalTexts = new ArrayList<>();

    @OneToOne(
        mappedBy = "content",
        cascade = CascadeType.ALL
    )
    private MessagePipedText pipedText;

}
