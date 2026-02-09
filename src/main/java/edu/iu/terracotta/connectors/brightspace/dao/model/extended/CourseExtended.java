package edu.iu.terracotta.connectors.brightspace.dao.model.extended;

import edu.iu.terracotta.connectors.brightspace.io.model.Course;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class CourseExtended extends LmsCourse {

    @Builder.Default private Course course = Course.builder().build();

    @Override
    public String getId() {
        return course.getIdentifier();
    }

    @Override
    public void setId(String id) {
        course.setIdentifier(id);
    }

    @Override
    public LmsCourse from() {
        LmsCourse lmsCourse = LmsCourse.builder().build();
        lmsCourse.setId(course.getIdentifier());
        lmsCourse.setType(CourseExtended.class);

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
