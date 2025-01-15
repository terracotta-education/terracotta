package edu.iu.terracotta.dao.model.dto.dashboard.results.overview.participant;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OverviewParticipant {

    private long assignmentCount;
    private long classEnrollment;
    private double consentRate;
    private long count;

}
