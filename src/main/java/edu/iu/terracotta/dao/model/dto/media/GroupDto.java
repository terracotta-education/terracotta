package edu.iu.terracotta.dao.model.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.imsglobal.caliper.entities.EntityType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupDto extends AbstractDto {

    private EntityType type;
    private String courseNumber;
    private String academicSession;

}
