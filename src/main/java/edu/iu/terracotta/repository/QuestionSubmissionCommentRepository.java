package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionSubmissionCommentRepository extends JpaRepository<QuestionSubmissionComment, Long> {

    List<QuestionSubmissionComment> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);

    boolean existsByQuestionSubmission_QuestionSubmissionIdAndQuestionSubmissionCommentId(Long questionSubmissionId, Long questionSubmissionCommentId);



}