package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.Group;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(name = "terr_exposure_group_condition",
        uniqueConstraints={
                @UniqueConstraint(columnNames = {"condition_condition_id", "group_group_id", "exposure_exposure_id"})}
        )
@Entity
public class ExposureGroupCondition extends BaseEntity {

    // ID
    @Column(name = "exposure_group_condition_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long exposureGroupConditionId;

    @JoinColumn(name = "condition_condition_id", nullable = false)
    @ManyToOne(optional = false)
    private Condition condition;

    @JoinColumn(name = "group_group_id", nullable = false)
    @ManyToOne(optional = false)
    private Group group;

    @JoinColumn(name = "exposure_exposure_id", nullable = false)
    @ManyToOne(optional = false)
    private Exposure exposure;

    public Exposure getExposure() {
        return exposure;
    }

    public void setExposure(Exposure exposure) {
        this.exposure = exposure;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }
}