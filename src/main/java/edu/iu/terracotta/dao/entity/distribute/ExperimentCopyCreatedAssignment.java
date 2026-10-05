package edu.iu.terracotta.dao.entity.distribute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An LMS assignment a copy candidate's recreation created (rather than re-pointed), saved the
 * moment the LMS created it - outside the import's own transaction, so it survives that
 * transaction being rolled back or the server stopping mid-import. A retry deletes these first,
 * so it doesn't leave the course with duplicates (see ExperimentCopyCandidateServiceImpl).
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "terr_experiment_copy_created_assignment")
public class ExperimentCopyCreatedAssignment extends BaseUuidEntity {

    @ManyToOne
    @JoinColumn(
        name = "copy_candidate_id",
        nullable = false
    )
    private ExperimentCopyCandidate copyCandidate;

    private String lmsAssignmentId;

}
