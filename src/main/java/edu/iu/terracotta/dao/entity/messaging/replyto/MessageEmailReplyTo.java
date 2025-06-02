package edu.iu.terracotta.dao.entity.messaging.replyto;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
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
@Table(name = "terr_messaging_email_reply_to")
public class MessageEmailReplyTo extends BaseMessageEntity {

    @ManyToOne
    @JoinColumn(name = "container_configuration_id")
    private MessageContainerConfiguration containerConfiguration;

    @ManyToOne
    @JoinColumn(name = "message_configuration_id")
    private MessageConfiguration messageConfiguration;

    @Column private String email;

}
