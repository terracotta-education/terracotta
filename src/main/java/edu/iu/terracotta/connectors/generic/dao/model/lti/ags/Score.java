package edu.iu.terracotta.connectors.generic.dao.model.lti.ags;

import java.util.HashMap;
import java.util.Map;

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
@SuppressWarnings({"PMD.LooseCoupling"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Score {

    @JsonProperty private String userId;
    @JsonProperty private Float scoreMaximum;
    @JsonProperty private Float scoreGiven;
    @JsonProperty private String comment;
    @JsonProperty private String activityProgress;
    @JsonProperty private String gradingProgress;
    @JsonProperty private String timestamp;

    @JsonProperty
    @Builder.Default
    private Map<String, Object> lmsSubmissionExtension = new HashMap<>();

}
