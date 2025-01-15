package edu.iu.terracotta.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.ExposureGroupCondition;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ExposureGroupConditionRepository extends JpaRepository<ExposureGroupCondition, Long> {

    Optional<ExposureGroupCondition> getByGroup_GroupIdAndExposure_ExposureId(Long groupId, Long exposureId);
    List<ExposureGroupCondition> findByGroup_GroupId(Long groupId);
    Page<ExposureGroupCondition> findByGroup_GroupId(Long groupId, Pageable pageable);
    List<ExposureGroupCondition> findByCondition_Experiment_ExperimentId(Long experimentId);
    List<ExposureGroupCondition> findByExposure_ExposureId(Long exposureId);
    void deleteByExposure_Experiment_ExperimentId(Long experimentId);
    Optional<ExposureGroupCondition> getByCondition_ConditionIdAndExposure_ExposureId(Long conditionId, Long exposureId);
    Optional<ExposureGroupCondition>  getByGroup_GroupIdAndCondition_ConditionId(Long groupId, Long conditionId);

}