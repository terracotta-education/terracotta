package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OutcomeDto {

    private Long outcomeId;
    private Long exposureId;
    private String title;
    private String lmsType;
    private String lmsOutcomeId;
    private Float maxPoints;
    private Boolean external;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<OutcomeScoreDto> outcomeScoreDtoList;

}
