package edu.iu.terracotta.model.app.enumerator.export;

import java.util.Arrays;

public enum OutcomesCsv {

    // NOTE: order is important!
    OUTCOME_ID("outcome_id"),
    PARTICIPANT_ID("participant_id"),
    EXPOSURE_ID("exposure_id"),
    SOURCE("source"),
    OUTCOME_NAME("outcome_name"),
    POINTS_POSSIBLE("points_possible"),
    OUTCOME_SCORE("outcome_score"),
    CONDITION_NAME("condition_name"),
    CONDITION_ID("condition_id");

    public static final String FILENAME = "outcomes.csv";

    private String header;

    OutcomesCsv(String header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(OutcomesCsv.values()).map(OutcomesCsv::toString).toArray(String[]::new);
    }

}
