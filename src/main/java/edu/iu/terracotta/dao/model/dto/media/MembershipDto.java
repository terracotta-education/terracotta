package edu.iu.terracotta.dao.model.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import edu.iu.terracotta.connectors.caliper.dao.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MembershipDto extends AbstractDto {

    private EntityType   type;
    private String   member;
    private String organization;
    private String[] roles;
    private Status active;
    private Instant dateCreated;

}
