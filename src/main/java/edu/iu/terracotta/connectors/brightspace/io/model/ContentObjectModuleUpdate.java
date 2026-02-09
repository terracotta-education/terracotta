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
public class ContentObjectModuleUpdate extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "Title": <string>,
        "ShortTitle": <string>,
        "Type": 0,
        "ModuleStartDate": <string:UTCDateTime>|null,
        "ModuleEndDate": <string:UTCDateTime>|null,
        "ModuleDueDate": <string:UTCDateTime>|null,
        "IsHidden": <boolean>,
        "IsLocked": <boolean>,
        "Description": { <composite:RichTextInput> }|null,
        "Duration": <number>|null
     }
     */

   @JsonProperty("Title") private String title;
   @JsonProperty("ShortTitle") private String shortTitle;
   @JsonProperty("ModuleStartDate") private String moduleStartDate;
   @JsonProperty("ModuleEndDate") private String moduleEndDate;
   @JsonProperty("ModuleDueDate") private String moduleDueDate;
   @JsonProperty("IsHidden") private Boolean isHidden;
   @JsonProperty("IsLocked") private Boolean isLocked;
   @JsonProperty("Description") private RichTextInput description;
   @JsonProperty("Duration") private Integer duration;

   @Builder.Default
   @JsonProperty("Type")
   private int type = 0;

   public static ContentObjectModuleUpdate from(ContentObjectModule contentObjectModule) {
       if (contentObjectModule == null) {
           return ContentObjectModuleUpdate.builder().build();
       }

       return ContentObjectModuleUpdate.builder()
            .description(RichTextInput.from(contentObjectModule.getDescription()))
            .duration(contentObjectModule.getDuration())
            .isHidden(contentObjectModule.getIsHidden())
            .isLocked(contentObjectModule.getIsLocked())
            .moduleStartDate(contentObjectModule.getModuleStartDate())
            .moduleEndDate(contentObjectModule.getModuleEndDate())
            .moduleDueDate(contentObjectModule.getModuleDueDate())
            .shortTitle(contentObjectModule.getShortTitle())
            .title(contentObjectModule.getTitle())
            .type(contentObjectModule.getType())
            .build();
   }

}
