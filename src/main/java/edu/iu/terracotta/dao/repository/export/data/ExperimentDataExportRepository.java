package edu.iu.terracotta.dao.repository.export.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.export.data.ExperimentDataExport;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;

@SuppressWarnings("PMD.MethodNamingConventions")
public interface ExperimentDataExportRepository extends JpaRepository<ExperimentDataExport, Long> {

    Optional<ExperimentDataExport> findByIdAndExperiment_ExperimentId(long id, long experimentId);
    Optional<ExperimentDataExport> findTopByExperiment_ExperimentIdAndStatusInOrderByCreatedAtDesc(long experimentId, List<ExperimentDataExportStatus> statuses);
    Optional<ExperimentDataExport> findByUuidAndExperiment_ExperimentId(UUID uuid, long experimentId);
    Optional<ExperimentDataExport> findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(long experimentId);
    List<ExperimentDataExport> findAllByExperiment_ExperimentIdAndStatus(long experimentId, ExperimentDataExportStatus status);
    List<ExperimentDataExport> findAllByUpdatedAtLessThanAndStatusIn(Timestamp updatedAt, List<ExperimentDataExportStatus> statuses);

}
