package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ConditionRepository extends JpaRepository<Condition, Long> {


    List<Condition> findByExperiment_ExperimentId(Long experimentId);

    boolean existsByExperiment_ExperimentIdAndConditionId(Long experimentId, Long conditionId);

    @Transactional
    @Modifying
    @Query("delete from Condition s where s.conditionId = ?1")
    void deleteByConditionId(Long conditionId);

}