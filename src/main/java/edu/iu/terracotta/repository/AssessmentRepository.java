package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByTreatment_TreatmentId(Long treatmentId);

    boolean existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);

}
