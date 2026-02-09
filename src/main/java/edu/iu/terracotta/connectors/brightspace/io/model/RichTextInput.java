package edu.iu.terracotta.connectors.brightspace.io.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.iu.terracotta.connectors.brightspace.io.model.enums.richtext.RichTextType;
import io.micrometer.common.util.StringUtils;
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
public class RichTextInput extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "Content": <string>,
        "Type": "Text|Html"
     }
     */

    @JsonProperty("Content") private String content;
    @JsonProperty("Type") private String type;

    public static RichTextInput from(RichText richText) {
        if (richText == null) {
            return RichTextInput.builder().build();
        }

        return RichTextInput.builder()
                .content(StringUtils.isNotBlank(richText.getHtml()) ? richText.getHtml() : richText.getText())
                .type(StringUtils.isNotBlank(richText.getHtml()) ? RichTextType.HTML.type() : RichTextType.TEXT.type())
                .build();
    }

}
