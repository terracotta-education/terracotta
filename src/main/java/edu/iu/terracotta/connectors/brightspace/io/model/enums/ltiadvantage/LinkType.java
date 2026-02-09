package edu.iu.terracotta.connectors.brightspace.io.model.enums.ltiadvantage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LinkType {

    Basic(1),
    Widget(2),
    Quicklink(3),
    InsertStuff(4),
    QuizBuilder(5),
    ActivityReadOnly(6);

    private int key;

    public int key() {
        return key;
    }

    public static LinkType fromKey(int key) {
        for (LinkType type : values()) {
            if (type.key == key) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown LinkType key: [%s]", key));
    }

}
