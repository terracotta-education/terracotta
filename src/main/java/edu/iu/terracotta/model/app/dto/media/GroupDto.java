package edu.iu.terracotta.model.app.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.imsglobal.caliper.entities.EntityType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupDto extends AbstractDto {

    private EntityType type;

    private String courseNumber;

    private String academicSession;

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    public String getCourseNumber() {
        return courseNumber;
    }

    public void setCourseNumber(String courseNumber) {
        this.courseNumber = courseNumber;
    }

    public String getAcademicSession() {
        return academicSession;
    }

    public void setAcademicSession(String academicSession) {
        this.academicSession = academicSession;
    }
}
