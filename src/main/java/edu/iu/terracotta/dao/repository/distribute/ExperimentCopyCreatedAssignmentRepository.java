package edu.iu.terracotta.dao.repository.distribute;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCreatedAssignment;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ExperimentCopyCreatedAssignmentRepository extends JpaRepository<ExperimentCopyCreatedAssignment, Long> {

    List<ExperimentCopyCreatedAssignment> findAllByCopyCandidate_Id(long copyCandidateId);

}
