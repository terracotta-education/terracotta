package edu.iu.terracotta.model.canvas;

import edu.ksu.canvas.annotation.CanvasObject;
import edu.ksu.canvas.model.assignment.Assignment;

@CanvasObject(
        postKey = "assignment"
)
public class AssignmentExtended extends Assignment {

private String secureParams;

    public String getSecureParams() {
        return secureParams;
    }

    public void setSecureParams(String secureParams) {
        this.secureParams = secureParams;
    }
}
