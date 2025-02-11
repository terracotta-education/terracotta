package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "lti_tool_deployment")
public class ToolDeployment extends BaseEntity {

    @Id
    @Column(name = "deployment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long deploymentId;

    @Column(nullable = false)
    private String ltiDeploymentId;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(
        name = "key_id",
        nullable = false
    )
    private PlatformDeployment platformDeployment;

    @JsonIgnore
    @OneToMany(
        mappedBy = "toolDeployment",
        fetch = FetchType.LAZY
    )
    private Set<LtiContextEntity> contexts;

}
