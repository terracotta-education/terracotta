package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Table(name = "terr_outcome_score")
@Entity
public class OutcomeScore extends BaseEntity {
    @Column(name = "outcome_score_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outcomeScoreId;

    @JoinColumn(name = "outcome_outcome_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Outcome outcome;

    @JoinColumn(name = "participant_participant_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Participant participant;

    @Column(name = "score_numeric")
    private Float scoreNumeric;


    public Long getOutcomeScoreId() { return outcomeScoreId; }

    public void setOutcomeScoreId(Long outcomeScoreId) { this.outcomeScoreId = outcomeScoreId; }

    public Outcome getOutcome() { return outcome; }

    public void setOutcome(Outcome outcome) { this.outcome = outcome; }

    public Participant getParticipant() { return participant; }

    public void setParticipant(Participant participant) { this.participant = participant; }

    public Float getScoreNumeric() { return scoreNumeric; }

    public void setScoreNumeric(Float scoreNumeric) { this.scoreNumeric = scoreNumeric; }
}