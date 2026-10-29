package edu.iu.terracotta.connectors.caliper.dao.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * The fields every Caliper entity Terracotta sends can carry. Fields left unset are left out of the JSON.
 */
@Getter
@SuperBuilder
@JsonPropertyOrder({"id", "type", "name"})
public abstract class CaliperEntity {

    private final String id;
    @Getter(AccessLevel.NONE) private final EntityType type;
    private final String name;
    private final Instant dateCreated;
    @Builder.Default private final Map<String, Object> extensions = new HashMap<>();

    // an entity's own type, unless the builder was given another one
    @JsonProperty("type")
    public EntityType getType() {
        return type != null ? type : defaultType();
    }

    @JsonIgnore
    protected abstract EntityType defaultType();

}
