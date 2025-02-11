package edu.iu.terracotta.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Outcome;

import java.util.List;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface OutcomeRepository extends JpaRepository<Outcome, Long> {

    List<Outcome> findByExposure_ExposureId(Long exposureId);
    List<Outcome> findByExposure_Experiment_ExperimentId(Long experimentId);
    Page<Outcome> findByExposure_Experiment_ExperimentId(Long experimentId, Pageable pageable);
    Outcome findByOutcomeId(Long outcomeId);
    boolean existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOutcomeId(Long experimentId, Long exposureId, Long outcomeId);

    @Modifying
    @Transactional
    @Query("delete from Outcome s where s.outcomeId = ?1")
    void deleteByOutcomeId(Long outcomeId);

}
