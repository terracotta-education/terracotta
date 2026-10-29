package edu.iu.terracotta.connectors.caliper.dao.model.entities;

import com.fasterxml.jackson.annotation.JsonInclude;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * The score a participant got on an attempt. Scores of 0 are left out, as Caliper does for unset values.
 */
@Getter
@SuperBuilder
public class Result extends CaliperEntity {

    private final Attempt attempt;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT) private final double maxResultScore;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT) private final double resultScore;
    private final String comment;

    @Override
    protected EntityType defaultType() {
        return EntityType.RESULT;
    }

}
