package edu.iu.terracotta.dao.entity.messaging.container;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;
import edu.iu.terracotta.dao.model.enums.messaging.MessageContainerUpdatedFields;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
@SuppressWarnings({"PMD.LooseCoupling"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "terr_messaging_container_configuration")
public class MessageContainerConfiguration extends BaseMessageEntity {

    private int containerOrder;
    private Timestamp sendAt;
    private Integer sendAtTimezoneOffset; // minutes offset from UTC
    private String title;
    private boolean toConsentedOnly;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "container_id")
    private MessageContainer container;

    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private MessageType type = MessageType.NONE;

    @Builder.Default
    @OneToMany(
        mappedBy = "containerConfiguration",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<MessageEmailReplyTo> replyTo = new ArrayList<>();

    @Transient
    @Builder.Default
    private Map<MessageContainerUpdatedFields, Boolean> modifiedFields = new HashMap<> (
        Map.of(
            MessageContainerUpdatedFields.REPLY_TO, false,
            MessageContainerUpdatedFields.SEND_AT, false,
            MessageContainerUpdatedFields.SEND_AT_TIMEZONE_OFFSET, false,
            MessageContainerUpdatedFields.TO_CONSENTED_ONLY, false,
            MessageContainerUpdatedFields.TYPE, false
        )
    );

}
