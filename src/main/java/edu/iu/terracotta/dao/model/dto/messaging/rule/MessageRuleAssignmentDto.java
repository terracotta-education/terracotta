package edu.iu.terracotta.dao.model.dto.messaging.rule;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
public class MessageRuleAssignmentDto {

    private String lmsId;
    private String title;
    private String gradingType;
    private List<MessageRuleComparisonDto> comparisons;

}
