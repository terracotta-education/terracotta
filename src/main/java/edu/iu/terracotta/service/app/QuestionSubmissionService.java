package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.DuplicateQuestionException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface QuestionSubmissionService {

    List<QuestionSubmission> findAllBySubmissionId(Long submissionId);

    List<QuestionSubmissionDto> getQuestionSubmissions(long submissionId, boolean answerSubmissions, boolean questionSubmissionComments, long assessmentId, boolean isStudent) throws AssessmentNotMatchingException;

    QuestionSubmission getQuestionSubmission(Long id);

    List<QuestionSubmissionDto> postQuestionSubmissions(List<QuestionSubmissionDto> questionSubmissionDtoList, long assessmentId, long submissionId, boolean student) throws DataServiceException, IdInPostException, InvalidUserException, DuplicateQuestionException, IdMissingException, TypeNotSupportedException;

    void updateQuestionSubmissions(Map<QuestionSubmission, QuestionSubmissionDto> map, boolean student) throws InvalidUserException, DataServiceException, AnswerNotMatchingException, AnswerSubmissionNotMatchingException, QuestionSubmissionNotMatchingException, IdMissingException;

    QuestionSubmissionDto toDto(QuestionSubmission questionSubmission, boolean answerSubmissions, boolean questionSubmissionComments);

    QuestionSubmission fromDto(QuestionSubmissionDto questionSubmissionDto) throws DataServiceException;

    QuestionSubmission save(QuestionSubmission questionSubmission);

    Optional<QuestionSubmission> findById(Long id);

    boolean existsByAssessmentIdAndSubmissionIdAndQuestionId(Long assessmentId, Long submissionId, Long questionId);

    void saveAndFlush(QuestionSubmission questionSubmissionToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean questionSubmissionBelongsToAssessmentAndSubmission(Long assessmentId, Long submissionId, Long questionSubmissionId);

    QuestionSubmission automaticGradingMC(QuestionSubmission questionSubmission, AnswerMcSubmission answerMcSubmission);

    void validateDtoPost(QuestionSubmissionDto questionSubmissionDto, Long assessmentId, Long submissionId, boolean student) throws IdMissingException, DuplicateQuestionException, InvalidUserException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId);

    void validateAndPrepareQuestionSubmissionList(List<QuestionSubmissionDto> questionSubmissionDtoList, long assessmentId, long submissionId, boolean student) throws IdInPostException, DataServiceException, DuplicateQuestionException, InvalidUserException, IdMissingException, AnswerSubmissionNotMatchingException, AnswerNotMatchingException, ExceedingLimitException, TypeNotSupportedException;

    void validateQuestionSubmission(QuestionSubmissionDto questionSubmissionDto) throws DataServiceException;

    void canSubmit(SecuredInfo securedInfo, long experimentId)
            throws CanvasApiException, IOException, AssignmentAttemptException;

}
