package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.AnswerMcSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AnswerMcSubmissionRepository extends JpaRepository<AnswerMcSubmission, Long> {
    List<AnswerMcSubmission> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);

    AnswerMcSubmission findByAnswerMcSubId(Long answerMcSubId);

    boolean existsByQuestionSubmission_QuestionSubmissionIdAndAnswerMcSubId(Long questionSubmissionId, Long answerMcSubId);

    @Transactional
    @Modifying
    @Query("delete from AnswerMcSubmission s where s.answerMcSubId = ?1")
    void deleteByAnswerMcSubId(Long answerMcSubId);
}