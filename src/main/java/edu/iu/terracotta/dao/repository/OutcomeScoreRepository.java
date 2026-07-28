package edu.iu.terracotta.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.OutcomeScore;

import java.util.List;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface OutcomeScoreRepository extends JpaRepository<OutcomeScore, Long> {

    List<OutcomeScore> findByOutcome_OutcomeId(Long outcomeId);
    @Query("SELECT s FROM OutcomeScore s WHERE s.outcome.exposure.experiment.experimentId = :experimentId")
    List<OutcomeScore> findByOutcome_Exposure_Experiment_ExperimentId(@Param("experimentId") Long experimentId);
    Page<OutcomeScore> findByOutcome_Exposure_Experiment_ExperimentId(Long experimentId, Pageable pageable);
    OutcomeScore findByOutcomeScoreId(Long outcomeScoreId);
    boolean existsByOutcome_OutcomeIdAndOutcomeScoreId(Long outcomeId, Long outcomeScoreId);

    @Modifying
    @Transactional
    @Query("delete from OutcomeScore s where s.outcomeScoreId = ?1")
    void deleteByOutcomeScoreId(Long outcomeScoreId);

}
