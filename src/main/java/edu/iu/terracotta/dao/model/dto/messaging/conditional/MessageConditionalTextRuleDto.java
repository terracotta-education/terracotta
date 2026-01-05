package edu.iu.terracotta.dao.model.dto.messaging.conditional;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleComparisonDto;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageConditionalTextRuleDto {

    private UUID id;
    private UUID ruleSetId;
    private String lmsAssignmentId;
    private MessageRuleAssignmentDto assignment;
    private MessageRuleComparisonDto comparison;
    private String value;
    private MessageRuleOperator operator;

}
