package edu.iu.terracotta.connectors.generic.dao.model.lti.ags;

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
public class Result {

    @JsonProperty private String id;
    @JsonProperty private String userId;
    @JsonProperty private String resultMaximum;
    @JsonProperty private String resultScore;
    @JsonProperty private String comment;
    @JsonProperty private String scoreOf;

}
