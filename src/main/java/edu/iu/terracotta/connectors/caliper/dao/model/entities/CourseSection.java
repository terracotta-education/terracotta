package edu.iu.terracotta.connectors.caliper.dao.model.entities;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import lombok.experimental.SuperBuilder;

/**
 * The course an event happened in.
 */
@SuperBuilder
public class CourseSection extends CaliperEntity {

    @Override
    protected EntityType defaultType() {
        return EntityType.COURSE_SECTION;
    }

}
