package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Outcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface OutcomeRepository extends JpaRepository<Outcome, Long> {
    List<Outcome> findByExposure_ExposureId(Long exposureId);

    List<Outcome> findByExposure_Experiment_ExperimentId(Long experimentId);

    Outcome findByOutcomeId(Long outcomeId);

    boolean existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOutcomeId(Long experimentId, Long exposureId, Long outcomeId);

    @Transactional
    void deleteByOutcomeId(Long outcomeId);

}
