package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.ksu.canvas.annotation.CanvasObject;
import edu.ksu.canvas.model.User;
import edu.ksu.canvas.model.assignment.Submission;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@CanvasObject(postKey = "submissions")
public class SubmissionExtended extends LmsSubmission {

    @Builder.Default private Submission submission = new Submission();

    @Override
    public Long getAttempt() {
        return submission.getAttempt();
    }

    @Override
    public Double getScore() {
        return submission.getScore();
    }

    @Override
    public Object getUser() {
        return (User) submission.getUser();
    }

    @Override
    public Long getUserId() {
        return submission.getUser().getId();
    }

    @Override
    public String getUserLoginId() {
        return submission.getUser().getLoginId();
    }

    @Override
    public String getUserName() {
        return submission.getUser().getName();
    }

    @Override
    public LmsSubmission convert() {
        return (LmsSubmission) this;
    }

}
