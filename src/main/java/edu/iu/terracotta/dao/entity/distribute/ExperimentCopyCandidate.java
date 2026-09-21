package edu.iu.terracotta.dao.entity.distribute;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyCandidateStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Staged pending, one row per (source Experiment, destination course) pair - created when a
 * Canvas "course copy" PNS notice identifies a course Terracotta has an Experiment in as the
 * origin of a newly copied course (see LtiNoticeServiceImpl/ExperimentCopyCandidateServiceImpl).
 * Surfaced to the instructor on their first real launch into the destination course, so they can
 * choose whether to recreate the source Experiment there via the existing export/import pipeline.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(
    name = "terr_experiment_copy_candidate",
    uniqueConstraints = @UniqueConstraint(columnNames = {"source_experiment_id", "destination_context_id"})
)
public class ExperimentCopyCandidate extends BaseUuidEntity {

    @Enumerated(EnumType.STRING)
    private ExperimentCopyCandidateStatus status;

    // set once importCandidate() has kicked off the real (async) import, so the UI can map this
    // candidate to that import request's own progress/poll if it wants to later - the import's
    // own success/failure is tracked entirely by the existing ExperimentImport row, not here
    private UUID resultingImportUuid;

    @ManyToOne
    @JoinColumn(
        name = "source_experiment_id",
        nullable = false
    )
    private Experiment sourceExperiment;

    @ManyToOne
    @JoinColumn(
        name = "destination_context_id",
        nullable = false
    )
    private LtiContextEntity destinationContext;

}
