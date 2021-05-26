package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExposureGroupConditionRepository extends JpaRepository<ExposureGroupCondition, Long> {
    Optional<Condition> getByGroup_GroupIdAndExposure_ExposureId(Long groupId, Long exposureId);

}