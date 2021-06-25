package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface AnswerSubmissionService {
    //METHODS FOR MC ANSWER SUBMISSIONS

    List<AnswerMcSubmission> findByQuestionSubmissionIdMC(Long questionSubmissionId);

    AnswerSubmissionDto toDtoMC(AnswerMcSubmission mcAnswer);

    AnswerMcSubmission fromDtoMC(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException;

    AnswerMcSubmission saveMC(AnswerMcSubmission mcAnswer);

    Optional<AnswerMcSubmission> findByIdMC(Long id);

    void saveAndFlushMC(AnswerMcSubmission answerToChange);

    void saveAllAnswersMC(List<AnswerMcSubmission> answerMcSubmissionList);

    void deleteByIdMC(Long id) throws EmptyResultDataAccessException;

    boolean mcAnswerSubmissionBelongsToQuestionSubmission(Long questionSubmissionId, Long answerMcSubmissionId);

    //METHODS FOR ESSAY ANSWER SUBMISSIONS

    List<AnswerEssaySubmission> findAllByQuestionSubmissionIdEssay(Long questionSubmissionId);

    AnswerSubmissionDto toDtoEssay(AnswerEssaySubmission essayAnswer);

    AnswerEssaySubmission fromDtoEssay(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException;

    AnswerEssaySubmission saveEssay(AnswerEssaySubmission essayAnswer);

    Optional<AnswerEssaySubmission> findByIdEssay(Long id);

    void saveAndFlushEssay(AnswerEssaySubmission answerToChange);

    void saveAllAnswersEssay(List<AnswerEssaySubmission> essayAnswerList);

    void deleteByIdEssay(Long id) throws EmptyResultDataAccessException;

    boolean essayAnswerSubmissionBelongsToQuestionSubmission(Long questionSubmissionId, Long answerEssaySubmissionId);

    //USED BY ALL TYPES
    String answerSubmissionNotFound(SecurityInfo securityInfo, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId, Long answerSubmissionId);
}
