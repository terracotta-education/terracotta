package edu.iu.terracotta.model.app;

public class RetakeDetails {

    private boolean retakeAllowed = false;
    private Float keptScore;
    private Integer submissionAttemptsCount;

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

}
