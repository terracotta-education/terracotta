package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "terr_condition")
public class Condition extends BaseEntity {

    @Id
    @Column(name = "condition_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conditionId;

    @Column
    private String name;

    @Column
    private Boolean defaultCondition;

    @Column
    private Float distributionPct;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "experiment_experiment_id", nullable = false)
    private Experiment experiment;

}
