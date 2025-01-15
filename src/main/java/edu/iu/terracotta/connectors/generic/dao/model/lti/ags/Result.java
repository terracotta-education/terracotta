package edu.iu.terracotta.connectors.generic.dao.model.lti.ags;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result {

    @JsonProperty private String id;
    @JsonProperty private String userId;
    @JsonProperty private String resultMaximum;
    @JsonProperty private String resultScore;
    @JsonProperty private String comment;
    @JsonProperty private String scoreOf;

}
