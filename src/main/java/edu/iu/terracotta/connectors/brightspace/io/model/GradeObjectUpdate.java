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
public class GradeObjectUpdate extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "MaxPoints": <number:decimal>,
        "CanExceedMaxPoints": <boolean>,
        "IsBonus": <boolean>,
        "ExcludeFromFinalGradeCalculation": <boolean>,
        "GradeSchemeId": <number:D2LID>|null,
        "Id": <number:D2LID>,  // Not on input actions
        "Name": <string>,
        "ShortName": <string>,
        "GradeType": "Numeric",
        "CategoryId": <number:D2LID>|null,
        "Description": { <composite:RichText> },  // { <composite:RichTextInput> } on input actions
        "GradeSchemeUrl": <string:APIURL>,  // Not on input actions
        "Weight": <number:decimal>,  // Not on input actions
        "AssociatedTool": { <composite:AssociatedTool> }|null,
        "IsHidden": <bool>
      }
     */

    @JsonProperty("MaxPoints") private Double maxPoints;
    @JsonProperty("CanExceedMaxPoints") private Boolean canExceedMaxPoints;
    @JsonProperty("IsBonus") private Boolean isBonus;
    @JsonProperty("ExcludeFromFinalGradeCalculation") private Boolean excludeFromFinalGradeCalculation;
    @JsonProperty("GradeSchemeId") private Long gradeSchemeId;
    @JsonProperty("Name") private String name;
    @JsonProperty("ShortName") private String shortName;
    @JsonProperty("GradeType") private String gradeType;
    @JsonProperty("CategoryId") private Long categoryId;
    @JsonProperty("Description") private RichTextInput description;
    @JsonProperty("AssociatedTool") private AssociatedTool associatedTool;
    @JsonProperty("IsHidden") private Boolean isHidden;

    public static GradeObjectUpdate from(GradeObject gradeObject) {
        if (gradeObject == null) {
            return GradeObjectUpdate.builder().build();
        }

        return GradeObjectUpdate.builder()
            .associatedTool(gradeObject.getAssociatedTool())
            .canExceedMaxPoints(gradeObject.getCanExceedMaxPoints())
            .categoryId(gradeObject.getCategoryId())
            .description(RichTextInput.from(gradeObject.getDescription()))
            .excludeFromFinalGradeCalculation(gradeObject.getExcludeFromFinalGradeCalculation())
            .gradeSchemeId(gradeObject.getGradeSchemeId())
            .gradeType(gradeObject.getGradeType())
            .isBonus(gradeObject.getIsBonus())
            .isHidden(gradeObject.getIsHidden())
            .maxPoints(gradeObject.getMaxPoints())
            .name(gradeObject.getName())
            .shortName(gradeObject.getShortName())
            .build();
    }

}
