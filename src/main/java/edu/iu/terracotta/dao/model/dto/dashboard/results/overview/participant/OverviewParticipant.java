package edu.iu.terracotta.dao.model.dto.dashboard.results.overview.participant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverviewParticipant {

    private long assignmentCount;
    private long classEnrollment;
    private double consentRate;
    private long count;

}
