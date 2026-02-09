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
public class GradeObject {

    /*
     {
        "MaxPoints": <number:decimal>,
        "CanExceedMaxPoints": <boolean>,
        "IsBonus": <boolean>,
        "ExcludeFromFinalGradeCalculation": <boolean>,
        "GradeSchemeId": <number:D2LID>|null,
        "Id": <number:D2LID>,
        "Name": <string>,
        "ShortName": <string>,
        "GradeType": "Numeric",
        "CategoryId": <number:D2LID>|null,
        "Description": { <composite:RichText> },
        "GradeSchemeUrl": <string:APIURL>,
        "Weight": <number:decimal>,
        "AssociatedTool": { <composite:AssociatedTool> }|null,
        "IsHidden": <bool>
     }
     */

    @JsonProperty("MaxPoints") private Double maxPoints;
    @JsonProperty("CanExceedMaxPoints") private Boolean canExceedMaxPoints;
    @JsonProperty("IsBonus") private Boolean isBonus;
    @JsonProperty("ExcludeFromFinalGradeCalculation") private Boolean excludeFromFinalGradeCalculation;
    @JsonProperty("GradeSchemeId") private Long gradeSchemeId;
    @JsonProperty("Id") private Long id;
    @JsonProperty("Name") private String name;
    @JsonProperty("ShortName") private String shortName;
    @JsonProperty("GradeType") private String gradeType;
    @JsonProperty("CategoryId") private Long categoryId;
    @JsonProperty("Description") private RichText description;
    @JsonProperty("GradeSchemeUrl") private String gradeSchemeUrl;
    @JsonProperty("Weight") private Double weight;
    @JsonProperty("AssociatedTool") private AssociatedTool associatedTool;
    @JsonProperty("IsHidden") private Boolean isHidden;

}
