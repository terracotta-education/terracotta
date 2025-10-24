package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.AnswerMcSubmission;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionDto;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentLockedException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.DuplicateQuestionException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface QuestionSubmissionService {

    List<QuestionSubmissionDto> getQuestionSubmissions(long submissionId, boolean answerSubmissions, boolean questionSubmissionComments, long assessmentId, boolean isStudent) throws AssessmentNotMatchingException, IOException;
    QuestionSubmission getQuestionSubmission(Long id);
    List<QuestionSubmissionDto> postQuestionSubmissions(List<QuestionSubmissionDto> questionSubmissionDtoList, long assessmentId, long submissionId, boolean student) throws DataServiceException, IdInPostException, InvalidUserException, DuplicateQuestionException, IdMissingException, TypeNotSupportedException;
    void updateQuestionSubmissions(Map<QuestionSubmission, QuestionSubmissionDto> map, boolean student) throws InvalidUserException, DataServiceException, AnswerNotMatchingException, AnswerSubmissionNotMatchingException, QuestionSubmissionNotMatchingException, IdMissingException;
    QuestionSubmissionDto toDto(QuestionSubmission questionSubmission, boolean answerSubmissions, boolean questionSubmissionComments) throws IOException;
    QuestionSubmission fromDto(QuestionSubmissionDto questionSubmissionDto) throws DataServiceException;
    void deleteById(Long id) throws EmptyResultDataAccessException;
    QuestionSubmission automaticGradingMC(QuestionSubmission questionSubmission, AnswerMcSubmission answerMcSubmission);
    void validateDtoPost(QuestionSubmissionDto questionSubmissionDto, Long assessmentId, Long submissionId, boolean student) throws IdMissingException, DuplicateQuestionException, InvalidUserException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId);
    void validateAndPrepareQuestionSubmissionList(List<QuestionSubmissionDto> questionSubmissionDtoList, long assessmentId, long submissionId, boolean student) throws IdInPostException, DataServiceException, DuplicateQuestionException, InvalidUserException, IdMissingException, AnswerSubmissionNotMatchingException, AnswerNotMatchingException, ExceedingLimitException, TypeNotSupportedException;
    void validateQuestionSubmission(QuestionSubmissionDto questionSubmissionDto) throws DataServiceException;
    void canSubmit(SecuredInfo securedInfo, long experimentId) throws IOException, AssignmentAttemptException, ApiException, TerracottaConnectorException, AssignmentLockedException;
    void canSubmit(SecuredInfo securedInfo, long experimentId, boolean preferLmsCheck) throws IOException, AssignmentAttemptException, ApiException, TerracottaConnectorException, AssignmentLockedException;
    List<QuestionSubmissionDto> handleFileQuestionSubmission(MultipartFile file, String questionSubmissionDtoStr, long experimentId, long assessmentId, long submissionId, boolean student, SecuredInfo securedInfo)
            throws IOException, AssignmentAttemptException, IdInPostException, DataServiceException, DuplicateQuestionException, InvalidUserException, IdMissingException,
                AnswerSubmissionNotMatchingException, AnswerNotMatchingException, ExceedingLimitException, TypeNotSupportedException, ApiException, TerracottaConnectorException, AssignmentLockedException;
    List<QuestionSubmissionDto> handleFileQuestionSubmissionUpdate(MultipartFile file, String questionSubmissionDtoStr, long experimentId, long assessmentId, long submissionId, long questionSubmissionId, boolean student, SecuredInfo securedInfo)
            throws IOException, AssignmentAttemptException, IdInPostException, DataServiceException, DuplicateQuestionException, InvalidUserException, IdMissingException,
                AnswerSubmissionNotMatchingException, AnswerNotMatchingException, ExceedingLimitException, TypeNotSupportedException, QuestionSubmissionNotMatchingException, ApiException, TerracottaConnectorException, AssignmentLockedException;

}
