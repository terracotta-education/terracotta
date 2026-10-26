package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.AnswerFileSubmission;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface AnswerFileSubmissionRepository extends JpaRepository<AnswerFileSubmission, Long> {

    AnswerFileSubmission findByUuid(UUID uuid);

    List<AnswerFileSubmission> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);
    List<AnswerFileSubmission> findByQuestionSubmission_QuestionSubmissionIdIn(List<Long> questionSubmissionIds);
    long countByQuestionSubmission_QuestionSubmissionIdIn(List<Long> questionSubmissionIds);
    List<AnswerFileSubmission> findByQuestionSubmission_Question_QuestionId(Long questionId);
    AnswerFileSubmission findByAnswerFileSubmissionId(Long answerFileSubmissionId);
    boolean existsByQuestionSubmission_QuestionSubmissionIdAndAnswerFileSubmissionId(Long questionSubmissionId, Long answerFileSubmissionId);

    /**
     * When a participant last uploaded (or replaced) a file for the assignment - the
     * same files an assignment file archive is built from (see
     * AssignmentAsyncServiceImpl.processAssignmentFileArchive).
     */
    @Query("""
        SELECT MAX(afs.updatedAt)
        FROM AnswerFileSubmission afs
        WHERE afs.questionSubmission.question.assessment.treatment.assignment.assignmentId = :assignmentId
        """)
    Optional<Timestamp> findLatestUploadTimeByAssignmentId(@Param("assignmentId") long assignmentId);

    @Transactional
    void deleteByAnswerFileSubmissionId(Long answerFileSubmissionId);

}
