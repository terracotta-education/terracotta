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
public class File extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "FileId": <number:D2LID>,
        "FileName": <string>,
        "Size": <number:long>
     }
     */

     @JsonProperty("FileId") private Long fileId;
     @JsonProperty("FileName") private String fileName;
     @JsonProperty("Size") private Long size;

}
