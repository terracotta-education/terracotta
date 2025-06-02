package edu.iu.terracotta.dao.model.enums.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageContentBodyHtmlElement {

    ATTR_DATA_ID("data-id"),
    ATTR_DATA_LABEL("data-label"),
    INVALID("INVALID"),
    INVALID_CSS_CLASS("invalid-piped-text"),
    LABEL_PREPEND_CONDITIONAL_TEXT("conditional text: "),
    LABEL_PREPEND_PIPED_TEXT("piped text: "),
    ONCLICK("onclick"),
    TAG_CONDITIONAL_TEXT("conditional-text"),
    TAG_PIPED_TEXT("piped-text");

    private final String value;
}
