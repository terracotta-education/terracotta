package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Condition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConditionRepository extends JpaRepository<Condition, Long> {


    List<Condition> findByExperiment_ExperimentId(Long experimentId);

    boolean existsByExperiment_ExperimentIdAndConditionId(Long experimentId, Long conditionId);

}