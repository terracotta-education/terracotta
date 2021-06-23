package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.AnswerMcSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerMcSubmissionRepository extends JpaRepository<AnswerMcSubmission, Long> {
    List<AnswerMcSubmission> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);

    boolean existsByQuestionSubmission_QuestionSubmissionIdAndAnswerMcSubId(Long questionSubmissionId, Long answerMcSubId);
}