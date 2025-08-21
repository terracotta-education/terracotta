package edu.iu.terracotta.connectors.brightspace.dao.model.extended;

import edu.iu.terracotta.connectors.brightspace.io.model.Submission;
import edu.iu.terracotta.connectors.brightspace.io.model.User;
import edu.iu.terracotta.connectors.brightspace.io.model.UserGradeValue;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@SuperBuilder
@SuppressWarnings("PMD.GuardLogStatement")
public class SubmissionExtended extends LmsSubmission {

    @Builder.Default private Submission submission = Submission.builder().build();

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
    public String getUserId() {
        return submission.getUserId();
    }

    @Override
    public void setUserId(String userId) {
        submission.setUserId(userId);

        if (submission.getUser() != null) {
            submission.getUser().setIdentifier(userId);
        }
    }

    @Override
    public String getUserLoginId() {
        if (submission.getUser() == null) {
            return null;
        }

        return submission.getUser().getUserName();
    }

    @Override
    public void setUserLoginId(String userLoginId) {
        if (submission.getUser() == null) {
            return;
        }

        submission.getUser().setUserName(userLoginId);
    }

    @Override
    public String getUserName() {
        if (submission.getUser() == null) {
            return null;
        }

        return submission.getUser().getDisplayName();
    }

    @Override
    public void setUserName(String userName) {
        if (submission.getUser() == null) {
            return;
        }

        submission.getUser().setDisplayName(userName);
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
        if (lmsSubmission == null) {
            return SubmissionExtended.builder().build();
        }

        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setAttempt(lmsSubmission.getAttempt());
        submissionExtended.setScore(lmsSubmission.getScore());
        submissionExtended.setUser(lmsSubmission.getUser());
        submissionExtended.setUserId(lmsSubmission.getUserId());
        submissionExtended.setUserLoginId(lmsSubmission.getUserLoginId());
        submissionExtended.setUserName(lmsSubmission.getUserName());

        return submissionExtended;
    }

    public static SubmissionExtended of(UserGradeValue userGradeValue, String lmsAssignmentId, String orgUnitId) {
        if (userGradeValue == null) {
            return SubmissionExtended.builder().build();
        }

        Double score = 0D;

        if (userGradeValue.getGradeValue() != null && userGradeValue.getGradeValue().getPointsNumerator() != null) {
            try {
                score = userGradeValue.getGradeValue().getPointsNumerator();
            } catch (NumberFormatException e) {
                log.warn(
                    "Could not parse user grade value [{}] for user [{}] in Brightspace course by orgUnitId: [{}]",
                    userGradeValue.getGradeValue().getPointsNumerator(),
                    userGradeValue.getUser().getIdentifier(), orgUnitId
                );
            }
        }

        SubmissionExtended submissionExtended = SubmissionExtended.builder().build();
        submissionExtended.setAssignmentId(lmsAssignmentId);
        submissionExtended.setAttempt(1L);
        submissionExtended.setGradeMatchesCurrentSubmission(true);
        submissionExtended.setScore(score);
        submissionExtended.setState(null);
        submissionExtended.setUser(userGradeValue.getUser());
        submissionExtended.setUserId(userGradeValue.getUser().getIdentifier());
        submissionExtended.setUserLoginId(userGradeValue.getUser().getUserName());
        submissionExtended.setUserName(userGradeValue.getUser().getDisplayName());

        return submissionExtended;
    }

}
