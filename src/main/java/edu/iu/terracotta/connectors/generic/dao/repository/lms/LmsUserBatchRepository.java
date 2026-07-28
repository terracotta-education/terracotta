package edu.iu.terracotta.connectors.generic.dao.repository.lms;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchEmailProjection;

public interface LmsUserBatchRepository extends JpaRepository<LmsUserBatch, UUID> {

    List<LmsUserBatch> findByBatchId(UUID batchId, Pageable pageable);
    void deleteByBatchId(UUID batchId);

    @Query("""
        SELECT
            l.lmsUserId as lmsUserId,
            l.email as email,
            l.userKey as userKey,
            l.name as name
        FROM
            LmsUserBatch l
        WHERE
            l.batchId = :batchId AND
            l.email IN :emails
    """)
    List<LmsUserBatchEmailProjection> findBatchProjectionsByBatchIdAndEmailIn(@Param("batchId") UUID batchId, @Param("emails") List<String> emails, PageRequest pageRequest);

}
