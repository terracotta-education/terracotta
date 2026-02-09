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
public class UserGradeValuePaged extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "Next": <string:APIURL>|null,
        "Objects": [
            { <composite:first_item_in_this_set> },
            { <composite:second_item_in_this_set> },
            { <composite:nth_item_in_this_set> },
             ...
        ]
     }
     */

    @JsonProperty("Next") private String next;
    @JsonProperty("Objects") private List<UserGradeValue> objects;

}
