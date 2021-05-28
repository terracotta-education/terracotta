package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Exposure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExposureRepository extends JpaRepository<Exposure, Long> {

    List<Exposure> findByExperiment_ExperimentId(Long experimentId);

    boolean existsByExperiment_ExperimentIdAndExposureId(Long experimentId, Long exposureId);
}