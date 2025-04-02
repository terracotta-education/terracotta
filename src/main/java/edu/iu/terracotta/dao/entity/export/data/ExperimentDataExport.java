package edu.iu.terracotta.dao.entity.export.data;

import java.beans.Transient;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "terr_experiment_data_export")
public class ExperimentDataExport extends BaseUuidEntity {

    public static final String FILE_NAME = "Terracotta_Experiment_%s_Data_Export_(%s).zip";
    public static final String COMPRESSED_FILE_EXTENSION = ".zip";
    public static final String MIME_TYPE = "application/zip";

    @Column private String fileName;
    @Column private String fileUri;
    @Column private String mimeType;
    @Column private String encryptionPhrase;
    @Column private String encryptionMethod;

    @Column
    @Enumerated(EnumType.STRING)
    private ExperimentDataExportStatus status;

    @ManyToOne
    @JoinColumn(
        name = "experiment_id",
        nullable = false
    )
    private Experiment experiment;

    @ManyToOne
    @JoinColumn(
        name = "owner_id",
        nullable = false
    )
    private LtiUserEntity owner;

    @Transient
    public long getExperimentId() {
        return experiment.getExperimentId();
    }

    @Transient
    public String getExperimentTitle() {
        return experiment.getTitle();
    }

}
