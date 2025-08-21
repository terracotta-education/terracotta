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
public class RichText extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "Text": <string:plaintext_form_of_text>,
        "Html": <string:HTML_form_of_text>|null
     }
     */

    @JsonProperty("Text") private String text;
    @JsonProperty("Html") private String html;

}
