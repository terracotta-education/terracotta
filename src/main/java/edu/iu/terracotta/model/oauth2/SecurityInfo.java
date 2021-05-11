package edu.iu.terracotta.model.oauth2;

import java.util.List;

public class SecurityInfo {

    long platformDeploymentId;
    long contextId;
    String userId;
    List<String> roles;
    //TODO, do we need more?

    public SecurityInfo() {
    }

    public long getPlatformDeploymentId() {
        return platformDeploymentId;
    }

    public void setPlatformDeploymentId(long platformDeploymentId) {
        this.platformDeploymentId = platformDeploymentId;
    }

    public long getContextId() {
        return contextId;
    }

    public void setContextId(long contextId) {
        this.contextId = contextId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
