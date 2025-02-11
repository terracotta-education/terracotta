package edu.iu.terracotta.dao.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TreatmentDto {

    private Long treatmentId;
    private AssessmentDto assessmentDto;
    private AssignmentDto assignmentDto;
    private Long conditionId;
    private Long assignmentId;

}
