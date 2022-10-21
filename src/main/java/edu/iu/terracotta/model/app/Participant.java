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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "terr_participant",
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

}
