package edu.iu.terracotta.connectors.brightspace.io.model.enums.dropbox;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DropboxType {

    Group(1),
    Individual(2);

    private int key;

    public int key() {
        return key;
    }

    public static DropboxType fromKey(int key) {
        for (DropboxType type : values()) {
            if (type.key == key) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown DropboxType key: [%s]", key));
    }

}
