package edu.iu.terracotta.model.app.dto.dashboard.results.outcomes;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class Outcomes {

    protected double mean;
    protected long number;
    protected List<Double> scores;
    protected double standardDeviation;
    protected String title;

}
