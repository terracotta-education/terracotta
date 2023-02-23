package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

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

@Entity
@Getter
@Setter
@Table(name = "terr_exposure")
public class Exposure extends BaseEntity {

    @Id
    @Column(name = "exposure_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long exposureId;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "experiment_experiment_id", nullable = false)
    private Experiment experiment;

    @Column
    private String title;

    @OneToMany(mappedBy = "exposure")
    private List<Outcome> outcomes;

}
