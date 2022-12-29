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
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "terr_exposure_group_condition",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"condition_condition_id", "group_group_id", "exposure_exposure_id"})
    }
)
public class ExposureGroupCondition extends BaseEntity {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long exposureGroupConditionId;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "condition_condition_id", nullable = false)
    private Condition condition;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "group_group_id", nullable = false)
    private Group group;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "exposure_exposure_id", nullable = false)
    private Exposure exposure;

}
