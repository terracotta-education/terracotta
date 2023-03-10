package edu.iu.terracotta.model.app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutcomeScoreDto {

    private Long outcomeScoreId;
    private Long outcomeId;
    private Long participantId;
    private Float scoreNumeric;

}
