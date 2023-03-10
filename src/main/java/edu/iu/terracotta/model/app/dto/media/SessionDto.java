package edu.iu.terracotta.model.app.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import org.joda.time.DateTime;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionDto extends AbstractDto {

    private String type;
    private DateTime startedAtTime;

}
