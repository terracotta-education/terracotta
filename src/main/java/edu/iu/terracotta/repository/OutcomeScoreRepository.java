package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.OutcomeScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutcomeScoreRepository extends JpaRepository<OutcomeScore, Long> {
    List<OutcomeScore> findByOutcome_OutcomeId(Long outcomeId);

    boolean existsByOutcome_OutcomeIdAndOutcomeScoreId(Long outcomeId, Long outcomeScoreId);
}