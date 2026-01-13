package edu.iu.terracotta.dao.entity.preview;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Treatment;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "terr_preview_treatment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TreatmentPreview extends BaseUuidEntity {

    @ManyToOne
    @JoinColumn(
        name = "treatment_id",
        nullable = false
    )
    private Treatment treatment;

    @ManyToOne
    @JoinColumn(
        name = "experiment_id",
        nullable = false
    )
    private Experiment experiment;

    @ManyToOne
    @JoinColumn(
        name = "condition_id",
        nullable = false
    )
    private Condition condition;

    @ManyToOne
    @JoinColumn(
        name = "owner_id",
        nullable = false
    )
    private LtiUserEntity owner;

}
