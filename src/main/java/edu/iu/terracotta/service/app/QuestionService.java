package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidQuestionTypeException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface QuestionService {

    List<Question> findAllByAssessmentId(Long assessmentId);

    List<QuestionDto> getQuestions(Long assessmentId);

    Question getQuestion(Long id);

    QuestionDto postQuestion(QuestionDto questionDto, long assessmentId, boolean answers) throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException;

    List<Question> duplicateQuestionsForAssessment(Long oldAssessmentId, Assessment newAssessment) throws DataServiceException, QuestionNotMatchingException;

    void updateQuestion(Map<Question, QuestionDto> map) throws NegativePointsException;

    QuestionDto toDto(Question question, boolean answers, boolean showCorrectAnswer);

    QuestionDto toDto(Question question, Long submissionId, boolean answers, boolean showCorrectAnswer);

    Question fromDto(QuestionDto questionDto) throws DataServiceException, NegativePointsException;

    Question save(Question question);

    Optional<Question> findById(Long id);

    Question findByQuestionId(Long id);

    Question saveAndFlush(Question questionToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean questionBelongsToAssessment(Long assessmentId, Long questionId);

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId);

    void validateQuestionType(QuestionDto questionDto) throws InvalidQuestionTypeException;
}
