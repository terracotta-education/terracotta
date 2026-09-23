package edu.iu.terracotta.dao.model.enums.messaging.rule.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;

public class RuleSetMatchTest {

    @Test
    public void testBuilderDefaults() {
        RuleSetMatch ruleSetMatch = RuleSetMatch.builder().build();

        assertFalse(ruleSetMatch.isMatch());
        assertNotNull(ruleSetMatch.getRuleMatches());
        assertTrue(ruleSetMatch.getRuleMatches().isEmpty());
    }

    @Test
    public void testBuilderWithExplicitValues() {
        UUID ruleSetUuid = UUID.randomUUID();

        RuleSetMatch ruleSetMatch = RuleSetMatch.builder()
            .ruleSetUuid(ruleSetUuid)
            .operator(MessageRuleOperator.AND)
            .match(true)
            .build();

        assertEquals(ruleSetUuid, ruleSetMatch.getRuleSetUuid());
        assertEquals(MessageRuleOperator.AND, ruleSetMatch.getOperator());
        assertTrue(ruleSetMatch.isMatch());
    }

    @Test
    public void testAddRuleMatchAppendsToExistingList() {
        RuleSetMatch ruleSetMatch = RuleSetMatch.builder().build();
        RuleMatch ruleMatch = RuleMatch.builder().match(true).build();

        ruleSetMatch.addRuleMatch(ruleMatch);

        assertEquals(1, ruleSetMatch.getRuleMatches().size());
        assertSame(ruleMatch, ruleSetMatch.getRuleMatches().get(0));
    }

    @Test
    public void testAddRuleMatchInitializesListWhenNull() {
        RuleSetMatch ruleSetMatch = RuleSetMatch.builder().ruleMatches(null).build();
        RuleMatch ruleMatch = RuleMatch.builder().build();

        ruleSetMatch.addRuleMatch(ruleMatch);

        assertEquals(1, ruleSetMatch.getRuleMatches().size());
    }

}
