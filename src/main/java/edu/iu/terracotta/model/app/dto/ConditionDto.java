package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionDto {

    private Long conditionId;
    private Long experimentId;
    private String name;
    private Boolean defaultCondition;
    private float distributionPct;

}
