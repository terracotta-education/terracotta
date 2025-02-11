package edu.iu.terracotta.service.app;

import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotMatchingException;
import edu.iu.terracotta.dao.model.dto.QuestionDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidQuestionTypeException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

public interface QuestionService {

    List<QuestionDto> getQuestions(Long assessmentId);
    Question getQuestion(Long id);
    QuestionDto postQuestion(QuestionDto questionDto, long assessmentId, boolean answers, boolean isNew)
        throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException,
        IntegrationNotFoundException, IntegrationClientNotFoundException;
    List<Question> duplicateQuestionsForAssessment(Long oldAssessmentId, Assessment newAssessment) throws DataServiceException, QuestionNotMatchingException;
    void updateQuestion(Map<Question, QuestionDto> map)
        throws NegativePointsException, IntegrationNotFoundException, IntegrationNotMatchingException, IntegrationConfigurationNotFoundException,
        IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException;
    QuestionDto toDto(Question question, boolean answers, boolean showCorrectAnswer);
    QuestionDto toDto(Question question, Long submissionId, boolean answers, boolean showCorrectAnswer);
    Question fromDto(QuestionDto questionDto) throws DataServiceException, NegativePointsException;
    Question save(Question question);
    Question findByQuestionId(Long id);
    void deleteById(Long id) throws EmptyResultDataAccessException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId);
    void validateQuestionType(QuestionDto questionDto) throws InvalidQuestionTypeException;

}
