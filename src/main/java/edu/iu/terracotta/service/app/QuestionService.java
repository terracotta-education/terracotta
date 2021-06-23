package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface QuestionService {

    List<Question> findAllByAssessmentId(Long assessmentId);

    QuestionDto toDto(Question question, boolean answers);

    Question fromDto(QuestionDto questionDto) throws DataServiceException;

    Question save(Question question);

    Optional<Question> findById(Long id);

    Question findByQuestionId(Long id);

    void saveAndFlush(Question questionToChange);

    void saveAllQuestions(List<Question> questionList);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean questionBelongsToAssessment(Long assessmentId, Long questionId);
}
