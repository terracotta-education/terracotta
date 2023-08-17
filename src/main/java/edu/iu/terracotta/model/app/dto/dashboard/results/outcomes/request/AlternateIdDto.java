package edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlternateIdDto {

    private String id;
    private List<Long> exposures;

}
