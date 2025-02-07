package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Group;

import java.util.List;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByExperiment_ExperimentId(Long experimentId);
    Group findByGroupId(Long groupId);
    boolean existsByExperiment_ExperimentIdAndGroupId(Long experimentId, Long groupId);
    long countByExperiment_ExperimentId(Long experimentId);
    void deleteByExperiment_ExperimentId(Long experimentId);

    @Modifying
    @Transactional
    @Query("delete from Group s where s.groupId = ?1")
    void deleteByGroupId(Long groupId);

}
