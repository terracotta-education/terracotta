package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Answer;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface AnswerService {

    List<Answer> findAllByQuestionId(Long questionId);

    AnswerDto toDto(Answer answer);

    Answer fromDto(AnswerDto answerDto) throws DataServiceException;

    Answer save(Answer answer);

    Optional<Answer> findById(Long id);

    Optional<Answer> findByQuestionIdAndAnswerId(Long questionId, Long answerId);

    void saveAndFlush(Answer answerToChange);

    void saveAllAnswers(List<Answer> answerList);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean answerBelongsToAssessmentAndQuestion(Long assessmentId, Long questionId, Long answerId);
}
