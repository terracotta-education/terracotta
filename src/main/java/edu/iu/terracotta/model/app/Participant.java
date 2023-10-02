package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(
    name = "terr_participant",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "experiment_experiment_id", "lti_user_entity_user_id" }),
        @UniqueConstraint(columnNames = { "experiment_experiment_id", "lti_membership_entity_membership_id" })
    }
)
public class Participant extends BaseEntity {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantId;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "experiment_experiment_id", nullable = false)
    private Experiment experiment;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "lti_user_entity_user_id", nullable = false)
    private LtiUserEntity ltiUserEntity;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "lti_membership_entity_membership_id", nullable = false)
    private LtiMembershipEntity ltiMembershipEntity;

    @Column
    private Boolean consent;

    @Column
    private Timestamp dateGiven;

    @Column
    private Timestamp dateRevoked;

    @Column
    @Enumerated(EnumType.STRING)
    private ParticipationTypes source;

    @Column
    private Boolean dropped;

    @OneToOne
    @JoinColumn(name = "group_group_id")
    private Group group;

    @Transient
    public boolean isTestStudent() {
        if (ltiUserEntity == null) {
            return true;
        }

        return ltiUserEntity.isTestStudent();
    }

}
