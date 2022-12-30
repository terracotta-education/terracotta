package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.OutcomeScore;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OutcomeScoreRepository extends JpaRepository<OutcomeScore, Long> {
    List<OutcomeScore> findByOutcome_OutcomeId(Long outcomeId);

    List<OutcomeScore> findByOutcome_Exposure_Experiment_ExperimentId(Long experimentId);

    Page<OutcomeScore> findByOutcome_Exposure_Experiment_ExperimentId(Long experimentId, Pageable pageable);

    OutcomeScore findByOutcomeScoreId(Long outcomeScoreId);

    boolean existsByOutcome_OutcomeIdAndOutcomeScoreId(Long outcomeId, Long outcomeScoreId);

    @Transactional
    @Modifying
    @Query("delete from OutcomeScore s where s.outcomeScoreId = ?1")
    void deleteByOutcomeScoreId(Long outcomeScoreId);
}