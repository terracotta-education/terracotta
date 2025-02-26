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
    public void setAttempt(Long attempt) {
        submission.setAttempt(attempt);
    }

    @Override
    public Double getScore() {
        return submission.getScore();
    }

    @Override
    public void setScore(Double score) {
        submission.setScore(score);
    }

    @Override
    public Object getUser() {
        return (User) submission.getUser();
    }

    @Override
    public void setUser(Object user) {
        submission.setUser((User) user);
    }

    @Override
    public Long getUserId() {
        return submission.getUser().getId();
    }

    @Override
    public void setUserId(Long userId) {
        submission.getUser().setId(userId);
    }

    @Override
    public String getUserLoginId() {
        return submission.getUser().getLoginId();
    }

    @Override
    public void setUserLoginId(String userLoginId) {
        submission.getUser().setLoginId(userLoginId);
    }

    @Override
    public String getUserName() {
        return submission.getUser().getName();
    }

    @Override
    public void setUserName(String userName) {
        submission.getUser().setName(userName);
    }

    @Override
    public LmsSubmission from() {
        LmsSubmission lmsSubmission = LmsSubmission.builder().build();
        lmsSubmission.setAttempt(getAttempt());
        lmsSubmission.setScore(getScore());
        lmsSubmission.setType(SubmissionExtended.class);
        lmsSubmission.setUser(getUser());
        lmsSubmission.setUserId(getUserId());
        lmsSubmission.setUserLoginId(getUserLoginId());
        lmsSubmission.setUserName(getUserName());

        return lmsSubmission;
    }

    public static SubmissionExtended of(LmsSubmission lmsSubmission) {
        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();

        if (lmsSubmission == null) {
            return submissionExtended;
        }

        submissionExtended.setAttempt(lmsSubmission.getAttempt());
        submissionExtended.setScore(lmsSubmission.getScore());
        submissionExtended.setUser(lmsSubmission.getUser());
        submissionExtended.setUserId(lmsSubmission.getUserId());
        submissionExtended.setUserLoginId(lmsSubmission.getUserLoginId());
        submissionExtended.setUserName(lmsSubmission.getUserName());

        return submissionExtended;
    }

}
