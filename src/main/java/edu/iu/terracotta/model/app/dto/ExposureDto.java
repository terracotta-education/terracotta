package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExposureDto {

    private Long exposureId;
    private Long experimentId;
    private String title;
    private List<GroupConditionDto> groupConditionList;

    public ExposureDto() {}

    public Long getExposureId() {return exposureId;}

    public void setExposureId(Long exposureId) {this.exposureId = exposureId;}

    public Long getExperimentId() {return experimentId;}

    public void setExperimentId(Long experimentId) {this.experimentId = experimentId;}

    public String getTitle() {return title;}

    public void setTitle(String title) {this.title = title;}

    public List<GroupConditionDto> getGroupConditionList() {
        return groupConditionList;
    }

    public void setGroupConditionList(List<GroupConditionDto> groupConditionList) {
        this.groupConditionList = groupConditionList;
    }
}
