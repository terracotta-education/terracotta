package edu.iu.terracotta.dao.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.model.enums.AssignmentFileArchiveStatus;

@SuppressWarnings("PMD.MethodNamingConventions")
public interface AssignmentFileArchiveRepository extends JpaRepository<AssignmentFileArchive, Long> {

    Optional<AssignmentFileArchive> findTopByAssignment_AssignmentIdAndStatusInOrderByCreatedAtDesc(long assignmentId, List<AssignmentFileArchiveStatus> statuses);
    Optional<AssignmentFileArchive> findByUuidAndAssignment_AssignmentId(UUID uuid, long assignmentId);
    Optional<AssignmentFileArchive> findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(long assignmentId);
    List<AssignmentFileArchive> findAllByAssignment_AssignmentIdAndStatus(long assignmentId, AssignmentFileArchiveStatus status);
    List<AssignmentFileArchive> findAllByUpdatedAtLessThanAndStatusIn(Timestamp updatedAt, List<AssignmentFileArchiveStatus> statuses);

}
