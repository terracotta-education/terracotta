package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuestionSubmissionCommentRepository extends JpaRepository<QuestionSubmissionComment, Long> {

    List<QuestionSubmissionComment> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);

    boolean existsByQuestionSubmission_QuestionSubmissionIdAndQuestionSubmissionCommentId(Long questionSubmissionId, Long questionSubmissionCommentId);

    @Transactional
    @Modifying
    @Query("delete from QuestionSubmissionComment s where s.questionSubmissionCommentId = ?1")
    void deleteByQuestionSubmissionCommentId(Long questionSubmissionCommentId);

}