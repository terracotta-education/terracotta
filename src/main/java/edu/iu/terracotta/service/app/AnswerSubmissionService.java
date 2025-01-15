package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerFileSubmission;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.model.app.dto.FileResponseDto;
import edu.iu.terracotta.model.app.integrations.AnswerIntegrationSubmission;

import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

public interface AnswerSubmissionService {

    List<AnswerSubmissionDto> getAnswerSubmissions(long questionSubmissionId, String answerType) throws DataServiceException, IOException;
    AnswerSubmissionDto getAnswerSubmission(long answerSubmissionId, String answerType) throws DataServiceException, IOException;
    AnswerSubmissionDto postAnswerSubmission(AnswerSubmissionDto answerSubmissionDto, long questionSubmissionId) throws IdInPostException, DataServiceException, TypeNotSupportedException, IOException;
    List<AnswerSubmissionDto> postAnswerSubmissions(List<AnswerSubmissionDto> answerSubmissionDto) throws IdInPostException, DataServiceException, TypeNotSupportedException, IdMissingException, ExceedingLimitException, IOException;
    void updateAnswerSubmission(AnswerSubmissionDto answerSubmissionDto, long answerSubmissionId, String answerType) throws AnswerNotMatchingException, DataServiceException;
    void deleteAnswerSubmission(long answerSubmissionId, String answerType) throws DataServiceException;
    List<AnswerSubmissionDto> getAnswerMcSubmissions(Long questionSubmissionId);
    AnswerSubmissionDto toDtoMC(AnswerMcSubmission mcAnswer);
    AnswerMcSubmission fromDtoMC(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException;
    void updateAnswerMcSubmission(Long id, AnswerSubmissionDto answerSubmissionDto) throws AnswerNotMatchingException;
    List<AnswerSubmissionDto> getAnswerEssaySubmissions(Long questionSubmissionId);
    AnswerSubmissionDto toDtoEssay(AnswerEssaySubmission essayAnswer);
    AnswerEssaySubmission fromDtoEssay(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException;
    void updateAnswerEssaySubmission(Long id, AnswerSubmissionDto answerSubmissionDto);
    String getAnswerType(Long questionSubmissionId);
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId, Long answerSubmissionId);
    AnswerSubmissionDto toDtoFile(AnswerFileSubmission essayAnswer) throws IOException;
    AnswerFileSubmission fromDtoFile(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException;
    List<AnswerSubmissionDto> getAnswerFileSubmissions(Long questionSubmissionId) throws IOException;
    void updateAnswerFileSubmission(Long id, AnswerSubmissionDto answerSubmissionDto);
    AnswerSubmissionDto handleFileAnswerSubmission(AnswerSubmissionDto answerSubmissionDto, MultipartFile file) throws IdInPostException, DataServiceException, TypeNotSupportedException, IOException;
    AnswerSubmissionDto handleFileAnswerSubmissionUpdate(AnswerSubmissionDto answerSubmissionDto, MultipartFile file) throws IdInPostException, DataServiceException, TypeNotSupportedException, IOException;
    FileResponseDto getFileResponseDto(long answerSubmissionId) throws IOException;
    AnswerIntegrationSubmission fromDtoIntegration(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException;

}
