package edu.iu.terracotta.dao.model.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import org.imsglobal.caliper.entities.EntityType;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonDto extends AbstractDto{

    private String id;
    private EntityType type;

}
