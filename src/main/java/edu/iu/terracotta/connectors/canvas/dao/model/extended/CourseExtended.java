package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import edu.ksu.canvas.annotation.CanvasObject;
import edu.ksu.canvas.model.Course;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@CanvasObject(postKey = "courses")
public class CourseExtended extends LmsCourse {

    @Builder.Default private Course course = new Course();

    @Override
    public Long getId() {
        return course.getId();
    }

    @Override
    public LmsCourse convert() {
        return (LmsCourse) this;
    }


}
