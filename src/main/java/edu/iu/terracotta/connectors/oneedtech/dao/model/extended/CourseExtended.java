package edu.iu.terracotta.connectors.oneedtech.dao.model.extended;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import edu.iu.terracotta.connectors.oneedtech.dao.model.lms.Course;
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
    public Long getId() {
        return 1L;
    }

    @Override
    public LmsCourse from() {
        LmsCourse convertedEntity = (LmsCourse) this;
        convertedEntity.setType(Course.class);

        return convertedEntity;
    }

}
