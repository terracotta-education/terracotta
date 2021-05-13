package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionDto {

    private Long conditionId;
    private Long experimentId;
    private String name;
    private boolean defaultCondition;
    private float distributionPct;

    public ConditionDto() {}

    public Long getConditionId() { return conditionId; }

    public void setConditionId(Long conditionId) { this.conditionId = conditionId; }

    public Long getExperimentId() { return experimentId; }

    public void setExperimentId(Long experimentId) { this.experimentId = experimentId; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public boolean getDefaultCondition() { return defaultCondition; }

    public void setDefaultCondition(boolean defaultCondition) { this.defaultCondition = defaultCondition; }

    public float getDistributionPct() { return distributionPct; }

    public void setDistributionPct(float pct) { this.distributionPct = pct; }
}
