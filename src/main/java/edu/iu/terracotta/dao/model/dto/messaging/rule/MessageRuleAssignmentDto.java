package edu.iu.terracotta.dao.model.dto.messaging.rule;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageRuleAssignmentDto {

    private String lmsId;
    private String title;
    private String gradingType;
    private List<MessageRuleComparisonDto> comparisons;

}
