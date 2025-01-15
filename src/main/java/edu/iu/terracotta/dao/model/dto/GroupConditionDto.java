package edu.iu.terracotta.dao.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupConditionDto {

    private Long groupId;
    private String groupName;
    private Long conditionId;
    private String conditionName;

}
