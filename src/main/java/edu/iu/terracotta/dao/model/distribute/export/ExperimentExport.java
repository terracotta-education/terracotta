package edu.iu.terracotta.dao.model.distribute.export;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.enums.DistributionTypes;
import edu.iu.terracotta.dao.model.enums.ExposureTypes;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
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
public class ExperimentExport {

    private long id;
    private String title;
    private String description;
    private ExposureTypes exposureType;
    private ParticipationTypes participationType;
    private DistributionTypes distributionType;

}
