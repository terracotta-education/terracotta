package edu.iu.terracotta.dao.model.dto.distribute;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyCandidateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CopyCandidateDto {

    private UUID id;
    private Long sourceExperimentId;
    private String experimentTitle;
    private String sourceCourseTitle;
    private int conditionCount;
    private int assignmentCount;
    private ExperimentCopyCandidateStatus status;

}
