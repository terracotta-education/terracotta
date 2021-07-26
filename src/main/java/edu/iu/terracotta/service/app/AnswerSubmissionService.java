package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

public interface AnswerSubmissionService {
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
}
