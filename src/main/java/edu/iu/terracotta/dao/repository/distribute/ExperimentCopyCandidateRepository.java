package edu.iu.terracotta.dao.repository.distribute;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCandidate;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyCandidateStatus;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ExperimentCopyCandidateRepository extends JpaRepository<ExperimentCopyCandidate, Long> {

    List<ExperimentCopyCandidate> findAllByDestinationContext_ContextIdAndStatus(long destinationContextId, ExperimentCopyCandidateStatus status);
    boolean existsBySourceExperiment_ExperimentIdAndDestinationContext_ContextId(long sourceExperimentId, long destinationContextId);
    boolean existsByDestinationContext_ContextIdAndStatus(long destinationContextId, ExperimentCopyCandidateStatus status);

}
