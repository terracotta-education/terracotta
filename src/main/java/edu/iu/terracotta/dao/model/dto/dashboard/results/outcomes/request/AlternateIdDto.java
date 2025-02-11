package edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlternateIdDto {

    private String id;
    private List<Long> exposures;

}
