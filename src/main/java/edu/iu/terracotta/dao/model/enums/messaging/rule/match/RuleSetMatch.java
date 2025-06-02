package edu.iu.terracotta.dao.model.enums.messaging.rule.match;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RuleSetMatch {

    private UUID ruleSetUuid;
    private MessageRuleOperator operator;

    @Builder.Default private boolean match = false;
    @Builder.Default private List<RuleMatch> ruleMatches = new ArrayList<>();

    public void addRuleMatch(RuleMatch ruleMatch) {
        if (ruleMatches == null) {
            ruleMatches = new ArrayList<>();
        }

        ruleMatches.add(ruleMatch);
    }

}
