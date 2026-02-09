package edu.iu.terracotta.connectors.brightspace.io.model.enums.dropbox;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CompletionType {

    OnSubmission(0),
    DueDate(1),
    ManuallyByLearner(2),
    OnEvaluation(3);

    private int key;

    public int key() {
        return key;
    }

    public static CompletionType fromKey(int key) {
        for (CompletionType type : values()) {
            if (type.key == key) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown CompletionType key: [%s]", key));
    }

}
