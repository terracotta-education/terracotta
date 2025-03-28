package edu.iu.terracotta.dao.model.dto.dashboard.results.overview.assignment.treatment;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class OverviewTreatments {

    private List<OverviewTreatment> rows;

}
