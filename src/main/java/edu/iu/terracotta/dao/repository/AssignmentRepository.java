package edu.iu.terracotta.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Assignment;

import java.util.List;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Assignment findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(Long experimentId, String lmsAssignmentId);
    Assignment findByAssignmentId(Long assignmentId);
    List<Assignment> findByExposure_ExposureIdAndSoftDeleted(Long exposureId, boolean softDeleted);
    List<Assignment> findByExposure_Experiment_ExperimentId(Long experimentId);
    boolean existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(Long experimentId, Long exposureId, Long assignmentId);
    boolean existsByExposure_Experiment_ExperimentIdAndAssignmentId(Long experimentId, Long assignmentId);
    Page<Assignment> findAll(Pageable pageable);
    List<Assignment> findAllByExposure_Experiment_PlatformDeployment_KeyId(long depoymentId);

    @Transactional
    void deleteByAssignmentId(Long submissionId);

    @Query("SELECT a FROM Assignment a WHERE a.exposure.experiment.platformDeployment.keyId = ?1 AND a.exposure.experiment.closed IS NULL AND a.assignmentId NOT IN (SELECT a2.assignmentId FROM Assignment a2 WHERE a2.softDeleted = true)")
    List<Assignment> findAssignmentsToCheckByPlatform(long keyId);

    @Query("SELECT a FROM Assignment a WHERE a.exposure.experiment.ltiContextEntity.contextId = ?1 AND a.exposure.experiment.closed IS NULL AND a.assignmentId NOT IN (SELECT a2.assignmentId FROM Assignment a2 WHERE a2.softDeleted = true)")
    List<Assignment> findAssignmentsToCheckByContext(long contextId);

}
