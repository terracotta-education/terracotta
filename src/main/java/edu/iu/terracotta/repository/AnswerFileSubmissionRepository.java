package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.AnswerFileSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AnswerFileSubmissionRepository extends JpaRepository<AnswerFileSubmission, Long> {

    List<AnswerFileSubmission> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);

   AnswerFileSubmission findByAnswerFileSubmissionId(Long answerFileSubmissionId);

    boolean existsByQuestionSubmission_QuestionSubmissionIdAndAnswerFileSubmissionId(Long questionSubmissionId, Long answerFileSubmissionId);

    @Transactional
    @Modifying
    @Query("delete from AnswerFileSubmission s where s.answerFileSubmissionId = ?1")
    void deleteByAnswerFileSubmissionId(Long answerFileSubmissionId);
}
