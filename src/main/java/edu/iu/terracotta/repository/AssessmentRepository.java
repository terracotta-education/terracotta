package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByTreatment_TreatmentId(Long treatmentId);

    Assessment findByAssessmentId(Long assessmentId);

    List<Assessment> findByTreatment_Assignment_AssignmentId(Long assignmentId);

    boolean existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);

    @Transactional
    void deleteByAssessmentId(Long assessmentId);

}
