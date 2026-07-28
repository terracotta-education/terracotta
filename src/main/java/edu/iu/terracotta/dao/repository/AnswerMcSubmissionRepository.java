package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.AnswerMcSubmission;

import java.util.List;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface AnswerMcSubmissionRepository extends JpaRepository<AnswerMcSubmission, Long> {

    List<AnswerMcSubmission> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);
    List<AnswerMcSubmission> findByQuestionSubmission_QuestionSubmissionIdIn(List<Long> questionSubmissionIds);
    long countByQuestionSubmission_QuestionSubmissionIdIn(List<Long> questionSubmissionIds);
    AnswerMcSubmission findByAnswerMcSubId(Long answerMcSubId);
    boolean existsByQuestionSubmission_QuestionSubmissionIdAndAnswerMcSubId(Long questionSubmissionId, Long answerMcSubId);

    @Modifying
    @Transactional
    @Query("delete from AnswerMcSubmission s where s.answerMcSubId = ?1")
    void deleteByAnswerMcSubId(Long answerMcSubId);

}