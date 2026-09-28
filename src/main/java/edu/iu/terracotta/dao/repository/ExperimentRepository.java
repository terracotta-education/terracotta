package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Experiment;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    List<Experiment> findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextIdAndCreatedBy_UserKey(long keyId, long contextId, String userKey);
    List<Experiment> findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(long keyId, long contextId);
    List<Experiment> findAllByLtiContextEntity_ContextId(long contextId);
    Experiment findByExperimentId(Long experimentId);
    Optional<Experiment> findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextIdAndExperimentId(long keyId, long contextId, Long experimentId);
    boolean existsByExperimentIdAndPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(Long experimentId, long keyId, long contextId);
    boolean existsByTitle(String title);
    Optional<Experiment> findByExperimentIdAndPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(long experimentId, long keyId, long contextId);
    boolean existsByTitleAndLtiContextEntity_ContextIdAndExperimentIdIsNot(String title, long contextId, Long experimentId);

    @Modifying
    @Transactional
    @Query("delete from Experiment e where e.experimentId = ?1")
    void deleteByExperimentId(Long experimentId);


    // one atomic statement, so concurrent first launches can't race each other to set it -
    // returns 1 for the launch that actually started the experiment, 0 for any other
    @Modifying
    @Query("UPDATE Experiment e SET e.started = ?2, e.updatedAt = ?2, e.version = e.version + 1 WHERE e.experimentId = ?1 AND e.started IS NULL")
    int markStarted(long experimentId, Timestamp started);

}