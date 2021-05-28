package edu.iu.terracotta.model.app;

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
public class Treatment {
    @Column(name = "treatment_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long treatmentId;

    @JoinColumn(name = "condition_condition_id", nullable = false)
    @ManyToOne(optional = false)
    private Condition condition;

    @JoinColumn(name = "assessment_assessment_id")
    @OneToOne(orphanRemoval = true)
    private Assessment assessment;

    @Column(name = "treatment_order")
    private Integer treatmentOrder;


    public Long getTreatmentId() { return treatmentId; }

    public void setTreatmentId(Long treatmentId) { this.treatmentId = treatmentId; }

    public Assessment getAssessment() { return assessment; }

    public void setAssessment(Assessment assessment) { this.assessment = assessment; }

    public Condition getCondition() { return condition; }

    public void setCondition(Condition condition) { this.condition = condition; }

    public Integer getTreatmentOrder() { return treatmentOrder; }

    public void setTreatmentOrder(Integer treatmentOrder) { this.treatmentOrder = treatmentOrder; }
}