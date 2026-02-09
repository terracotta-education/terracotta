package edu.iu.terracotta.connectors.brightspace.io.model.enums.richtext;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RichTextType {

    HTML("Html"),
    TEXT("Text");

    private final String type;

    public String type() {
        return type;
    }

}
