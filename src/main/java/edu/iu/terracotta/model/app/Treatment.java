package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Table(name = "terr_treatment")
@Entity
public class Treatment extends BaseEntity {
    @Column(name = "treatment_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long treatmentId;

    @JoinColumn(name = "condition_condition_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Condition condition;

    @JoinColumn(name = "assessment_assessment_id")
    @OneToOne
    private Assessment assessment;

    @JoinColumn(name = "assignment_assignment_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Assignment assignment;

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public Long getTreatmentId() { return treatmentId; }

    public void setTreatmentId(Long treatmentId) { this.treatmentId = treatmentId; }

    public Assessment getAssessment() { return assessment; }

    public void setAssessment(Assessment assessment) { this.assessment = assessment; }

    public Condition getCondition() { return condition; }

    public void setCondition(Condition condition) { this.condition = condition; }

}