package edu.iu.terracotta.model.app.dto;

public class TreatmentDto {

    private Long treatmentId;
    private AssessmentDto assessmentDto;
    private Long conditionId;
    private Integer treatmentOrder;


    public Long getTreatmentId() { return treatmentId; }

    public void setTreatmentId(Long treatmentId) { this.treatmentId = treatmentId; }

    public AssessmentDto getAssessmentDto() { return assessmentDto; }

    public void setAssessmentDto(AssessmentDto assessmentDto) { this.assessmentDto = assessmentDto; }

    public Long getConditionId() { return conditionId; }

    public void setConditionId(Long conditionId) { this.conditionId = conditionId; }

    public Integer getTreatmentOrder() { return treatmentOrder; }

    public void setTreatmentOrder(Integer treatmentOrder) { this.treatmentOrder = treatmentOrder; }
}
