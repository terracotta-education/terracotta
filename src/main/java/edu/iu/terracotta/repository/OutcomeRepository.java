package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Outcome;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OutcomeRepository extends JpaRepository<Outcome, Long> {
    List<Outcome> findByExposure_ExposureId(Long exposureId);

    List<Outcome> findByExposure_Experiment_ExperimentId(Long experimentId);

    Page<Outcome> findByExposure_Experiment_ExperimentId(Long experimentId, Pageable pageable);

    Outcome findByOutcomeId(Long outcomeId);

    boolean existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOutcomeId(Long experimentId, Long exposureId, Long outcomeId);

    @Transactional
    @Modifying
    @Query("delete from Outcome s where s.outcomeId = ?1")
    void deleteByOutcomeId(Long outcomeId);
}