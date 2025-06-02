package edu.iu.terracotta.dao.entity.messaging.message;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import edu.iu.terracotta.dao.model.enums.messaging.MessageRecipientMatchType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
@Table(name = "terr_messaging_message_configuration")
public class MessageConfiguration extends BaseMessageEntity {

    @Column private String subject;
    @Column private Timestamp sendAt;
    @Column private Integer sendAtTimezoneOffset; // minutes offset from UTC

    @Column
    @Builder.Default
    private boolean enabled = true;

    @Column
    @Builder.Default
    private boolean toConsentedOnly = false;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "message_id")
    private Message message;

    @Column
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private MessageType type = MessageType.NONE;

    @Column
    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @Column
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
