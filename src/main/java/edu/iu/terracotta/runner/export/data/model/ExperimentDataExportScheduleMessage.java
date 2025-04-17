package edu.iu.terracotta.runner.export.data.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ExperimentDataExportScheduleMessage {

    private long id;
    private String fileName;
    private String fileUri;
    private Timestamp deletedAt;
    private List<String> errors;

    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }

        errors.add(error);
    }

}
