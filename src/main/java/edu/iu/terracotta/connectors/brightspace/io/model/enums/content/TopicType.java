package edu.iu.terracotta.connectors.brightspace.io.model.enums.content;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TopicType {

    File(1),
    Link(3);

    private int key;

    public int key() {
        return key;
    }

    public static TopicType fromKey(int key) {
        for (TopicType type : values()) {
            if (type.key == key) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown TopicType key: [%s]", key));
    }

}
