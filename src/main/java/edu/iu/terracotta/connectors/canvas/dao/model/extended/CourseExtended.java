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
    public void setId(Long id) {
        course.setId(id);
    }

    @Override
    public LmsCourse from() {
        LmsCourse lmsCourse = LmsCourse.builder().build();
        lmsCourse.setId(course.getId());
        lmsCourse.setType(CourseExtended.class);

        return lmsCourse;
    }

    public static CourseExtended of(LmsCourse lmsCourse) {
        CourseExtended courseExtended = CourseExtended.builder().build();

        if (lmsCourse == null) {
            return courseExtended;
        }

        courseExtended.setId(lmsCourse.getId());

        return courseExtended;
    }

}
