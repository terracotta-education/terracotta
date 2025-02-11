package edu.iu.terracotta.service.app;

import edu.iu.terracotta.dao.entity.AnswerMc;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AnswerDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

public interface AnswerService {

    List<AnswerDto> findAllByQuestionIdMC(Long questionId, boolean showCorrectAnswer);
    List<AnswerDto> findAllByQuestionIdMC(QuestionSubmission questionSubmission, boolean showCorrectAnswer);
    AnswerDto getAnswerMC(Long answerId);
    AnswerDto postAnswerMC(AnswerDto answerDto, long questionId) throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException;
    AnswerDto toDtoMC(AnswerMc answer, int answerOrder, boolean showCorrectAnswer);
    AnswerMc fromDtoMC(AnswerDto answerDto) throws DataServiceException;
    AnswerMc findByAnswerId(Long answerId);
    List<AnswerDto> updateAnswerMC(Map<AnswerMc, AnswerDto> map);
    void deleteByIdMC(Long id) throws EmptyResultDataAccessException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId, Long answerId);
    void limitReached(Long questionId) throws MultipleChoiceLimitReachedException;
    List<AnswerMc> duplicateAnswersForQuestion(Long originalQuestionId, Question newQuestion) throws QuestionNotMatchingException;
    String getQuestionType(Long questionId);

}
