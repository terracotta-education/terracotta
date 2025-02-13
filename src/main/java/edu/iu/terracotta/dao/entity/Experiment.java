package edu.iu.terracotta.dao.entity;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.dao.model.enums.DistributionTypes;
import edu.iu.terracotta.dao.model.enums.ExposureTypes;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "experiment_id",
        nullable = false
    )
    private Long experimentId;

    @Column private String title;
    @Column private Timestamp started;
    @Column private Timestamp closed;

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

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "platform_deployment_key_id",
        nullable = false
    )
    private PlatformDeployment platformDeployment;

    @OneToOne
    @JoinColumn(name = "consent_document_consent_document_id")
    private ConsentDocument consentDocument;

    @OneToMany
    @JoinColumn(name = "experiment_experiment_id")
    private List<Condition> conditions;

    @OneToMany
    @JoinColumn(name = "experiment_experiment_id")
    private List<Exposure> exposures;

    @OneToMany
    @JoinColumn(name = "experiment_experiment_id")
    private List<Participant> participants;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "lti_context_entity_context_id",
        nullable = false
    )
    private LtiContextEntity ltiContextEntity;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private LtiUserEntity createdBy;

    /**
     * Get particpants that are not test students
     *
     * @return
     */
    public List<Participant> getParticipants() {
        return participants.stream()
            .filter(participant -> !participant.getLtiUserEntity().isTestStudent())
            .toList();
    }

    @Transient
    public boolean isStarted() {
        return started != null;
    }

    @Transient
    public boolean canSetExposureType() {
        return ExposureTypes.NOSET == exposureType;
    }

    @Transient
    public boolean isSingleCondition() {
        return conditions.size() == 1;
    }

}
