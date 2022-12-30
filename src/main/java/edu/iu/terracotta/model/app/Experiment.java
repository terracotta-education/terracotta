package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "terr_experiment")
public class Experiment extends BaseEntity {

    @Column(name = "experiment_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long experimentId;

    @JoinColumn(name = "platform_deployment_key_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PlatformDeployment platformDeployment;

    @JoinColumn(name = "lti_context_entity_context_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LtiContextEntity ltiContextEntity;

    @Column(name = "title")
    private String title;

    @Lob
    @Column(name = "description")
    private String description;

    @Enumerated
    @Column(name = "exposure_type")
    private ExposureTypes exposureType;

    @Enumerated
    @Column(name = "participation_type")
    private ParticipationTypes participationType;

    @Enumerated
    @Column(name = "distribution_type")
    private DistributionTypes distributionType;

    @Column(name = "started")
    private Timestamp started;

    @JoinColumn(name = "experiment_experiment_id")
    @OneToMany
    private List<Condition> conditions;

    @JoinColumn(name = "experiment_experiment_id")
    @OneToMany
    private List<Exposure> exposures;

    @JoinColumn(name = "experiment_experiment_id")
    @OneToMany
    private List<Participant> participants;

    @JoinColumn(name = "consent_document_consent_document_id")
    @OneToOne
    private ConsentDocument consentDocument;

    @JoinColumn(name = "created_by")
    @ManyToOne
    private LtiUserEntity createdBy;

    @Column(name = "closed")
    private Timestamp closed;

    public LtiUserEntity getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(LtiUserEntity createdBy) {
        this.createdBy = createdBy;
    }

    public ConsentDocument getConsentDocument() {
        return consentDocument;
    }

    public void setConsentDocument(ConsentDocument consentDocument) {
        this.consentDocument = consentDocument;
    }

    public Timestamp getStarted() {
        return started;
    }

    public void setStarted(Timestamp started) {
        this.started = started;
    }

    public DistributionTypes getDistributionType() {
        return distributionType;
    }

    public void setDistributionType(DistributionTypes distributionType) {
        this.distributionType = distributionType;
    }

    public ParticipationTypes getParticipationType() {
        return participationType;
    }

    public void setParticipationType(ParticipationTypes participationType) {
        this.participationType = participationType;
    }

    public ExposureTypes getExposureType() {
        return exposureType;
    }

    public void setExposureType(ExposureTypes exposureType) {
        this.exposureType = exposureType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public LtiContextEntity getLtiContextEntity() {
        return ltiContextEntity;
    }

    public void setLtiContextEntity(LtiContextEntity ltiContextEntity) {
        this.ltiContextEntity = ltiContextEntity;
    }

    public PlatformDeployment getPlatformDeployment() {
        return platformDeployment;
    }

    public void setPlatformDeployment(PlatformDeployment platformDeployment) { this.platformDeployment = platformDeployment; }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public List<Exposure> getExposures() { return exposures; }

    public void setExposures(List<Exposure> exposures) { this.exposures = exposures; }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

    public Timestamp getClosed() {
        return closed;
    }

    public void setClosed(Timestamp closed) {
        this.closed = closed;
    }
}
