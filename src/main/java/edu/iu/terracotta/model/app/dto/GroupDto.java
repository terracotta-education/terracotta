package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupDto {

    private Long groupId;
    private Long experimentId;
    private String name;

    public GroupDto() {}

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getExperimentId() {return experimentId;}

    public void setExperimentId(Long experimentId) {this.experimentId = experimentId;}

}
