package edu.iu.terracotta.model.app.enumerator.export;

import java.util.Arrays;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ParticipantTreatmentCsv {

    // NOTE: order is important!
    PARTICIPANT_ID("participant_id"),
    EXPOSURE_ID("exposure_id"),
    CONDITION_ID("condition_id"),
    CONDITION_TYPE("condition_name"),
    ASSIGNMENT_ID("assignment_id"),
    ASSIGNMENT_NAME("assignment_name"),
    ASSIGNMENT_DUE_DATE("assignment_due_date"),
    TREATMENT_ID("treatment_id"),
    GRADE_TYPE("grade_type"),
    ATTEMPTS_ALLOWED("attempts_allowed"),
    TIME_REQUIRED_BETWEEN_ATTEMPTS("time_required_between_attempts"),
    FINAL_SCORE("final_score");

    public static final String FILENAME = "participant_treatment.csv";

    private String header;

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(ParticipantTreatmentCsv.values())
            .map(ParticipantTreatmentCsv::toString)
            .toArray(String[]::new);
    }

}
