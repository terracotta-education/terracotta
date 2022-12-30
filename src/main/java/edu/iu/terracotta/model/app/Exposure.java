package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Entity;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.GenerationType;
import javax.persistence.GeneratedValue;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.List;

@Table(name = "terr_exposure")
@Entity
public class Exposure extends BaseEntity {

    // ID
    @Column(name = "exposure_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long exposureId;

    // Experiment ID
    @JoinColumn(name = "experiment_experiment_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Experiment experiment;

    // Title
    @Column(name = "title")
    private String title;

    @OneToMany(mappedBy = "exposure")
    private List<Outcome> outcomes;


    //methods
    public Long getExposureId() { return exposureId; }

    public void setExposureId(Long exposureId) { this.exposureId = exposureId; }

    public Experiment getExperiment() { return experiment; }

    public void setExperiment(Experiment experiment) { this.experiment = experiment; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public List<Outcome> getOutcomes() { return outcomes; }

    public void setOutcomes(List<Outcome> outcomes) { this.outcomes = outcomes; }
}