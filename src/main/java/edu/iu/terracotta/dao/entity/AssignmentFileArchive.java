package edu.iu.terracotta.dao.entity;

import java.beans.Transient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.model.enums.AssignmentFileArchiveStatus;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "terr_assignment_file_archive")
public class AssignmentFileArchive extends BaseUuidEntity {

    public static final String COMPRESSED_FILE_EXTENSION = ".zip";
    public static final String MIME_TYPE = "application/zip";

    @Column private String fileName;
    @Column private String fileUri;
    @Column private  String mimeType;
    @Column private String encryptionPhrase;
    @Column private String encryptionMethod;

    @Column
    @Enumerated(EnumType.STRING)
    private AssignmentFileArchiveStatus status;

    @ManyToOne
    @JoinColumn(
        name = "assignment_id",
        nullable = false
    )
    private Assignment assignment;

    @ManyToOne
    @JoinColumn(
        name = "owner_id",
        nullable = false
    )
    private LtiUserEntity owner;

    @Transient
    public long getAssignmentId() {
        return assignment.getAssignmentId();
    }

    @Transient
    public String getAssignmentTitle() {
        return assignment.getTitle();
    }

    @Transient
    public Experiment getExperiment() {
        return assignment.getExposure().getExperiment();
    }

    @Transient
    public long getExperimentId() {
        return getExperiment().getExperimentId();
    }

    @Transient
    public String getExperimentTitle() {
        return getExperiment().getTitle();
    }

}
