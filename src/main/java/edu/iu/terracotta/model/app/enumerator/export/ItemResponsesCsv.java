package edu.iu.terracotta.model.app.enumerator.export;

import java.util.Arrays;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ItemResponsesCsv {

    // NOTE: order is important!
    ITEM_RESPONSE_ID("item_response_id"),
    SUBMISSION_ID("submission_id"),
    ASSIGNMENT_ID("assignment_id"),
    CONDITION_ID("condition_id"),
    TREATMENT_ID("treatment_id"),
    PARTICIPANT_ID("participant_id"),
    ITEM_ID("item_id"),
    RESPONSE_TYPE("response_type"),
    RESPONSE("response"),
    RESPONSE_ID("response_id"),
    REPONSE_POSITION("response_position"),
    CORRECTNESS("correctness"),
    RESPONDED_AT("responded_at"),
    POINTS_POSSIBLE("points_possible"),
    CALCULATED_SCORE("calculated_score"),
    OVERRIDE_SCORE("override_score");

    public static final String FILENAME = "item_responses.csv";

    private String header;

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(ItemResponsesCsv.values())
            .map(ItemResponsesCsv::toString)
            .toArray(String[]::new);
    }

}
