package edu.iu.terracotta.model.app;

import edu.iu.terracotta.utils.TextConstants;

public class RetakeDetails {

    private boolean retakeAllowed = false;
    private Float keptScore;
    private Integer submissionAttemptsCount;
    private String retakeNotAllowedReason;

    public boolean isRetakeAllowed() {
        return retakeAllowed;
    }

    public void setRetakeAllowed(boolean retakeAllowed) {
        this.retakeAllowed = retakeAllowed;
    }

    public Float getKeptScore() {
        return keptScore;
    }

    public void setKeptScore(Float keptScore) {
        this.keptScore = keptScore;
    }

    public Integer getSubmissionAttemptsCount() {
        return submissionAttemptsCount;
    }

    public void setSubmissionAttemptsCount(Integer submissionAttemptsCount) {
        this.submissionAttemptsCount = submissionAttemptsCount;
    }

    public String getRetakeNotAllowedReason() {
        return retakeNotAllowedReason;
    }

    public void setRetakeNotAllowedReason(String retakeNotAllowedReason) {
        this.retakeNotAllowedReason = retakeNotAllowedReason;
    }

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
