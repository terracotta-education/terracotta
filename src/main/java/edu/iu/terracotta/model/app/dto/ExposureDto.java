package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExposureDto {

    private Long exposureId;
    private Long experimentId;
    private String title;

    public ExposureDto() {}

    public Long getExposureId() {return exposureId;}

    public void setExposureId(Long exposureId) {this.exposureId = exposureId;}

    public Long getExperimentId() {return experimentId;}

    public void setExperimentId(Long experimentId) {this.experimentId = experimentId;}

    public String getTitle() {return title;}

    public void setTitle(String title) {this.title = title;}
}
