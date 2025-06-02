package edu.iu.terracotta.service.app.messaging;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleComparisonDto;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleComparison;

public interface MessageRuleComparisonService {

    MessageRuleComparisonDto toDto(MessageRuleComparison recipientRuleComparison);
    MessageRuleComparison fromDto(MessageRuleComparisonDto recipientRuleComparisonDto);
    Map<String, List<LmsSubmission>> getLmsSubmissions(Message message) throws ApiException, TerracottaConnectorException, IOException;
    boolean conditionalTextRuleSetsMatch(MessageConditionalText conditionalText, Map<String, List<LmsSubmission>> lmsSubmissions);
    boolean recipientRuleSetsMatch(List<MessageRecipientRuleSet> ruleSets, Map<String, List<LmsSubmission>> lmsSubmissions);

}
