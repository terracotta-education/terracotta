package edu.iu.terracotta.dao.entity.messaging.log;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.model.enums.messaging.MessageProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "terr_messaging_message_log")
public class MessageLog extends BaseMessageEntity {

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "message_id",
        nullable = false
    )
    private Message message;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "lti_user_user_id",
        nullable = false
    )
    private LtiUserEntity recipient;

    @Column
    @Enumerated(EnumType.STRING)
    private MessageProcessingStatus status;

    @Column private String body;
    @Column private String errorMessage;
    @Column private String remoteId;

    @Transient
    public Condition getCondition() {
        return message.getCondition();
    }

    @Transient
    public long getConditionId() {
        return message.getConditionId();
    }

    @Transient
    public String getConditionName() {
        return getCondition().getName();
    }

    @Transient
    public MessageConfiguration getMessageConfiguration() {
        return message.getConfiguration();
    }

    @Transient
    public String getMessageSubject() {
        return getMessageConfiguration().getSubject();
    }

    @Transient
    public MessageContent getMessageContent() {
        return message.getContent();
    }

    @Transient
    public String getMessageBody() {
        return getMessageContent().getHtml();
    }

}
