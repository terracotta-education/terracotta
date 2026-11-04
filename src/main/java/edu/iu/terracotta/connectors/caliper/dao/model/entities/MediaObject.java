package edu.iu.terracotta.connectors.caliper.dao.model.entities;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * A video or other media a participant interacted with.
 */
@Getter
@SuperBuilder
public class MediaObject extends CaliperEntity {

    private final String mediaType;
    private final String duration;

    @Override
    protected EntityType defaultType() {
        return EntityType.MEDIA_OBJECT;
    }

}
