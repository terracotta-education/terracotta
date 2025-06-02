package edu.iu.terracotta.dao.entity;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "terr_participant",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "experiment_experiment_id", "lti_user_entity_user_id" }),
        @UniqueConstraint(columnNames = { "experiment_experiment_id", "lti_membership_entity_membership_id" })
    }
)
public class Participant extends BaseUuidEntity {

    @Column private Boolean consent;
    @Column private Timestamp dateGiven;
    @Column private Timestamp dateRevoked;
    @Column private Boolean dropped;

    @Column
    @Enumerated(EnumType.STRING)
    private ParticipationTypes source;

    @OneToOne
    @JoinColumn(name = "group_group_id")
    private Group group;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "experiment_experiment_id",
        nullable = false
    )
    private Experiment experiment;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "lti_user_entity_user_id",
        nullable = false
    )
    private LtiUserEntity ltiUserEntity;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "lti_membership_entity_membership_id",
        nullable = false
    )
    private LtiMembershipEntity ltiMembershipEntity;

    @Transient
    public boolean isTestStudent() {
        return ltiUserEntity == null || ltiUserEntity.isTestStudent();
    }

    @Transient
    public Long getParticipantId() {
        return this.id;
    }

    @Transient
    public void setParticipantId(Long participantId) {
        this.id = participantId;
    }

}
