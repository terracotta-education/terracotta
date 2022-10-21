package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExposureDto {

    private Long exposureId;
    private Long experimentId;
    private String title;
    private List<GroupConditionDto> groupConditionList;

}
