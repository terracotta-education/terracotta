package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Condition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConditionRepository extends JpaRepository<Condition, Long> {


    List<Condition> findByExperiment_ExperimentId(Long experimentId);

    Optional<Condition> findByConditionId(Long conditionId);

}