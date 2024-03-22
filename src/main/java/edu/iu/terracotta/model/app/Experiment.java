package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "terr_experiment")
public class Experiment extends BaseEntity {

    @Id
    @Column(name = "experiment_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long experimentId;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "platform_deployment_key_id", nullable = false)
    private PlatformDeployment platformDeployment;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "lti_context_entity_context_id", nullable = false)
    private LtiContextEntity ltiContextEntity;

    @Column
    private String title;

    @Lob
    @Column
    private String description;

    @Column
    @Enumerated
    private ExposureTypes exposureType;

    @Column
    @Enumerated
    private ParticipationTypes participationType;

    @Column
    @Enumerated
    private DistributionTypes distributionType;

    @Column
    private Timestamp started;

    @OneToMany
    @JoinColumn(name = "experiment_experiment_id")
    private List<Condition> conditions;

    @OneToMany
    @JoinColumn(name = "experiment_experiment_id")
    private List<Exposure> exposures;

    @OneToMany
    @JoinColumn(name = "experiment_experiment_id")
    private List<Participant> participants;

    @OneToOne
    @JoinColumn(name = "consent_document_consent_document_id")
    private ConsentDocument consentDocument;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private LtiUserEntity createdBy;

    @Column
    private Timestamp closed;

    @Transient
    public boolean isStarted() {
        return started != null;
    }

    @Transient
    public boolean canSetExposureType() {
        return ExposureTypes.NOSET == exposureType;
    }

}
