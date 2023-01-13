package edu.iu.terracotta.model.oauth2;

import java.sql.Timestamp;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecuredInfo {

    long platformDeploymentId;
    long contextId;
    String userId;
    List<String> roles;
    String canvasUserId;
    String canvasUserGlobalId;
    String canvasLoginId;
    String canvasUserName;
    String canvasCourseId;
    String canvasAssignmentId;
    Timestamp dueAt;
    Timestamp lockAt;
    Timestamp unlockAt;
    String nonce;
    Boolean consent;
    Integer studentAttempts;

    /**
     * Number of allowed attempts for assignment. The value is only populated
     * when Terracotta tool is launched as an assignment.
     *
     * @return the number of allowed attempts, or -1 if attempts are unlimited
     */
    Integer allowedAttempts;

}
