package edu.iu.terracotta.model.canvas;

import edu.ksu.canvas.annotation.CanvasObject;
import edu.ksu.canvas.model.assignment.Assignment;

@CanvasObject(
        postKey = "assignment"
)
public class AssignmentExtended extends Assignment {

private String secureParams;

private int allowedAttempts = -1;

private boolean canSubmit = true;

    public String getSecureParams() {
        return secureParams;
    }

    public void setSecureParams(String secureParams) {
        this.secureParams = secureParams;
    }

    public int getAllowedAttempts() {
        return allowedAttempts;
    }

    public void setAllowedAttempts(int allowedAttempts) {
        this.allowedAttempts = allowedAttempts;
    }

    public boolean isCanSubmit() {
        return canSubmit;
    }

    public void setCanSubmit(boolean canSubmit) {
        this.canSubmit = canSubmit;
    }
}
