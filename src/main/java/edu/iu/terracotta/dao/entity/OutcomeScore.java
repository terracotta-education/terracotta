package edu.iu.terracotta.dao.entity;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "terr_outcome_score")
public class OutcomeScore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "outcome_score_id",
        nullable = false
    )
    private Long outcomeScoreId;

    @Column private Float scoreNumeric;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "outcome_outcome_id",
        nullable = false
    )
    private Outcome outcome;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "participant_participant_id",
        nullable = false
    )
    private Participant participant;

}
