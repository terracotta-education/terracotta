package edu.iu.terracotta.connectors.caliper.dao.model.entities;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * The LTI launch an event happened in.
 */
@Getter
@SuperBuilder
public class LtiSession extends CaliperEntity {

    private final Map<String, Object> messageParameters;

    @Override
    protected EntityType defaultType() {
        return EntityType.LTI_SESSION;
    }

}
