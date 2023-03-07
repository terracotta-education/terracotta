package edu.iu.terracotta.model.canvas;

import edu.ksu.canvas.annotation.CanvasObject;
import edu.ksu.canvas.model.assignment.Assignment;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CanvasObject(
        postKey = "assignment"
)
public class AssignmentExtended extends Assignment {

    private String secureParams;
    private int allowedAttempts = -1;
    private boolean canSubmit = true;

}
