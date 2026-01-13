package edu.iu.terracotta.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "terr_obsolete_assignment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObsoleteAssignment extends BaseUuidEntity {

    public static final String PREFIX = "OBSOLETE";
    public static final String URL = "obsolete/assignment";

    @Column private String lmsAssignmentId;
    @Column private String originalTitle;
    @Column private String originalUrl;

    @ManyToOne
    @JoinColumn(
        name = "lti_context_context_id",
        nullable = false
    )
    private LtiContextEntity context;

}
