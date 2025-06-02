package edu.iu.terracotta.dao.model.distribute.export.messages;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
public class MessageRecipientRuleSetExport {

    private long id;
    private long messageId;
    private MessageRuleOperator operator;
    private List<MessageRecipientRuleExport> rules;

}
