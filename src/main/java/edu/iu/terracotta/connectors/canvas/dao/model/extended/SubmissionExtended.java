package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

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
    public String getAssignmentId() {
        if (submission == null || submission.getAssignmentId() == null) {
            return null;
        }

        return Long.toString(submission.getAssignmentId());
    }

    @Override
    public void setAssignmentId(String assignmentId) {
        if (submission == null) {
            return;
        }

        submission.setAssignmentId(Long.parseLong(assignmentId));
    }

    @Override
    public Long getAttempt() {
        if (submission == null) {
            return null;
        }

        if (submission.getAttempt() == null) {
            return 0L;
        }

        return submission.getAttempt();
    }

    @Override
    public void setAttempt(Long attempt) {
        if (submission == null) {
            return;
        }

        submission.setAttempt(attempt);
    }

    @Override
    public boolean isGradeMatchesCurrentSubmission() {
        return BooleanUtils.isTrue(submission.getGradeMatchesCurrentSubmission());
    }

    @Override
    public void setGradeMatchesCurrentSubmission(boolean gradeMatchesCurrentSubmission) {
        if (submission == null) {
            return;
        }

        submission.setGradeMatchesCurrentSubmission(gradeMatchesCurrentSubmission);
    }

    @Override
    public Double getScore() {
        if (submission == null) {
            return null;
        }

        return submission.getScore();
    }

    @Override
    public void setScore(Double score) {
        if (submission == null) {
            return;
        }

        submission.setScore(score);
    }

    @Override
    public String getState() {
        if (submission == null) {
            return null;
        }

        return submission.getWorkflowState();
    }

    @Override
    public void setState(String state) {
        if (submission == null) {
            return;
        }

        submission.setWorkflowState(state);
    }

    @Override
    public Object getUser() {
        if (submission == null) {
            return null;
        }

        if (submission.getUser() == null) {
            return null;
        }

        return (User) submission.getUser();
    }

    @Override
    public void setUser(Object user) {
        if (submission == null) {
            return;
        }

        submission.setUser((User) user);
    }

    @Override
    public String getUserId() {
        if (submission == null) {
            return null;
        }

        if (submission.getUser() == null) {
            return null;
        }

        return Long.toString(submission.getUserId());
    }

    @Override
    public void setUserId(String userId) {
        if (submission == null || StringUtils.isBlank(userId)) {
            return;
        }

        submission.setUserId(Long.parseLong(userId));

        if (submission.getUser() != null) {
            submission.getUser().setId(Long.parseLong(userId));
        }
    }

    @Override
    public String getUserLoginId() {
        if (submission.getUser() == null) {
            return null;
        }

        return submission.getUser().getLoginId();
    }

    @Override
    public void setUserLoginId(String userLoginId) {
        if (submission.getUser() == null) {
            return;
        }

        submission.getUser().setLoginId(userLoginId);
    }

    @Override
    public String getUserName() {
        if (submission.getUser() == null) {
            return null;
        }

        return submission.getUser().getName();
    }

    @Override
    public void setUserName(String userName) {
        if (submission.getUser() == null) {
            return;
        }

        submission.getUser().setName(userName);
    }

    @Override
    public LmsSubmission from() {
        LmsSubmission lmsSubmission = LmsSubmission.builder().build();
        lmsSubmission.setAssignmentId(getAssignmentId());
        lmsSubmission.setAttempt(getAttempt());
        lmsSubmission.setGradeMatchesCurrentSubmission(isGradeMatchesCurrentSubmission());
        lmsSubmission.setScore(getScore());
        lmsSubmission.setState(getState());
        lmsSubmission.setType(SubmissionExtended.class);
        lmsSubmission.setUser(getUser());
        lmsSubmission.setUserId(getUserId());
        lmsSubmission.setUserLoginId(getUserLoginId());
        lmsSubmission.setUserName(getUserName());

        return lmsSubmission;
    }

    public static SubmissionExtended of(LmsSubmission lmsSubmission) {
        if (lmsSubmission == null) {
            return SubmissionExtended.builder().build();
        }

        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setAssignmentId(lmsSubmission.getAssignmentId());
        submissionExtended.setAttempt(lmsSubmission.getAttempt());
        submissionExtended.setGradeMatchesCurrentSubmission(lmsSubmission.isGradeMatchesCurrentSubmission());
        submissionExtended.setScore(lmsSubmission.getScore());
        submissionExtended.setState(lmsSubmission.getState());
        submissionExtended.setUser(lmsSubmission.getUser());
        submissionExtended.setUserId(lmsSubmission.getUserId());
        submissionExtended.setUserLoginId(lmsSubmission.getUserLoginId());
        submissionExtended.setUserName(lmsSubmission.getUserName());

        return submissionExtended;
    }

}
