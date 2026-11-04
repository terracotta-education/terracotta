package edu.iu.terracotta.connectors.caliper.dao.model.entities;

import com.fasterxml.jackson.annotation.JsonInclude;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * A participant's attempt at an assessment: one Terracotta submission. A count of 0 is left out.
 */
@Getter
@SuperBuilder
public class Attempt extends CaliperEntity {

    private final Assessment assignable;
    private final Person assignee;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT) private final int count;
    private final Instant startedAtTime;
    private final Instant endedAtTime;

    @Override
    protected EntityType defaultType() {
        return EntityType.ATTEMPT;
    }

}
