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

}
