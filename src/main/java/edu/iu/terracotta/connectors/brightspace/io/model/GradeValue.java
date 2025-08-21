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
public class GradeValue {

    /*
     {
        "DisplayedGrade": <string>,
        "GradeObjectIdentifier": <string:D2LID>,
        "GradeObjectName": <string>,
        "GradeObjectType": <number:GRADEOBJ_T>,
        "GradeObjectTypeName": <string>|null,
        "Comments": { <composite:RichText> },
        "PrivateComments": { <composite:RichText> },
        "LastModified": <string:UTCDateTime>|null,
        "LastModifiedBy": <string:D2LID>|null,
        "ReleasedDate": <string:UTCDateTime>|null,
        "PointsNumerator": <number>|null,
        "PointsDenominator": <number>|null,
        "WeightedDenominator": <number>|null,
        "WeightedNumerator": <number>|null
     }
     */

    @JsonProperty("DisplayedGrade") private String displayedGrade;
    @JsonProperty("GradeObjectIdentifier") private String gradeObjectIdentifier;
    @JsonProperty("GradeObjectName") private String gradeObjectName;
    @JsonProperty("GradeObjectType") private Integer gradeObjectType;
    @JsonProperty("GradeObjectTypeName") private String gradeObjectTypeName;
    @JsonProperty("Comments") private RichText comments;
    @JsonProperty("PrivateComments") private RichText privateComments;
    @JsonProperty("LastModified") private String lastModified;
    @JsonProperty("LastModifiedBy") private String lastModifiedBy;
    @JsonProperty("ReleasedDate") private String releasedDate;
    @JsonProperty("PointsNumerator") private Double pointsNumerator;
    @JsonProperty("PointsDenominator") private Double pointsDenominator;
    @JsonProperty("WeightedDenominator") private Double weightedDenominator;
    @JsonProperty("WeightedNumerator") private Double weightedNumerator;

}
