package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.app.enumerator.LmsType;
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

@Table(name = "terr_outcome")
@Entity
public class Outcome extends BaseEntity {
    @Column(name = "outcome_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outcomeId;

    @JoinColumn(name = "exposure_exposure_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Exposure exposure;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "lms_type")
    private LmsType lmsType;

    @Column(name = "external")
    private Boolean external;

    @Column(name = "lms_outcome_id")
    private String lmsOutcomeId;

    @Column(name = "max_points")
    private Float maxPoints;

    @OneToMany(mappedBy = "outcome")
    private List<OutcomeScore> outcomeScores;


    public Exposure getExposure() { return exposure; }

    public void setExposure(Exposure exposure) { this.exposure = exposure; }

    public Long getOutcomeId() { return outcomeId; }

    public void setOutcomeId(Long outcomeId) { this.outcomeId = outcomeId; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public LmsType getLmsType() { return lmsType; }

    public void setLmsType(LmsType lmsType) { this.lmsType = lmsType; }

    public Boolean getExternal() { return external; }

    public void setExternal(Boolean external) { this.external = external; }

    public String getLmsOutcomeId() { return lmsOutcomeId; }

    public void setLmsOutcomeId(String lmsOutcomeId) { this.lmsOutcomeId = lmsOutcomeId; }

    public Float getMaxPoints() { return maxPoints; }

    public void setMaxPoints(Float maxPoints) { this.maxPoints = maxPoints; }

    public List<OutcomeScore> getOutcomeScores() { return outcomeScores; }

    public void setOutcomeScores(List<OutcomeScore> outcomeScores) { this.outcomeScores = outcomeScores; }
}