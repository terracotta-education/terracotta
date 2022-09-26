package edu.iu.terracotta.model.app.enumerator.export;

import java.util.Arrays;

public enum ParticipantTreatmentCsv {

    // NOTE: order is important!
    PARTICIPANT_ID("participant_id"),
    EXPOSURE_ID("exposure_id"),
    CONDITION_ID("condition_id"),
    CONDITION_TYPE("condition_name"),
    ASSIGNMENT_ID("assignment_id"),
    ASSIGNMENT_NAME("assignment_name"),
    TREATMENT_ID("treatment_id"),
    GRADE_TYPE("grade_type");

    public static final String FILENAME = "participant_treatment.csv";

    private String header;

    private ParticipantTreatmentCsv(String header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(ParticipantTreatmentCsv.values()).map(ParticipantTreatmentCsv::toString).toArray(String[]::new);
    }

}
