package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class OutcomeDto {

    private Long outcomeId;
    private Long exposureId;
    private String title;
    private String lmsType;
    private String lmsOutcomeId;
    private Float maxPoints;
    private Boolean external;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<OutcomeScoreDto> outcomeScoreDtoList;

    public Long getOutcomeId() { return outcomeId; }

    public void setOutcomeId(Long outcomeId) { this.outcomeId = outcomeId; }

    public Long getExposureId() { return exposureId; }

    public void setExposureId(Long exposureId) { this.exposureId = exposureId; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getLmsType() { return lmsType; }

    public void setLmsType(String lmsType) { this.lmsType = lmsType; }

    public String getLmsOutcomeId() { return lmsOutcomeId; }

    public void setLmsOutcomeId(String lmsOutcomeId) { this.lmsOutcomeId = lmsOutcomeId; }

    public Float getMaxPoints() { return maxPoints; }

    public void setMaxPoints(Float maxPoints) { this.maxPoints = maxPoints; }

    public Boolean getExternal() { return external; }

    public void setExternal(Boolean external) { this.external = external; }

    public List<OutcomeScoreDto> getOutcomeScoreDtoList() { return outcomeScoreDtoList; }

    public void setOutcomeScoreDtoList(List<OutcomeScoreDto> outcomeScoreDtoList) { this.outcomeScoreDtoList = outcomeScoreDtoList; }
}
