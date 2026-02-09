package edu.iu.terracotta.connectors.generic.dao.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LmsConnector {

    BRIGHTSPACE("Brightspace"),
    CANVAS("Canvas"),
    GENERIC("Generic"),
    ONE_ED_TECH("1EdTech");

    private String title;

    public String title() {
        return title;
    }

}
