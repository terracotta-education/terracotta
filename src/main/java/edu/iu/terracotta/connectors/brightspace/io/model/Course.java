package edu.iu.terracotta.connectors.brightspace.io.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Course extends BaseBrightspaceModel implements Serializable {

    public static final long serialVersionUID = 1L;

    /*
     {
        "Identifier": <string:D2LID>,
        "Name": <string>,
        "Code": <string>,
        "IsActive": <boolean>,
        "Path": <string>,
        "StartDate": <string:UTCDateTime>|null,
        "EndDate": <string:UTCDateTime>|null,
        "LocaleId": <number:D2LID>|null,
        "ForceLocale": <boolean>,
        "CourseTemplate": { <composite:Course.BasicOrgUnit> }|null,
        "Semester": { <composite:Course.BasicOrgUnit> }|null,
        "Department": { <composite:Course.BasicOrgUnit> }|null,
        "Description": { <composite:RichText> },
        "CanSelfRegister": <boolean>
     }
     */

    @JsonProperty("LocaleId") private Long localeId;
    @JsonProperty("ForceLocale") private Boolean forceLocale;
    @JsonProperty("Identifier") private String identifier;
    @JsonProperty("Name") private String name;
    @JsonProperty("Code") private String code;
    @JsonProperty("IsActive") private Boolean isActive;
    @JsonProperty("CanSelfRegister") private Boolean canSelfRegister;
    @JsonProperty("Description") private RichText description;
    @JsonProperty("Path") private String path;
    @JsonProperty("StartDate") private String startDate;
    @JsonProperty("EndDate") private String endDate;
    @JsonProperty("CourseTemplate") private OrgUnit courseTemplate;
    @JsonProperty("Semester") private OrgUnit semester;
    @JsonProperty("Department") private OrgUnit department;

}
