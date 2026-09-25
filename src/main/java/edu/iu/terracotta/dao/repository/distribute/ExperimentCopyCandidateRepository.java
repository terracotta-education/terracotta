package edu.iu.terracotta.dao.repository.distribute;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCandidate;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyCandidateStatus;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ExperimentCopyCandidateRepository extends JpaRepository<ExperimentCopyCandidate, Long> {

    List<ExperimentCopyCandidate> findAllByDestinationContext_ContextIdAndStatus(long destinationContextId, ExperimentCopyCandidateStatus status);
    boolean existsBySourceExperiment_ExperimentIdAndDestinationContext_ContextId(long sourceExperimentId, long destinationContextId);
    boolean existsByDestinationContext_ContextIdAndStatus(long destinationContextId, ExperimentCopyCandidateStatus status);
    boolean existsByDestinationContext_ContextIdAndStatusIn(long destinationContextId, List<ExperimentCopyCandidateStatus> statuses);
    List<ExperimentCopyCandidate> findAllByDestinationContext_ContextIdAndStatusIn(long destinationContextId, List<ExperimentCopyCandidateStatus> statuses);
    List<ExperimentCopyCandidate> findAllByDestinationContext_ContextIdAndStatusInAndAcknowledgedAtIsNull(long destinationContextId, List<ExperimentCopyCandidateStatus> statuses);
    List<ExperimentCopyCandidate> findAllByStatusInAndUpdatedAtBefore(List<ExperimentCopyCandidateStatus> statuses, Timestamp updatedAt);
    List<ExperimentCopyCandidate> findAllByStatusAndAcknowledgedAtIsNullAndUpdatedAtBefore(ExperimentCopyCandidateStatus status, Timestamp updatedAt);

}
