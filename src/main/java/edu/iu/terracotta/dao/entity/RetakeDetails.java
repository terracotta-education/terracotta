package edu.iu.terracotta.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.utils.TextConstants;
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
@SuppressWarnings({"PMD.LooseCoupling"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetakeDetails {

    @Builder.Default private boolean retakeAllowed = false;

    private Float keptScore;
    private Integer submissionAttemptsCount;
    private String retakeNotAllowedReason;
    private Float lastAttemptScore;

    public enum RetakeNotAllowedReason {

        MAX_NUMBER_ATTEMPTS_REACHED, // max # of attempts reached
        WAIT_TIME_NOT_REACHED, // submission wait time has not passed
        OTHER, // generic catch-all reason (should not happen)

    }

    public static String calculateRetakeNotAllowedReason(String message) {
        switch (message) {
            case TextConstants.LIMIT_OF_SUBMISSIONS_REACHED:
                return RetakeNotAllowedReason.MAX_NUMBER_ATTEMPTS_REACHED.toString();

            case TextConstants.ASSIGNMENT_SUBMISSION_WAIT_TIME_NOT_REACHED:
                return RetakeNotAllowedReason.WAIT_TIME_NOT_REACHED.toString();

            default:
                return RetakeNotAllowedReason.OTHER.toString();
        }
    }

}
