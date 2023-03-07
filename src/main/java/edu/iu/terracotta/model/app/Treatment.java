package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "terr_treatment")
public class Treatment extends BaseEntity {

    @Id
    @Column(name = "treatment_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long treatmentId;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "condition_condition_id", nullable = false)
    private Condition condition;

    @OneToOne
    @JoinColumn(name = "assessment_assessment_id")
    private Assessment assessment;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "assignment_assignment_id", nullable = false)
    private Assignment assignment;

}
