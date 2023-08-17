package edu.iu.terracotta.model.app.dto.dashboard.results.overview;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class Overview {

    protected Long id;
    protected String title;
    protected long submissionCount;
    protected double submissionRate;
    protected double averageGrade;
    protected double standardDeviation;

}
