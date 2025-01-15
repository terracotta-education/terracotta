package edu.iu.terracotta.dao.entity;

import edu.iu.terracotta.utils.TextConstants;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetakeDetails {

    private boolean retakeAllowed = false;
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
