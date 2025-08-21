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
public class UserGradeValue extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "User": { <composite:User> },
        "GradeValue": { <composite:GradeValue> }|null
     }
     */

    @JsonProperty("User") private User user;
    @JsonProperty("GradeValue") private GradeValue gradeValue;

}
