package edu.iu.terracotta.dao.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExposureDto {

    private Long exposureId;
    private Long experimentId;
    private String title;
    private List<GroupConditionDto> groupConditionList;

}
