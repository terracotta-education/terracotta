package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.sql.Timestamp;

@Table(name = "terr_participant", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "experiment_experiment_id", "lti_user_entity_user_id" }),
        @UniqueConstraint(columnNames = { "experiment_experiment_id", "lti_membership_entity_membership_id" })
})
@Entity
public class Participant extends BaseEntity {
    @Column(name = "participant_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantId;

    @JoinColumn(name = "experiment_experiment_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Experiment experiment;

    @JoinColumn(name = "lti_user_entity_user_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LtiUserEntity ltiUserEntity;

    @JoinColumn(name = "lti_membership_entity_membership_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LtiMembershipEntity ltiMembershipEntity;

    @Column(name = "consent")
    private Boolean consent;

    @Column(name = "date_given")
    private Timestamp dateGiven;

    @Column(name = "date_revoked")
    private Timestamp dateRevoked;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private ParticipationTypes source;

    @Column(name = "dropped")
    private Boolean dropped;

    @JoinColumn(name = "group_group_id")
    @OneToOne
    private Group group;

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public LtiUserEntity getLtiUserEntity() {
        return ltiUserEntity;
    }

    public void setLtiUserEntity(LtiUserEntity ltiUserEntity) {
        this.ltiUserEntity = ltiUserEntity;
    }

    public LtiMembershipEntity getLtiMembershipEntity() {
        return ltiMembershipEntity;
    }

    public void setLtiMembershipEntity(LtiMembershipEntity ltiMembershipEntity) {
        this.ltiMembershipEntity = ltiMembershipEntity;
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

    public Boolean getConsent() { return consent; }

    public void setConsent(Boolean consent) { this.consent = consent; }

    public Timestamp getDateGiven() { return dateGiven; }

    public void setDateGiven(Timestamp dateGiven) { this.dateGiven = dateGiven; }

    public Timestamp getDateRevoked() { return dateRevoked; }

    public void setDateRevoked(Timestamp dateRevoked) { this.dateRevoked = dateRevoked; }

    public ParticipationTypes getSource() { return source; }

    public void setSource(ParticipationTypes source) { this.source = source; }

    public Boolean getDropped() {
        return dropped;
    }

    public void setDropped(Boolean dropped) {
        this.dropped = dropped;
    }
}
