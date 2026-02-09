package edu.iu.terracotta.connectors.brightspace.io.model.enums.dropbox;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubmissionType {

    File(0),
    Text(1),
    OnPaper(2),
    Observed(3),
    FileOrText(4);

    private int key;

    public int key() {
        return key;
    }

    public static SubmissionType fromKey(int key) {
        for (SubmissionType type : values()) {
            if (type.key == key) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown SubmissionType key: [%s]", key));
    }

}
