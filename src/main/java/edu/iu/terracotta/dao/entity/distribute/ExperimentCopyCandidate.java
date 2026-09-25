package edu.iu.terracotta.dao.entity.distribute;

import java.sql.Timestamp;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyCandidateStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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
 * The source Experiment is then recreated in the destination course automatically, in the
 * background, via the existing export/import pipeline; this row records how that went.
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

    // how many times recreation has been started for this candidate - automatic recovery of a
    // recreation interrupted by a server restart gives up after a configured number
    private int attempts;

    // set once importCandidate() has kicked off the real (async) import, so the UI can map this
    // candidate to that import request's own progress/poll if it wants to later - the import's
    // own success/failure is tracked entirely by the existing ExperimentImport row, not here
    private UUID resultingImportUuid;

    // why recreation failed, when status is ERROR - recreation runs in the background with no
    // one watching, so this is the only record of what went wrong
    @Column(length = 1024)
    private String errorMessage;

    // set once an instructor has been shown the result of this recreation on their first launch
    // into the destination course, so it's only shown once
    private Timestamp acknowledgedAt;

    // which of the course's copied LMS assignments this candidate's recreation re-points, found
    // (by launch URL) the first time it runs and saved as JSON (see RepointPlan). A retry reuses
    // it instead of matching by URL again, since a re-point that already happened left the
    // assignment's URL pointing at an assignment the failed attempt rolled back.
    @Lob
    private String repointPlan;

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
