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

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface AnswerSubmissionService {

    //general methods
    List<AnswerSubmissionDto> getAnswerSubmissions(long questionSubmissionId, String answerType) throws DataServiceException, IOException;

    AnswerSubmissionDto getAnswerSubmission(long answerSubmissionId, String answerType) throws DataServiceException, IOException;

    AnswerSubmissionDto postAnswerSubmission(AnswerSubmissionDto answerSubmissionDto, long questionSubmissionId) throws IdInPostException, DataServiceException, TypeNotSupportedException, IOException;

    List<AnswerSubmissionDto> postAnswerSubmissions(List<AnswerSubmissionDto> answerSubmissionDto)
            throws IdInPostException, DataServiceException, TypeNotSupportedException, IdMissingException,
            ExceedingLimitException, IOException;

    void updateAnswerSubmission(AnswerSubmissionDto answerSubmissionDto, long answerSubmissionId, String answerType) throws AnswerNotMatchingException, DataServiceException;

    void deleteAnswerSubmission(long answerSubmissionId, String answerType) throws DataServiceException;

    //METHODS FOR MC ANSWER SUBMISSIONS

    List<AnswerMcSubmission> findByQuestionSubmissionIdMC(Long questionSubmissionId);

    List<AnswerSubmissionDto> getAnswerMcSubmissions(Long questionSubmissionId);

    AnswerSubmissionDto toDtoMC(AnswerMcSubmission mcAnswer);

    AnswerMcSubmission fromDtoMC(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException;

    AnswerMcSubmission saveMC(AnswerMcSubmission mcAnswer);

    AnswerMcSubmission getAnswerMcSubmission(Long answerSubmissionId);

    void updateAnswerMcSubmission(Long id, AnswerSubmissionDto answerSubmissionDto) throws AnswerNotMatchingException;

    void saveAndFlushMC(AnswerMcSubmission answerToChange);

    void deleteByIdMC(Long id) throws EmptyResultDataAccessException;

    boolean mcAnswerSubmissionBelongsToQuestionSubmission(Long questionSubmissionId, Long answerMcSubmissionId);

    //METHODS FOR ESSAY ANSWER SUBMISSIONS

    List<AnswerEssaySubmission> findAllByQuestionSubmissionIdEssay(Long questionSubmissionId);

    List<AnswerSubmissionDto> getAnswerEssaySubmissions(Long questionSubmissionId);

    AnswerSubmissionDto toDtoEssay(AnswerEssaySubmission essayAnswer);

    AnswerEssaySubmission fromDtoEssay(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException;

    AnswerEssaySubmission saveEssay(AnswerEssaySubmission essayAnswer);

    Optional<AnswerEssaySubmission> findByIdEssay(Long id);

    AnswerEssaySubmission getAnswerEssaySubmission(Long answerSubmissionId);

    void updateAnswerEssaySubmission(Long id, AnswerSubmissionDto answerSubmissionDto);

    void saveAndFlushEssay(AnswerEssaySubmission answerToChange);

    void deleteByIdEssay(Long id) throws EmptyResultDataAccessException;

    boolean essayAnswerSubmissionBelongsToQuestionSubmission(Long questionSubmissionId, Long answerEssaySubmissionId);

    //USED BY ALL TYPES
    String getAnswerType(Long questionSubmissionId);

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId, Long answerSubmissionId);

    //METHODS FOR FILE SUBMISSIONS

    AnswerSubmissionDto toDtoFile(AnswerFileSubmission essayAnswer) throws IOException;

    AnswerFileSubmission fromDtoFile(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException;

    List<AnswerFileSubmission> findAllByQuestionSubmissionIdFile(Long questionSubmissionId);

    List<AnswerSubmissionDto> getAnswerFileSubmissions(Long questionSubmissionId) throws IOException;

    AnswerFileSubmission saveFile(AnswerFileSubmission essayAnswer);

    Optional<AnswerFileSubmission> findByIdFile(Long id);

    AnswerFileSubmission getAnswerFileSubmission(Long answerSubmissionId);

    void updateAnswerFileSubmission(Long id, AnswerSubmissionDto answerSubmissionDto);

    void saveAndFlushFile(AnswerFileSubmission answerToChange);

    void deleteByIdFile(Long id) throws EmptyResultDataAccessException;

    boolean fileAnswerSubmissionBelongsToQuestionSubmission(Long questionSubmissionId, Long answerFileSubmissionId);

    AnswerSubmissionDto handleFileAnswerSubmission(AnswerSubmissionDto answerSubmissionDto, MultipartFile file)
        throws IdInPostException, DataServiceException, TypeNotSupportedException, IOException;

    AnswerSubmissionDto handleFileAnswerSubmissionUpdate(AnswerSubmissionDto answerSubmissionDto, MultipartFile file)
        throws IdInPostException, DataServiceException, TypeNotSupportedException, IOException;

    FileResponseDto getFileResponseDto(long answerSubmissionId) throws IOException;

}
