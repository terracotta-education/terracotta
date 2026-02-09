package edu.iu.terracotta.connectors.canvas.dao.model.enums.jwt;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CanvasJwtClaim {

    ALLOWED_ATTEMPTS(null, "allowed_attempts"),
    CANVAS("CANVAS", null),
    CANVAS_ASSIGNMENT_ID("canvasAssignmentId", "canvas_assignment_id"),
    CANVAS_COURSE_ID("canvasCourseId", "canvas_course_id"),
    CANVAS_LOGIN_ID("canvasLoginId", "canvas_login_id"),
    CANVAS_USER_GLOBAL_ID("canvasUserGlobalId", "canvas_user_global_id"),
    CANVAS_USER_ID("canvasUserId", "canvas_user_id"),
    CANVAS_USER_NAME("canvasUserName", "canvas_user_name"),
    STUDENT_ATTEMPTS(null, "student_attempts");

    private String key;
    private String _key;

    public String key() {
        return key;
    }

    public String key(int index) {
        switch (index) {
            case 0: {
                return key;
            }
            case 1: {
                return _key;
            }
            default:
                throw new IllegalArgumentException(String.format("Invalid index: [%s]", index));
        }
    }

}
