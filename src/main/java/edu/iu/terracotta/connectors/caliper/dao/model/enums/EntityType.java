package edu.iu.terracotta.connectors.caliper.dao.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;

/**
 * Caliper 1.1 entity types, written to and read from JSON by their Caliper value.
 */
@AllArgsConstructor
public enum EntityType {

    AGENT("Agent"),
    ANNOTATION("Annotation"),
    ASSESSMENT("Assessment"),
    ASSESSMENT_ITEM("AssessmentItem"),
    ASSIGNABLE_DIGITAL_RESOURCE("AssignableDigitalResource"),
    ATTEMPT("Attempt"),
    AUDIO_OBJECT("AudioObject"),
    BOOKMARK_ANNOTATION("BookmarkAnnotation"),
    CHAPTER("Chapter"),
    COLLECTION("Collection"),
    COURSE_OFFERING("CourseOffering"),
    COURSE_SECTION("CourseSection"),
    DIGITAL_RESOURCE("DigitalResource"),
    DIGITAL_RESOURCE_COLLECTION("DigitalResourceCollection"),
    DOCUMENT("Document"),
    ENTITY("Entity"),
    EPUB_CHAPTER("EpubChapter"),
    EPUB_PART("EpubPart"),
    EPUB_SUB_CHAPTER("EpubSubChapter"),
    EPUB_VOLUME("EpubVolume"),
    FILLINBLANK("FillinBlankResponse"),
    FORUM("Forum"),
    FRAME("Frame"),
    GROUP("Group"),
    HIGHLIGHT_ANNOTATION("HighlightAnnotation"),
    IMAGE_OBJECT("ImageObject"),
    LEARNING_OBJECTIVE("LearningObjective"),
    LTI_SESSION("LtiSession"),
    MEDIA_LOCATION("MediaLocation"),
    MEDIA_OBJECT("MediaObject"),
    MEMBERSHIP("Membership"),
    MESSAGE("Message"),
    MULTIPLECHOICE("MultipleChoiceResponse"),
    MULTIPLERESPONSE("MultipleResponseResponse"),
    PAGE("Page"),
    PERSON("Person"),
    ORGANIZATION("Organization"),
    READING("Reading"),
    RESPONSE("Response"),
    RESULT("Result"),
    SCORE("Score"),
    SELECTTEXT("SelectTextResponse"),
    SESSION("Session"),
    SHARED_ANNOTATION("SharedAnnotation"),
    SOFTWARE_APPLICATION("SoftwareApplication"),
    TAG_ANNOTATION("TagAnnotation"),
    THREAD("Thread"),
    TRUEFALSE("TrueFalseResponse"),
    VIDEO_OBJECT("VideoObject"),
    WEB_PAGE("WebPage");

    private final String value;

    @JsonValue
    public String value() {
        return value;
    }

}
