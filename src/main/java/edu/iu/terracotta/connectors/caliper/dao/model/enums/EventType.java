package edu.iu.terracotta.connectors.caliper.dao.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;

/**
 * Caliper 1.1 event types, written to and read from JSON by their Caliper value.
 */
@AllArgsConstructor
public enum EventType {

    ANNOTATION("AnnotationEvent"),
    ASSESSMENT("AssessmentEvent"),
    ASSESSMENT_ITEM("AssessmentItemEvent"),
    ASSIGNABLE("AssignableEvent"),
    EVENT("Event"),
    FORUM("ForumEvent"),
    MEDIA("MediaEvent"),
    MESSAGE("MessageEvent"),
    NAVIGATION("NavigationEvent"),
    GRADE("GradeEvent"),
    READING("ReadingEvent"),
    SESSION("SessionEvent"),
    THREAD("ThreadEvent"),
    TOOL_USE("ToolUseEvent"),
    VIEW("ViewEvent");

    private final String value;

    @JsonValue
    public String value() {
        return value;
    }

}
