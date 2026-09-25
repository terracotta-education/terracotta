package edu.iu.terracotta.dao.model.dto.distribute;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyStatus;
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
public class CopyStatusDto {

    private ExperimentCopyStatus status;
    private String sourceCourseTitle;

    // the experiment imports created by the recreation, so the UI can leave them out of its
    // ordinary per-import alerts and show this one combined message instead
    private List<UUID> importIds;

}
