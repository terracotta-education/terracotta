package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface QuestionSubmissionCommentRepository extends JpaRepository<QuestionSubmissionComment, Long> {

    List<QuestionSubmissionComment> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);

    QuestionSubmissionComment findByQuestionSubmissionCommentId(Long questionSubmissionCommentId);

    boolean existsByQuestionSubmission_QuestionSubmissionIdAndQuestionSubmissionCommentId(Long questionSubmissionId, Long questionSubmissionCommentId);

    @Transactional
    void deleteByQuestionSubmissionCommentId(Long questionSubmissionCommentId);

}
