package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByExperiment_ExperimentId(Long experimentId);

    Optional<Group> findByGroupId(Long groupId);

    boolean existsByExperiment_ExperimentIdAndGroupId(Long experimentId, Long groupId);

    long countByExperiment_ExperimentId(Long experimentId);

    void deleteByExperiment_ExperimentId(Long experimentId);

    @Transactional
    @Modifying
    @Query("delete from Group s where s.groupId = ?1")
    void deleteByGroupId(Long groupId);


}