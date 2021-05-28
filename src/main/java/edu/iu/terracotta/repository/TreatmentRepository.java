package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    List<Treatment> findByCondition_ConditionId(Long conditionId);

    Optional<Treatment> findByTreatmentId(Long treatmentId);

    boolean existsByCondition_Experiment_ExperimentIdAndCondition_ConditionIdAndTreatmentId(Long experimentId, Long conditionId, Long treatmentId);


}