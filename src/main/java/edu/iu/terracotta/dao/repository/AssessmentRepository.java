package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Assessment;

import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    Assessment findByUuid(UUID uuid);


    @Query("select e.assessmentId from Assessment e where e.uuid = ?1")

    Optional<Long> findIdByUuid(UUID uuid);

    @Query("select e.uuid from Assessment e where e.assessmentId = ?1")
    Optional<UUID> findUuidByAssessmentId(long assessmentId);

    List<Assessment> findByTreatment_TreatmentId(Long treatmentId);
    Assessment findByAssessmentId(Long assessmentId);
    List<Assessment> findByTreatment_Assignment_AssignmentId(Long assignmentId);
    List<Assessment> findByTreatment_Assignment_AssignmentIdIn(Collection<Long> assignmentIds);
    boolean existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);

    @Transactional
    void deleteByAssessmentId(Long assessmentId);

}
