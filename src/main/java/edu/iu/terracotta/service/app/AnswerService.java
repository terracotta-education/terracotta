package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import org.apache.http.HttpHeaders;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

public interface AnswerService {

    //METHODS FOR MC ANSWERS
    List<AnswerMc> findAllByQuestionIdMC(Long questionId);

    AnswerDto toDtoMC(AnswerMc answer);

    AnswerMc fromDtoMC(AnswerDto answerDto) throws DataServiceException;

    AnswerMc saveMC(AnswerMc answer);

    Optional<AnswerMc> findByIdMC(Long id);

    Optional<AnswerMc> findByQuestionIdAndAnswerId(Long questionId, Long answerId);

    void saveAndFlushMC(AnswerMc answerToChange);

    void saveAllAnswersMC(List<AnswerMc> answerList);

    void deleteByIdMC(Long id) throws EmptyResultDataAccessException;

    boolean mcAnswerBelongsToQuestionAndAssessment(Long assessmentId, Long questionId, Long answerId);


    //METHODS FOR ALL ANSWER TYPES
    String answerNotFound(SecurityInfo securityInfo, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId, Long answerId);
}
