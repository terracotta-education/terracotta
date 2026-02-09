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
public class Link extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "LinkId": <number:D2LID>,
        "LinkName": <string>,
        "Href": <string>
     }
     */

    @JsonProperty("LinkId") private String linkId;
    @JsonProperty("LinkName") private String linkName;
    @JsonProperty("Href") private String href;

}
