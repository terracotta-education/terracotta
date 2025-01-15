package edu.iu.terracotta.connectors.generic.dao.model.lti.ags;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Score {

    @JsonProperty private String userId;
    @JsonProperty private Float scoreMaximum;
    @JsonProperty private Float scoreGiven;
    @JsonProperty private String comment;
    @JsonProperty private String activityProgress;
    @JsonProperty private String gradingProgress;
    @JsonProperty private String timestamp;
    @JsonProperty private Map<String, Object> lmsSubmissionExtension = new HashMap<>();

}
