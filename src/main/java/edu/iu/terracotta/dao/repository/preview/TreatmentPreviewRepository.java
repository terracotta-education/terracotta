package edu.iu.terracotta.dao.repository.preview;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.preview.TreatmentPreview;

@SuppressWarnings("PMD.MethodNamingConventions")
public interface TreatmentPreviewRepository extends JpaRepository<TreatmentPreview, Long> {

    Optional<TreatmentPreview> findByUuidAndTreatment_TreatmentIdAndExperiment_ExperimentIdAndCondition_ConditionIdAndOwner_UserKey(UUID uuid, long treatmentId, long experimentId, long conditionId, String ownerId);

}
