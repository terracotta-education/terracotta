package edu.iu.terracotta.dao.model.enums.export;

import java.util.Arrays;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TreatmentCsv {

    // NOTE: order is important!
    ASSIGNMENT_NAME("assignment_name"),
    ASSIGNMENT_DUE_DATE("assignment_due_date"),
    LMS_ASSIGNMENT_ID("lms_assignment_id"),
    TERRACOTTA_ASSIGNMENT_ID("assignment_id"),
    TREATMENT_ID("treatment_id"),
    TREATMENT_INSTRUCTIONS("treatment_instructions"),
    EXPOSURE_ID("exposure_id"),
    CONDITION_ID("condition_id"),
    CONDITION_NAME("condition_name"),
    GRADE_TYPE("grade_type"),
    ATTEMPTS_ALLOWED("attempts_allowed"),
    TIME_REQUIRED_BETWEEN_ATTEMPTS("time_required_between_attempts");

    public static final String FILENAME = "treatment.csv";

    private String header;

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(values())
            .map(TreatmentCsv::toString)
            .toArray(String[]::new);
    }

}
