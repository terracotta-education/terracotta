package edu.iu.terracotta.connectors.caliper.dao.model.entities;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import lombok.experimental.SuperBuilder;

/**
 * An application: Terracotta itself, or the LMS an event was referred from.
 */
@SuperBuilder
public class SoftwareApplication extends CaliperEntity {

    @Override
    protected EntityType defaultType() {
        return EntityType.SOFTWARE_APPLICATION;
    }

}
