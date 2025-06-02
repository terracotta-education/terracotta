package edu.iu.terracotta.service.app.messaging.impl.rules;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleComparisonDto;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleAssignmentGradingType;
import edu.iu.terracotta.service.app.messaging.MessageRuleAssignmentService;

@Service
public class MessageRuleAssignmentServiceImpl implements MessageRuleAssignmentService {

    @Override
    public MessageRuleAssignmentDto toDto(LmsAssignment lmsAssignment) {
        MessageRuleAssignmentGradingType gradingType = EnumUtils.getEnum(MessageRuleAssignmentGradingType.class, lmsAssignment.getGradingType(), MessageRuleAssignmentGradingType.pass_fail);

        return MessageRuleAssignmentDto.builder()
            .comparisons(
                gradingType.getAllowedComparisons().stream()
                    .map(comparison ->
                        MessageRuleComparisonDto.builder()
                            .id(comparison)
                            .label(comparison.getLabel())
                            .requiresValue(comparison.isRequiresValue())
                            .build()
                    )
                    .toList()
            )
            .lmsId(lmsAssignment.getId())
            .gradingType(lmsAssignment.getGradingType())
            .title(lmsAssignment.getName())
            .build();
    }

    @Override
    public List<MessageRuleAssignmentDto> toDto(List<LmsAssignment> lmsAssignments) {
        return CollectionUtils.emptyIfNull(lmsAssignments).stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public String fromDto(MessageRuleAssignmentDto recipientRuleAssignmentDto) {
        return recipientRuleAssignmentDto.getLmsId();
    }

}
