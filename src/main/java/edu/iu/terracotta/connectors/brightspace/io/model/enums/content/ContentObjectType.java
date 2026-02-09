package edu.iu.terracotta.connectors.brightspace.io.model.enums.content;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContentObjectType {

    Module(0),
    Topic(1);

    private int key;

    public int key() {
        return key;
    }

    public static ContentObjectType fromKey(int key) {
        for (ContentObjectType type : values()) {
            if (type.key == key) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown ContentObjectType key: [%s]", key));
    }

}
