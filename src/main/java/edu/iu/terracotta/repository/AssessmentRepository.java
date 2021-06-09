package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByTreatment_TreatmentId(Long treatmentId);

    boolean existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);

}
