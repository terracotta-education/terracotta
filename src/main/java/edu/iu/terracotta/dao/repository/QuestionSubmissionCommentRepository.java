package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.QuestionSubmissionComment;

import java.util.List;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface QuestionSubmissionCommentRepository extends JpaRepository<QuestionSubmissionComment, Long> {

    List<QuestionSubmissionComment> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);
    QuestionSubmissionComment findByQuestionSubmissionCommentId(Long questionSubmissionCommentId);
    boolean existsByQuestionSubmission_QuestionSubmissionIdAndQuestionSubmissionCommentId(Long questionSubmissionId, Long questionSubmissionCommentId);

    @Modifying
    @Transactional
    @Query("delete from QuestionSubmissionComment s where s.questionSubmissionCommentId = ?1")
    void deleteByQuestionSubmissionCommentId(Long questionSubmissionCommentId);

}
