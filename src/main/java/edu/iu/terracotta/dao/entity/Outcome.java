package edu.iu.terracotta.dao.entity;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.dao.model.enums.LmsType;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "terr_outcome")
public class Outcome extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "outcome_id",
        nullable = false
    )
    private Long outcomeId;

    @Column private String title;
    @Column private Boolean external;
    @Column private String lmsOutcomeId;
    @Column private Float maxPoints;

    @Column
    @Enumerated(EnumType.STRING)
    private LmsType lmsType;

    @OneToMany(mappedBy = "outcome")
    private List<OutcomeScore> outcomeScores;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "exposure_exposure_id",
        nullable = false
    )
    private Exposure exposure;

}
