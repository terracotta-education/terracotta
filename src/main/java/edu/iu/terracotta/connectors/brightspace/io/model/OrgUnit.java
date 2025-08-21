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
public class OrgUnit extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "Identifier": <string:D2LID>,
        "Name": <string>,
        "Code": <string>
     }
     */

    @JsonProperty("Identifier") private String identifier;
    @JsonProperty("Name") private String name;
    @JsonProperty("Code") private String code;

}
