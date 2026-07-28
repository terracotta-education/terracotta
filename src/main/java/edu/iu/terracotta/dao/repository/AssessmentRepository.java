package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Assessment;

import java.util.Collection;
import java.util.List;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByTreatment_TreatmentId(Long treatmentId);
    Assessment findByAssessmentId(Long assessmentId);
    List<Assessment> findByTreatment_Assignment_AssignmentId(Long assignmentId);
    List<Assessment> findByTreatment_Assignment_AssignmentIdIn(Collection<Long> assignmentIds);
    boolean existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);

    @Transactional
    void deleteByAssessmentId(Long assessmentId);

}
