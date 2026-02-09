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
public class ContentObjectModule extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "Structure": [ <Content.ContentObject>, ... ],
        "ModuleStartDate": <string:UTCDateTime>|null,
        "ModuleEndDate": <string:UTCDateTime>|null,
        "ModuleDueDate": <string:UTCDateTime>|null,
        "IsHidden": <boolean>,
        "IsLocked": <boolean>,
        "Id": <number:D2LID>,
        "Title": <string>,
        "ShortTitle": <string>,
        "Color": <string>|null,  // Added with LMS 20.24.7
        "Type": 0,
        "Description": { <composite:RichText> }|null,
        "ParentModuleId": <number:D2LID>|null,
        "Duration": <number>|null,
        "LastModifiedDate": <string:UTCDateTime>|null
      }
     */

   @JsonProperty("Title") private String title;
   @JsonProperty("ShortTitle") private String shortTitle;
   @JsonProperty("ModuleStartDate") private String moduleStartDate;
   @JsonProperty("ModuleEndDate") private String moduleEndDate;
   @JsonProperty("ModuleDueDate") private String moduleDueDate;
   @JsonProperty("IsHidden") private Boolean isHidden;
   @JsonProperty("IsLocked") private Boolean isLocked;
   @JsonProperty("Description") private RichText description;
   @JsonProperty("Duration") private Integer duration;
   @JsonProperty("Id") private Long id;
   @JsonProperty("Structure") private List<ContentObjectTopic> structure;
   @JsonProperty("Color") private String color;
   @JsonProperty("ParentModuleId") private Long parentModuleId;
   @JsonProperty("LastModifiedDate") private String lastModifiedDate;

   @Builder.Default
   @JsonProperty("Type")
   private int type = 0;

}
