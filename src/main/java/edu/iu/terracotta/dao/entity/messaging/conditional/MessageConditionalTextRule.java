package edu.iu.terracotta.dao.entity.messaging.conditional;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleComparison;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "terr_messaging_conditional_text_rule")
public class MessageConditionalTextRule extends BaseMessageEntity {

    @ManyToOne
    @JoinColumn(
        name = "rule_set_id",
        nullable = false
    )
    private MessageConditionalTextRuleSet ruleSet;

    @Column
    @Enumerated(EnumType.STRING)
    private MessageRuleOperator operator;

    @Column
    @Enumerated(EnumType.STRING)
    private MessageRuleComparison comparison;

    @Column private String lmsAssignmentId;
    @Column private String value;

}
