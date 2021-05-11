package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.sql.Timestamp;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentDto {

    private Long experimentId;
    private Long platformDeploymentId;
    private Long contextId;
    private String title;
    private String description;
    private String exposureType;
    private String participationType;
    private String distributionType;
    private Timestamp started;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Timestamp createdAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Timestamp updatedAt;
    //TODO, uncomment this when we have conditions and add getter, setters and test it
    //@JsonInclude(JsonInclude.Include.NON_NULL)
    //private List<ConditionDTO> conditions;


    public ExperimentDto() {
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public Long getContextId() {
        return contextId;
    }

    public void setContextId(Long contextId) {
        this.contextId = contextId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExposureType() {
        return exposureType;
    }

    public void setExposureType(String exposureType) {
        this.exposureType = exposureType;
    }

    public String getParticipationType() {
        return participationType;
    }

    public void setParticipationType(String participationType) {
        this.participationType = participationType;
    }

    public String getDistributionType() {
        return distributionType;
    }

    public void setDistributionType(String distributionType) {
        this.distributionType = distributionType;
    }

    public Timestamp getStarted() {
        return started;
    }

    public void setStarted(Timestamp started) {
        this.started = started;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getPlatformDeploymentId() {
        return platformDeploymentId;
    }

    public void setPlatformDeploymentId(Long platformDeploymentId) {
        this.platformDeploymentId = platformDeploymentId;
    }
}
