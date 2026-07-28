package edu.iu.terracotta.connectors.brightspace.io.errors;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrightspaceErrorResponse {

    private Long errorReportId;
    private String status;
    private List<ErrorMessage> errors;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorMessage {
        private String message;
    }

}
