package edu.iu.terracotta.connectors.brightspace.io.model;

import java.io.Serializable;
import java.util.List;

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
public class Assessment extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "ScoreDenominator": <number:decimal>|null,
        "Rubrics": [  // Array of Rubric blocks
             { <composite:Rubric.Rubric> },
             { <composite:Rubric.Rubric> }, ...
        ]
     }
     */

    @JsonProperty("ScoreDenominator") private Float scoreDenominator;
    @JsonProperty("Rubrics") private List<Object> rubrics;

}
