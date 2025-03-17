package edu.iu.terracotta.dao.repository.distribute;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ExperimentImportRepository extends JpaRepository<ExperimentImport, Long> {

    Optional<ExperimentImport> findByUuid(UUID uuid);
    Optional<ExperimentImport> findByUuidAndOwner_UserKeyAndContext_ContextId(UUID uuid, String userKey, long contextId);
    List<ExperimentImport> findAllByUpdatedAtLessThanOrStatusIn(Timestamp updatedAt, List<ExperimentImportStatus> statuses);
    List<ExperimentImport> findAllByOwner_UserKeyAndContext_ContextIdAndStatusIn(String userKey, long contextId, List<ExperimentImportStatus> statuses);

}
