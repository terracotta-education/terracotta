package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.app.enumerator.LmsType;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "terr_outcome")
public class Outcome extends BaseEntity {

    @Id
    @Column(name = "outcome_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outcomeId;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "exposure_exposure_id", nullable = false)
    private Exposure exposure;

    @Column
    private String title;

    @Column
    @Enumerated(EnumType.STRING)
    private LmsType lmsType;

    @Column
    private Boolean external;

    @Column
    private String lmsOutcomeId;

    @Column
    private Float maxPoints;

    @OneToMany(mappedBy = "outcome")
    private List<OutcomeScore> outcomeScores;

}
