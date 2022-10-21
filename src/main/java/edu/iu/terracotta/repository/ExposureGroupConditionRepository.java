package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.ExposureGroupCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface ExposureGroupConditionRepository extends JpaRepository<ExposureGroupCondition, Long> {

    Optional<ExposureGroupCondition> getByGroup_GroupIdAndExposure_ExposureId(Long groupId, Long exposureId);

    List<ExposureGroupCondition> findByGroup_GroupId(Long groupId);

    List<ExposureGroupCondition> findByCondition_Experiment_ExperimentId(Long experimentId);

    List<ExposureGroupCondition> findByExposure_ExposureId(Long exposureId);

    void deleteByExposure_Experiment_ExperimentId(Long experimentId);

    Optional<ExposureGroupCondition> getByCondition_ConditionIdAndExposure_ExposureId(Long conditionId, Long exposureId);

    Optional<ExposureGroupCondition>  getByGroup_GroupIdAndCondition_ConditionId(Long groupId, Long conditionId);

}
