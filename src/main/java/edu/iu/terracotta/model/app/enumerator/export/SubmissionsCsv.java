package edu.iu.terracotta.model.app.enumerator.export;

import java.util.Arrays;

public enum SubmissionsCsv {

    // NOTE: order is important!
    SUBMISSION_ID("submission_id"),
    PARTICIPANT_ID("participant_id"),
    ASSIGNMENT_ID("assignment_id"),
    TREATMENT_ID("treatment_id"),
    SUBMITTED_AT("submitted_at"),
    CALCULATED_SCORE("calculated_score"),
    OVERRIDE_SCORE("override_score"),
    FINAL_SCORE("final_score");

    public static final String FILENAME = "submissions.csv";

    private String header;

    private SubmissionsCsv(String header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(SubmissionsCsv.values()).map(SubmissionsCsv::toString).toArray(String[]::new);
    }

}
