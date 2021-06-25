package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByExposure_ExposureId(Long exposureId);

    boolean existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(Long experimentId, Long exposureId, Long assignmentId);

    boolean existsByExposure_Experiment_ExperimentIdAndAssignmentId(Long experimentId, Long assignmentId);

    @Transactional
    @Modifying
    @Query("delete from Assignment s where s.assignmentId = ?1")
    void deleteByAssignmentId(Long submissionId);
}