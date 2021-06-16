package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExposureGroupConditionRepository extends JpaRepository<ExposureGroupCondition, Long> {
    Optional<Condition> getByGroup_GroupIdAndExposure_ExposureId(Long groupId, Long exposureId);

    List<ExposureGroupCondition> findByCondition_Experiment_ExperimentId(Long experimentId);

    List<ExposureGroupCondition> findByExposure_ExposureId(Long exposureId);

    void deleteByExposure_Experiment_ExperimentId(Long experimentId);



}