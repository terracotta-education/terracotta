package edu.iu.terracotta.dao.entity.messaging.message;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "terr_messaging_message")
public class Message extends BaseMessageEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "exposure_group_condition_id")
    private ExposureGroupCondition exposureGroupCondition;

    @OneToOne(
        mappedBy = "message",
        cascade = CascadeType.ALL
    )
    private MessageConfiguration configuration;

    @OneToOne(
        mappedBy = "message",
        cascade = CascadeType.ALL
    )
    private MessageContent content;

    @ManyToOne
    @JoinColumn(
        name = "container_id",
        nullable = false
    )
    private MessageContainer container;

    @Builder.Default
    @OneToMany(
        mappedBy = "message",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<MessageRecipientRuleSet> ruleSets = new ArrayList<>();

    @Transient
    public LtiUserEntity getOwner() {
        return container.getOwner();
    }

    @Transient
    public long getExposureGroupConditionId() {
        return exposureGroupCondition.getExposureGroupConditionId();
    }

    @Transient
    public Condition getCondition() {
        return exposureGroupCondition.getCondition();
    }

    @Transient
    public long getConditionId() {
        return getCondition().getConditionId();
    }

    @Transient
    public long getExposureId() {
        return exposureGroupCondition.getExposure().getExposureId();
    }

    @Transient
    public Experiment getExperiment() {
        return getCondition().getExperiment();
    }

    @Transient
    public long getExperimentId() {
        return getExperiment().getExperimentId();
    }

    @Transient
    public PlatformDeployment getPlatformDeployment() {
        return getExperiment().getPlatformDeployment();
    }

    @Transient
    public MessageStatus getStatus() {
        return configuration.getStatus();
    }

    @Transient
    public boolean isEnabled() {
        return configuration.isEnabled();
    }

    @Transient
    public MessageType getType() {
        return configuration.getType();
    }

    @Transient
    public Timestamp getSendAt() {
        return configuration.getSendAt();
    }

    @Transient
    public Integer getSendAtTimestampOffset() {
        if (configuration.getSendAtTimezoneOffset() == null) {
            return 0;
        }

        return configuration.getSendAtTimezoneOffset();
    }

    @Transient
    public String getSubject() {
        return configuration.getSubject();
    }

    @Transient
    public boolean isToConsentedOnly() {
        return configuration.isToConsentedOnly();
    }

    @Transient
    public List<MessageEmailReplyTo> getReplyTo() {
        return configuration.getReplyTo();
    }

    @Transient
    public MessagePipedText getPipedText() {
        return content.getPipedText();
    }

    @Transient
    public boolean isDefaultMessage() {
        return BooleanUtils.isTrue(getCondition().getDefaultCondition());
    }

}
