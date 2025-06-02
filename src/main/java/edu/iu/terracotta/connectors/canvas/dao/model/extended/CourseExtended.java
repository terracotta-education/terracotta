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
    public String getId() {
        if (course == null || course.getId() == null) {
            return null;
        }

        return String.valueOf(course.getId());
    }

    @Override
    public void setId(String id) {
        if (course == null) {
            return;
        }

        course.setId(Long.valueOf(id));
    }

    @Override
    public LmsCourse from() {
        LmsCourse lmsCourse = LmsCourse.builder().build();
        lmsCourse.setId(getId());
        lmsCourse.setType(getType());

        return lmsCourse;
    }

    public static CourseExtended of(LmsCourse lmsCourse) {
        if (lmsCourse == null) {
            return CourseExtended.builder().build();
        }

        CourseExtended courseExtended = CourseExtended.builder().build();
        courseExtended.setId(lmsCourse.getId());

        return courseExtended;
    }

}
