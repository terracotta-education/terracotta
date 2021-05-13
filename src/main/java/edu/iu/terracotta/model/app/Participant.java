package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.LtiUserEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Table(name = "terr_participant")
@Entity
public class Participant {
    @Column(name = "participant_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long participantId;

    @JoinColumn(name = "experiment_experiment_id", nullable = false)
    @ManyToOne(optional = false)
    private Experiment experiment;

    @JoinColumn(name = "lti_user_entity_user_id", nullable = false)
    @OneToOne(optional = false)
    private LtiUserEntity ltiUserEntity;

    public LtiUserEntity getLtiUserEntity() {
        return ltiUserEntity;
    }

    public void setLtiUserEntity(LtiUserEntity ltiUserEntity) {
        this.ltiUserEntity = ltiUserEntity;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }
}