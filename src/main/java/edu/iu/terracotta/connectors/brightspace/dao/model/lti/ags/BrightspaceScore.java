package edu.iu.terracotta.connectors.brightspace.dao.model.lti.ags;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressWarnings("PMD.LooseCoupling")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrightspaceScore {

    @JsonProperty private String userId;
    @JsonProperty private String scoreMaximum;
    @JsonProperty private String scoreGiven;
    @JsonProperty private String comment;
    @JsonProperty private String activityProgress;
    @JsonProperty private String gradingProgress;
    @JsonProperty private String timestamp;

    private Map<String, Object> lmsSubmissionExtension = new HashMap<>();

}
