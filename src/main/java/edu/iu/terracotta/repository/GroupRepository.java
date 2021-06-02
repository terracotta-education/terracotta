package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByExperiment_ExperimentId(Long experimentId);

    Optional<Group> findByGroupId(Long groupId);

    boolean existsByExperiment_ExperimentIdAndGroupId(Long experimentId, Long groupId);

    long countByExperiment_ExperimentId(Long experimentId);

    long deleteByExperiment_ExperimentId(Long experimentId);




}