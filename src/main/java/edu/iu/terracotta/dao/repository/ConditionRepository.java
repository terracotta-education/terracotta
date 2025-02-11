package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Condition;

import java.util.List;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ConditionRepository extends JpaRepository<Condition, Long> {

    List<Condition> findByExperiment_ExperimentId(Long experimentId);
    long countByExperiment_ExperimentId(Long experimentId);
    Condition findByConditionId(Long conditionId);
    boolean existsByExperiment_ExperimentIdAndConditionId(Long experimentId, Long conditionId);
    boolean existsByNameAndExperiment_ExperimentIdAndConditionIdIsNot(String name, Long experimentId, Long conditionId);
    boolean existsByConditionIdAndDefaultCondition(Long conditionId, Boolean defaultCondition);
    List<Condition> findByNameAndExperiment_ExperimentIdAndConditionIdIsNot(String name, Long experimentId,Long conditionId);

    @Modifying
    @Transactional
    @Query("delete from Condition s where s.conditionId = ?1")
    void deleteByConditionId(Long conditionId);

}