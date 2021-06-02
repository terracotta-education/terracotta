package edu.iu.terracotta.model.app.dto;

public class TreatmentDto {

    private Long treatmentId;
    private AssessmentDto assessmentDto;
    private Long conditionId;
    private Long assignmentId;


    public Long getTreatmentId() { return treatmentId; }

    public void setTreatmentId(Long treatmentId) { this.treatmentId = treatmentId; }

    public AssessmentDto getAssessmentDto() { return assessmentDto; }

    public void setAssessmentDto(AssessmentDto assessmentDto) { this.assessmentDto = assessmentDto; }

    public Long getConditionId() { return conditionId; }

    public void setConditionId(Long conditionId) { this.conditionId = conditionId; }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }
}
