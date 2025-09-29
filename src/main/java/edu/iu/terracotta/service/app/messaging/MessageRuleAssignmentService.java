package edu.iu.terracotta.service.app.messaging;

import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;

public interface MessageRuleAssignmentService {

    MessageRuleAssignmentDto toDto(LmsAssignment lmsAssignment);
    List<MessageRuleAssignmentDto> toDto(List<LmsAssignment> lmsAssignments);
    String fromDto(MessageRuleAssignmentDto ruleAssignmentDto);

}
