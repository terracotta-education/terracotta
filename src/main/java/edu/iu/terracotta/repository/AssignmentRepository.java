package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Exposure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByExposure_ExposureId(Long exposureId);

    boolean existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(Long experimentId, Long exposureId, Long assignmentId);

    boolean existsByExposure_Experiment_ExperimentIdAndAssignmentId(Long experimentId, Long assignmentId);


}