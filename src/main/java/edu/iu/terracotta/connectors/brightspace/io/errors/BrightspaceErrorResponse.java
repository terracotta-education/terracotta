package edu.iu.terracotta.connectors.brightspace.io.errors;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BrightspaceErrorResponse {

    private Long errorReportId;
    private String status;
    private List<ErrorMessage> errors;

    @Getter
    @Setter
    public class ErrorMessage {
        private String message;
    }

}
