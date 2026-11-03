package edu.iu.terracotta.connectors.caliper.dao.model.entities;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import edu.iu.terracotta.connectors.caliper.dao.model.enums.Role;
import edu.iu.terracotta.connectors.caliper.dao.model.enums.Status;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * A person's membership in a course.
 */
@Getter
@SuperBuilder
public class Membership extends CaliperEntity {

    private final Person member;
    private final CourseSection organization;
    private final List<Role> roles;
    private final Status status;

    @Override
    protected EntityType defaultType() {
        return EntityType.MEMBERSHIP;
    }

}
