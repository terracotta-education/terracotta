package edu.iu.terracotta.dao.model.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.imsglobal.caliper.entities.EntityType;
import org.imsglobal.caliper.entities.agent.Status;
import org.joda.time.DateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings({"PMD.LooseCoupling"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class MembershipDto extends AbstractDto {

    private EntityType   type;
    private String   member;
    private String organization;
    private String[] roles;
    private Status active;
    private DateTime dateCreated;

}
