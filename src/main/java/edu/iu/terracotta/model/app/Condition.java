package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;

import javax.persistence.*;

@Table(name = "terr_condition")
@Entity
public class Condition extends BaseEntity {

    // condition_id
    @Column(name = "condition_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conditionId;

    // name
    @Column(name = "name")
    private String name;

    // default_condition
    @Column(name = "default_condition")
    private Boolean defaultCondition;

    // distribution_pct
    @Column(name = "distribution_pct")
    private Float distributionPct;

    @JoinColumn(name = "experiment_experiment_id", nullable = false)
    @ManyToOne(optional = false)
    private Experiment experiment;


    //methods
    public Long getConditionId() { return conditionId; }

    public void setConditionId(Long id) { this.conditionId = id; }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Boolean getDefaultCondition() { return defaultCondition; }

    public void setDefaultCondition(Boolean defaultCondition) { this.defaultCondition = defaultCondition; }

    public Float getDistributionPct() { return distributionPct; }

    public void setDistributionPct(Float distributionPct) { this.distributionPct = distributionPct; }
}