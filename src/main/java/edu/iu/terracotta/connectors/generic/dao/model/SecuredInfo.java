package edu.iu.terracotta.connectors.generic.dao.model;

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
    private String lmsUserId;
    private String lmsUserGlobalId;
    private String lmsLoginId;
    private String lmsUserName;
    private String lmsCourseId;
    private String lmsAssignmentId;
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
