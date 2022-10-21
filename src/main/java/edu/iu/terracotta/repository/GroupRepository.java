package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByExperiment_ExperimentId(Long experimentId);

    Group findByGroupId(Long groupId);

    boolean existsByExperiment_ExperimentIdAndGroupId(Long experimentId, Long groupId);

    long countByExperiment_ExperimentId(Long experimentId);

    void deleteByExperiment_ExperimentId(Long experimentId);

    @Transactional
    void deleteByGroupId(Long groupId);

}
