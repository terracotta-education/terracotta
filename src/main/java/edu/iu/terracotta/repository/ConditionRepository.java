package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface ConditionRepository extends JpaRepository<Condition, Long> {

    List<Condition> findByExperiment_ExperimentId(Long experimentId);

    long countByExperiment_ExperimentId(Long experimentId);

    Condition findByConditionId(Long conditionId);

    boolean existsByExperiment_ExperimentIdAndConditionId(Long experimentId, Long conditionId);

    boolean existsByNameAndExperiment_ExperimentIdAndConditionIdIsNot(String name, Long experimentId, Long conditionId);

    boolean existsByConditionIdAndDefaultCondition(Long conditionId, Boolean defaultCondition);

    List<Condition> findByNameAndExperiment_ExperimentIdAndConditionIdIsNot(String name, Long experimentId,Long conditionId);

    @Transactional
    void deleteByConditionId(Long conditionId);

}
