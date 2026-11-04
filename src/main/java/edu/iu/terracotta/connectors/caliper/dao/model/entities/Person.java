package edu.iu.terracotta.connectors.caliper.dao.model.entities;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import lombok.experimental.SuperBuilder;

/**
 * A person, such as the student or instructor an event is about.
 */
@SuperBuilder
public class Person extends CaliperEntity {

    @Override
    protected EntityType defaultType() {
        return EntityType.PERSON;
    }

}
