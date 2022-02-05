package edu.iu.terracotta.model.app.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.imsglobal.caliper.entities.EntityType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaLocationDto extends AbstractDto {
    private EntityType type;
    private String currentTime;

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }
}
