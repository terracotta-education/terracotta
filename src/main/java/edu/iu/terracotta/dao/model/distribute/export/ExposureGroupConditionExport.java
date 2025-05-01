package edu.iu.terracotta.dao.model.distribute.export;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
public class ExposureGroupConditionExport {

    private long id;
    private long conditionId;
    private long groupId;
    private long exposureId;

}
