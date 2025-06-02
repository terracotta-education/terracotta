package edu.iu.terracotta.dao.model.enums.export;

import java.util.Arrays;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ExperimentCsv {

    // NOTE: order is important!
    EXPERIMENT_ID("experiment_id"),
    COURSE_ID("course_id"),
    EXERIMENT_TITLE("experiment_title"),
    EXPERIMENT_DESCRIPTION("experiment_description"),
    EXPOSURE_TYPE("exposure_type"),
    PARTICIPATION_TYPE("participation_type"),
    DISTRIBUTION_TYPE("distribution_type"),
    EXPORT_AT("export_at"),
    ENROLLMENT_CNT("enrollment_cnt"),
    PARTICIPATION_CNT("participant_cnt"),
    CONDITION_CNT("condition_cnt"),
    CREATED_AT("created_at"),
    STARTED_AT("started_at");

    public static final String FILENAME = "experiment.csv";

    private String header;

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(values())
            .map(ExperimentCsv::toString)
            .toArray(String[]::new);
    }

}
