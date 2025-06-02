package edu.iu.terracotta.dao.entity.messaging.conditional;

import java.util.ArrayList;
import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "terr_messaging_conditional_text_rule_set")
public class MessageConditionalTextRuleSet extends BaseMessageEntity {

    @Column
    @Enumerated(EnumType.STRING)
    private MessageRuleOperator operator;

    @ManyToOne
    @JoinColumn(name = "conditional_text_id")
    private MessageConditionalText conditionalText;

    @Builder.Default
    @OneToMany(
        mappedBy = "ruleSet",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<MessageConditionalTextRule> rules = new ArrayList<>();

}
