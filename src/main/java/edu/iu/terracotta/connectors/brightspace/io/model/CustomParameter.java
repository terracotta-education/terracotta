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
public class CustomParameter extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "Name": <string>,
        "Value": <string>
     }
     */

    @JsonProperty("Name") private String name;
    @JsonProperty("Value") private String value;

    @Getter
    @AllArgsConstructor
    public enum Keys {

        ASSIGNMENT_ID("assignmentId"),
        EXPERIMENT_ID("experimentId"),
        BRIGHTSPACE_ASSIGNMENT_ID("brightspace_assignment_id");

        private final String key;

        public String key() {
            return key;
        }

    }

}
