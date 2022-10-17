package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

public interface AnswerService {

    //METHODS FOR MC ANSWERS
    List<AnswerDto> findAllByQuestionIdMC(Long questionId, boolean showCorrectAnswer);

    List<AnswerDto> findAllByQuestionIdMC(QuestionSubmission questionSubmission, boolean showCorrectAnswer);

    AnswerDto getAnswerMC(Long answerId);

    AnswerDto postAnswerMC(AnswerDto answerDto, long questionId) throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException;

    AnswerDto toDtoMC(AnswerMc answer, int answerOrder, boolean showCorrectAnswer);

    AnswerMc fromDtoMC(AnswerDto answerDto) throws DataServiceException;

    AnswerMc saveMC(AnswerMc answer);

    AnswerMc findByAnswerId(Long answerId);

    List<AnswerDto> updateAnswerMC(Map<AnswerMc, AnswerDto> map);

    void deleteByIdMC(Long id) throws EmptyResultDataAccessException;

    boolean mcAnswerBelongsToQuestionAndAssessment(Long assessmentId, Long questionId, Long answerId);

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId, Long answerId);

    void limitReached(Long questionId) throws MultipleChoiceLimitReachedException;

    List<AnswerMc> duplicateAnswersForQuestion(Long originalQuestionId, Question newQuestion) throws QuestionNotMatchingException;

    //METHODS FOR ALL ANSWER TYPES
    String getQuestionType(Long questionId);
}
