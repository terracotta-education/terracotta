package edu.iu.terracotta.dao.entity.messaging.conditional;

import java.util.ArrayList;
import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "terr_messaging_conditional_text")
public class MessageConditionalText extends BaseMessageEntity {

    @ManyToOne
    @JoinColumn(name = "content_id")
    private MessageContent content;

    @OneToOne(
        mappedBy = "conditionalText",
        cascade = CascadeType.ALL
    )
    private MessageConditionalTextResult result;

    @Builder.Default
    @OneToMany(
        mappedBy = "conditionalText",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<MessageConditionalTextRuleSet> ruleSets = new ArrayList<>();

    @Column private String label;

}
