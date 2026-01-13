package edu.iu.terracotta.dao.entity.messaging.container;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
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
@Table(name = "terr_messaging_container")
public class MessageContainer extends BaseMessageEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "exposure_id")
    private Exposure exposure;

    @OneToOne(
        mappedBy = "container",
        cascade = CascadeType.ALL
    )
    private MessageContainerConfiguration configuration;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private LtiUserEntity owner;

    @Builder.Default
    @OneToMany(
        mappedBy = "container",
        cascade = CascadeType.ALL
    )
    private List<Message> messages = new ArrayList<>();

    @Transient
    public long getExposureId() {
        return exposure.getExposureId();
    }

    @Transient
    public MessageStatus getStatus() {
        return configuration.getStatus();
    }

    @Transient
    public int getOrder() {
        return configuration.getContainerOrder();
    }

    @Transient
    public List<MessageEmailReplyTo> getReplyTo() {
        return configuration.getReplyTo();
    }

    @Transient
    public Timestamp getSendAt() {
        return configuration.getSendAt();
    }

    @Transient
    public Integer getSendAtTimezoneOffset() {
        if (configuration.getSendAtTimezoneOffset() == null) {
            return 0;
        }

        return configuration.getSendAtTimezoneOffset();
    }

    @Transient
    public boolean isSingleVersion() {
        return messages.size() == 1;
    }

}
