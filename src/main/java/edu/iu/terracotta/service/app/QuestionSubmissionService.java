package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.DuplicateQuestionException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface QuestionSubmissionService {

    List<QuestionSubmission> findAllBySubmissionId(Long submissionId);

    List<QuestionSubmissionDto> getQuestionSubmissions(Long submissionId);

    QuestionSubmission getQuestionSubmission(Long id);

    void updateQuestionSubmissions(Map<QuestionSubmission, QuestionSubmissionDto> map, boolean student) throws InvalidUserException;

    QuestionSubmissionDto toDto(QuestionSubmission questionSubmission, boolean questionSubmissionComments);

    QuestionSubmission fromDto(QuestionSubmissionDto questionSubmissionDto) throws DataServiceException;

    QuestionSubmission save(QuestionSubmission questionSubmission);

    Optional<QuestionSubmission> findById(Long id);

    boolean existsByAssessmentIdAndQuestionId(Long assessmentId, Long questionId);

    void saveAndFlush(QuestionSubmission questionSubmissionToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean questionSubmissionBelongsToAssessmentAndSubmission(Long assessmentId, Long submissionId, Long questionSubmissionId);

    QuestionSubmission automaticGradingMC(QuestionSubmission questionSubmission, AnswerMcSubmission answerMcSubmission);

    void validateDto(QuestionSubmissionDto questionSubmissionDto, Long assessmentId, boolean student) throws IdMissingException, DuplicateQuestionException, InvalidUserException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId);
}
