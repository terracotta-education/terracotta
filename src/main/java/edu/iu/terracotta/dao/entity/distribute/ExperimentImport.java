package edu.iu.terracotta.dao.entity.distribute;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
@Table(name = "terr_experiment_import")
public class ExperimentImport extends BaseUuidEntity {

    public static final String ZIP_FILE_PREFIX = "terracotta_experiment_export-";
    public static final String JSON_FILE_NAME = "experiment.json";
    public static final String CONSENT_FILE_NAME = "consent.pdf";
    public static final String EXPERIMENT_TITLE_PREFIX = "(Imported)";
    public static final String ERROR_MESSAGE_SEPARATOR = ":::";

    @Column private String sourceTitle;
    @Column private String importedTitle;
    @Column private String fileName;
    @Column private String fileUri;

    @Column
    @Enumerated(EnumType.STRING)
    private ExperimentImportStatus status;

    @ManyToOne
    @JoinColumn(
        name = "owner_id",
        nullable = false
    )
    private LtiUserEntity owner;

    @ManyToOne
    @JoinColumn(
        name = "context_id",
        nullable = false
    )
    private LtiContextEntity context;

    @Builder.Default
    @OneToMany(mappedBy = "experimentImport")
    private List<ExperimentImportError> errors = new ArrayList<>();

    public void addErrorMessage(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }

        errors.add(
            ExperimentImportError.builder()
                .text(error)
                .build()
        );
    }

    @Transient
    public boolean isDeleted() {
        return List.of(
                ExperimentImportStatus.DELETED,
                ExperimentImportStatus.DELETION_ERROR
            )
            .contains(status);
    }

}
