package edu.iu.terracotta.dao.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
