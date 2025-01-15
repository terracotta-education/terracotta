package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Treatment;

import java.util.List;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    List<Treatment> findByCondition_ConditionId(Long conditionId);
    List<Treatment> findByCondition_ConditionIdAndAssignment_AssignmentId(Long conditionId, Long assignmentId);
    List<Treatment> findByCondition_Experiment_ExperimentId(Long experimentId);
    List<Treatment> findByAssignment_AssignmentId(Long assignmentId);
    Treatment findByTreatmentId(Long treatmentId);
    boolean existsByCondition_Experiment_ExperimentIdAndCondition_ConditionIdAndTreatmentId(Long experimentId, Long conditionId, Long treatmentId);
    boolean existsByAssignment_AssignmentIdAndCondition_ConditionId(Long assignmentId, Long conditionId);

    @Modifying
    @Transactional
    @Query("delete from Treatment s where s.treatmentId = ?1")
    void deleteByTreatmentId(Long treatmentId);

}
