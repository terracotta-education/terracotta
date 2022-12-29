package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Exposure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface ExposureRepository extends JpaRepository<Exposure, Long> {

    List<Exposure> findByExperiment_ExperimentId(Long experimentId);

    Exposure findByExposureId(Long exposureId);

    boolean existsByExperiment_ExperimentIdAndExposureId(Long experimentId, Long exposureId);

    void deleteByExperiment_ExperimentId(Long experimentId);

    @Transactional
    void deleteByExposureId(Long exposureId);

}