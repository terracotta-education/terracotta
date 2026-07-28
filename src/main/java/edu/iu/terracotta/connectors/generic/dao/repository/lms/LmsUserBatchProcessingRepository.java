package edu.iu.terracotta.connectors.generic.dao.repository.lms;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;

public interface LmsUserBatchProcessingRepository extends JpaRepository<LmsUserBatchProcessing, Long> {

    Optional<LmsUserBatchProcessing> findByBatchId(UUID batchId);

}
