package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import java.sql.Timestamp;
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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
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
@Table(name = "lti_tool_deployment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolDeployment extends BaseEntity {

    @Id
    @Column(name = "deployment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long deploymentId;

    @Column(nullable = false)
    private String ltiDeploymentId;

    // whether the LTI Platform Notification Service course-copy handler has already been
    // registered with the platform for this deployment (see
    // CanvasAdvantageNoticeServiceImpl.ensureNoticeHandlerRegistered) - registration is an extra
    // outbound call to the platform, so this makes it a one-time cost per deployment instead of
    // repeating it on every launch
    @Column(nullable = false)
    private boolean noticeHandlerRegistered;

    // last time a notice handler registration attempt was made (success or failure) - lets
    // CanvasAdvantageNoticeServiceImpl back off for a cooldown period instead of retrying on
    // every single launch when the platform keeps rejecting the request (e.g. an institution's
    // Canvas Developer Key doesn't have the noticehandlers scope granted yet)
    private Timestamp noticeHandlerRegistrationAttemptedAt;

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
