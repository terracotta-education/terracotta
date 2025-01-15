package edu.iu.terracotta.dao.model.dto.dashboard.results.overview.grade;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class Grade {

    private double average;
    private double standardDeviation;

}
