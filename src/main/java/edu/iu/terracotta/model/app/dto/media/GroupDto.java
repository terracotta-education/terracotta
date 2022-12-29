package edu.iu.terracotta.model.app.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import org.imsglobal.caliper.entities.EntityType;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupDto extends AbstractDto {

    private EntityType type;
    private String courseNumber;
    private String academicSession;

}
