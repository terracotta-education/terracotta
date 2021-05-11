package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    List<Experiment>
    findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(
          long keyId, long contextId);

  Optional<Experiment> findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextIdAndExperimentId(
      long keyId, long contextId, Long experimentId);
}