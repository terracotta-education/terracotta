package edu.iu.terracotta.dao.entity.messaging.piped;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
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
@Table(name = "terr_messaging_piped_text_item_value")
public class MessagePipedTextItemValue extends BaseMessageEntity {

    @Column private String value;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "piped_text_item_id",
        nullable = false
    )
    private MessagePipedTextItem item;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "lti_user_user_id",
        nullable = false
    )
    private LtiUserEntity user;

}
