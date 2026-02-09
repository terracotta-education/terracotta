package edu.iu.terracotta.connectors.brightspace.io.model;

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
public class AssociatedTool {

    /*
     {
        "ToolId": <number:D2LID>,
        "ToolItemId": <number:D2LID>
     }
     */

    @JsonProperty("ToolId") private Long toolId;
    @JsonProperty("ToolItemId") private Long toolItemId;

}
