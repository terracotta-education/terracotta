package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Assignment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Assignment findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(Long experimentId, String lmsAssignmentId);

    Assignment findByAssignmentId(Long assignmentId);

    List<Assignment> findByExposure_ExposureIdAndSoftDeleted(Long exposureId, boolean softDeleted);

    List<Assignment> findByExposure_Experiment_ExperimentId(Long experimentId);

    boolean existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(Long experimentId, Long exposureId, Long assignmentId);

    boolean existsByExposure_Experiment_ExperimentIdAndAssignmentId(Long experimentId, Long assignmentId);

    Page<Assignment> findAll(Pageable pageable);

    @Transactional
    void deleteByAssignmentId(Long submissionId);

    @Query("select a from Assignment a where a.exposure.experiment.platformDeployment.keyId = ?1 and a.exposure.experiment.closed is null and a.assignmentId NOT IN (Select a2.assignmentId from Assignment a2 where a2.softDeleted = true)")
    List<Assignment> findAssignmentsToCheckByPlatform(long keyId);

    @Query("select a from Assignment a where a.exposure.experiment.ltiContextEntity.contextId = ?1 and a.exposure.experiment.closed is null and a.assignmentId NOT IN (Select a2.assignmentId from Assignment a2 where a2.softDeleted = true)")
    List<Assignment> findAssignmentsToCheckByContext(long contextId);

}
