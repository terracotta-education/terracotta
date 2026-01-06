package edu.iu.terracotta.dao.model.dto.messaging.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleComparison;
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
public class MessageRuleComparisonDto {

    private MessageRuleComparison id;
    private String label;
    private boolean requiresValue;

}
