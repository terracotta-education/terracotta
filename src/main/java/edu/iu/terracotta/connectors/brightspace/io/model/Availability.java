package edu.iu.terracotta.connectors.brightspace.io.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class Availability extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "StartDate": <string:UTCDateTime>|null,
        "EndDate": <string:UTCDateTime>|null,
        "StartDateAvailabilityType": <string:AVAILABILITY_T>|null,
        "EndDateAvailabilityType": <string:AVAILABILITY_T>|null
     }
     */

    @JsonProperty("StartDate") private String startDate;
    @JsonProperty("EndDate") private String endDate;
    @JsonProperty("StartDateAvailabilityType") private String startDateAvailabilityType;
    @JsonProperty("EndDateAvailabilityType") private String endDateAvailabilityType;

}
