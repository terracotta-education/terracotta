package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Exposure;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ExposureRepository extends JpaRepository<Exposure, Long> {

    List<Exposure> findByExperiment_ExperimentId(Long experimentId);
    Exposure findByExposureId(Long exposureId);
    boolean existsByExperiment_ExperimentIdAndExposureId(Long experimentId, Long exposureId);
    Optional<Exposure> findByExperiment_ExperimentIdAndExposureId(long experimentId, long exposureId);
    void deleteByExperiment_ExperimentId(Long experimentId);

    @Modifying
    @Transactional
    @Query("delete from Exposure s where s.exposureId = ?1")
    void deleteByExposureId(Long exposureId);

}
