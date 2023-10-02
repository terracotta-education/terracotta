package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.app.enumerator.LmsType;
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
