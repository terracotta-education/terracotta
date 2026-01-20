package edu.iu.terracotta.dao.entity.distribute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "terr_experiment_import_error")
public class ExperimentImportError extends BaseUuidEntity {

    @Column private String text;

    @ManyToOne(
        optional = false,
        cascade = CascadeType.ALL
    )
    @JoinColumn(
        name = "experiment_import_id",
        nullable = false
    )
    private ExperimentImport experimentImport;

}
