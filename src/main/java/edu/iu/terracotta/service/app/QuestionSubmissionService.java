package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionDto;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface QuestionSubmissionService {

    List<QuestionSubmission> findAllBySubmissionId(Long submissionId);

    QuestionSubmissionDto toDto(QuestionSubmission questionSubmission, boolean questionSubmissionComments);

    QuestionSubmission fromDto(QuestionSubmissionDto questionSubmissionDto) throws DataServiceException;

    QuestionSubmission save(QuestionSubmission questionSubmission);

    Optional<QuestionSubmission> findById(Long id);

    boolean existsByAssessmentIdAndQuestionId(Long assessmentId, Long questionId);

    void saveAndFlush(QuestionSubmission questionSubmissionToChange);

    void saveAllQuestionSubmissions(List<QuestionSubmission> questionSubmissionList);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean questionSubmissionBelongsToAssessmentAndSubmission(Long assessmentId, Long submissionId, Long questionSubmissionId);

    QuestionSubmission automaticGrading(QuestionSubmission questionSubmission);
}
