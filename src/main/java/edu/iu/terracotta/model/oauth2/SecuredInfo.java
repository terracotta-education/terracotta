package edu.iu.terracotta.model.oauth2;

import java.sql.Timestamp;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecuredInfo {

    private long platformDeploymentId;
    private long contextId;
    private String userId;
    private List<String> roles;
    private String canvasUserId;
    private String canvasUserGlobalId;
    private String canvasLoginId;
    private String canvasUserName;
    private String canvasCourseId;
    private String canvasAssignmentId;
    private Timestamp dueAt;
    private Timestamp lockAt;
    private Timestamp unlockAt;
    private String nonce;
    private Boolean consent;
    private Integer studentAttempts;

    /**
     * Number of allowed attempts for assignment. The value is only populated
     * when Terracotta tool is launched as an assignment.
     *
     * @return the number of allowed attempts, or -1 if attempts are unlimited
     */
    private Integer allowedAttempts;

}
