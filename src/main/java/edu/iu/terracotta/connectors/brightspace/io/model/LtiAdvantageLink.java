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
public class LtiAdvantageLink extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "LinkId": <D2LID>,
        "DeploymentId": <GUID>,
        "IsEnabled": <boolean>,
        "Name": <string>,
        "Description": <string>|null,
        "URL": <string>,
        "Type": <number:LINK_TYPE_T>,
        "Height": <int>|null,
        "Width": <int>|null,
        "CustomParameters": null|[ { <composite:LTI.CustomParameters> }, ... ],
        "IsAvailable": <boolean>,
        "OwnerOrgUnitId": <D2LID>
     }
    */

    @JsonProperty("LinkId") private Long linkId;
    @JsonProperty("DeploymentId") private String deploymentId;
    @JsonProperty("IsEnabled") private Boolean isEnabled;
    @JsonProperty("Name") private String name;
    @JsonProperty("Description") private String description;
    @JsonProperty("URL") private String url;
    @JsonProperty("Type") private Integer type;
    @JsonProperty("Height") private Integer height;
    @JsonProperty("Width") private Integer width;
    @JsonProperty("CustomParameters") private List<CustomParameter> customParameters;
    @JsonProperty("IsAvailable") private Boolean isAvailable;
    @JsonProperty("OwnerOrgUnitId") private Long ownerOrgUnitId;

}
