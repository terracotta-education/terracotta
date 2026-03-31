package edu.iu.terracotta.dao.entity.messaging.message;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import edu.iu.terracotta.dao.model.enums.messaging.MessageRecipientMatchType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "terr_messaging_message_configuration")
public class MessageConfiguration extends BaseMessageEntity {

    private String subject;
    private Timestamp sendAt;
    private Integer sendAtTimezoneOffset; // minutes offset from UTC

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private boolean toConsentedOnly = false;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "message_id")
    private Message message;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private MessageType type = MessageType.NONE;

    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @Enumerated(EnumType.STRING)
    private MessageRecipientMatchType recipientMatchType;

    @Builder.Default
    @OneToMany(
        mappedBy = "messageConfiguration",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<MessageEmailReplyTo> replyTo = new ArrayList<>();

}
