package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.OutcomeScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OutcomeScoreRepository extends JpaRepository<OutcomeScore, Long> {
    List<OutcomeScore> findByOutcome_OutcomeId(Long outcomeId);

    List<OutcomeScore> findByOutcome_Exposure_Experiment_ExperimentId(Long experimentId);

    boolean existsByOutcome_OutcomeIdAndOutcomeScoreId(Long outcomeId, Long outcomeScoreId);

    @Transactional
    @Modifying
    @Query("delete from OutcomeScore s where s.outcomeScoreId = ?1")
    void deleteByOutcomeScoreId(Long outcomeScoreId);
}