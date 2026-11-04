package edu.iu.terracotta.connectors.caliper.dao.model.entities;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * The point in a media object an event happened at.
 */
@Getter
@SuperBuilder
public class MediaLocation extends CaliperEntity {

    private final String currentTime;

    @Override
    protected EntityType defaultType() {
        return EntityType.MEDIA_LOCATION;
    }

}
