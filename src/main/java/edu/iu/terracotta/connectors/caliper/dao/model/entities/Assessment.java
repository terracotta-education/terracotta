package edu.iu.terracotta.connectors.caliper.dao.model.entities;

import com.fasterxml.jackson.annotation.JsonInclude;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * A Terracotta assessment. A max attempts or max score of 0 is left out, as Caliper does for unset values.
 */
@Getter
@SuperBuilder
public class Assessment extends CaliperEntity {

    @JsonInclude(JsonInclude.Include.NON_DEFAULT) private final int maxAttempts;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT) private final double maxScore;
    private final String version;

    @Override
    protected EntityType defaultType() {
        return EntityType.ASSESSMENT;
    }

}
