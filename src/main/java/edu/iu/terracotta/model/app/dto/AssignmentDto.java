package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentDto {

    private Long assignmentId;
    private Long exposureId;
    private String title;
    private String lmsAssignmentId;
    private Integer assignmentOrder;

    public AssignmentDto() {}

    public Long getAssignmentId() { return assignmentId; }

    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public Long getExposureId() { return exposureId; }

    public void setExposureId(Long exposureId) { this.exposureId = exposureId; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getLmsAssignmentId() { return lmsAssignmentId; }

    public void setLmsAssignmentId(String lmsAssignmentId) { this.lmsAssignmentId = lmsAssignmentId; }

    public Integer getAssignmentOrder() { return assignmentOrder; }

    public void setAssignmentOrder(Integer assignmentOrder) { this.assignmentOrder = assignmentOrder; }
}
