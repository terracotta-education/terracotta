package edu.iu.terracotta.dao.model.dto.preview;

import java.util.UUID;

import edu.iu.terracotta.dao.model.dto.SubmissionDto;
import edu.iu.terracotta.dao.model.dto.TreatmentDto;
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
public class TreatmentPreviewDto {

    private UUID id;
    private TreatmentDto treatment;
    private SubmissionDto submission;

}
