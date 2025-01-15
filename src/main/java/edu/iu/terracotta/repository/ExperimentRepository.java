package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    List<Experiment> findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextIdAndCreatedBy_UserKey(long keyId, long contextId, String userKey);
    List<Experiment> findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(long keyId, long contextId);
    Experiment findByExperimentId(Long experimentId);
    Optional<Experiment> findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextIdAndExperimentId(long keyId, long contextId, Long experimentId);
    boolean existsByExperimentIdAndPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(Long experimentId, long keyId, long contextId);
    Optional<Experiment> findByExperimentIdAndPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(long experimentId, long keyId, long contextId);
    boolean existsByTitleAndLtiContextEntity_ContextIdAndExperimentIdIsNot(String title, long contextId, Long experimentId);

    @Modifying
    @Transactional
    @Query("delete from Experiment e where e.experimentId = ?1")
    void deleteByExperimentId(Long experimentId);

}