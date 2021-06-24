package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AnswerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class AnswerServiceImpl implements AnswerService {

    @Autowired
    AllRepositories allRepositories;


    /*
    MULTIPLE CHOICE
     */
    @Override
    public List<AnswerMc> findAllByQuestionIdMC(Long questionId) {
        return allRepositories.answerMcRepository.findByQuestion_QuestionId(questionId);
    }

    @Override
    public AnswerDto toDtoMC(AnswerMc answer, boolean student) {
        AnswerDto answerDto = new AnswerDto();
        answerDto.setAnswerId(answer.getAnswerMcId());
        answerDto.setHtml(answer.getHtml());
        answerDto.setAnswerOrder(answer.getAnswerOrder());
        answerDto.setQuestionId(answer.getQuestion().getQuestionId());
        answerDto.setAnswerType("MC");
        if(student){
            answerDto.setCorrect(null);
        } else {
            answerDto.setCorrect(answer.getCorrect());
        }

        return answerDto;
    }

    @Override
    public AnswerMc fromDtoMC(AnswerDto answerDto) throws DataServiceException {

        AnswerMc answer = new AnswerMc();
        answer.setAnswerMcId(answerDto.getAnswerId());
        answer.setHtml(answerDto.getHtml());
        answer.setCorrect(answerDto.getCorrect());
        answer.setAnswerOrder(answerDto.getAnswerOrder());
        Optional<Question> question = allRepositories.questionRepository.findById(answerDto.getQuestionId());
        if(question.isPresent()){
            answer.setQuestion(question.get());
        } else {
            throw new DataServiceException("The question for the answer does not exist");
        }
        return answer;
    }

    @Override
    public AnswerMc saveMC(AnswerMc answer) { return allRepositories.answerMcRepository.save(answer); }

    @Override
    public Optional<AnswerMc> findByIdMC(Long id) { return allRepositories.answerMcRepository.findById(id); }

    @Override
    public Optional<AnswerMc> findByQuestionIdAndAnswerId(Long questionId, Long answerId) {
        return allRepositories.answerMcRepository.findByQuestion_QuestionIdAndAnswerMcId(questionId, answerId);
    }

    @Override
    public void saveAndFlushMC(AnswerMc answerTOChange) { allRepositories.answerMcRepository.saveAndFlush(answerTOChange); }

    @Override
    @Transactional
    public void saveAllAnswersMC(List<AnswerMc> answerList) { allRepositories.answerMcRepository.saveAll(answerList); }

    @Override
    public void deleteByIdMC(Long id) { allRepositories.answerMcRepository.deleteByAnswerMcId(id); }

    @Override
    public boolean mcAnswerBelongsToQuestionAndAssessment(Long assessmentId, Long questionId, Long answerId) {
        return allRepositories.answerMcRepository.existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerMcId(assessmentId, questionId, answerId);
    }

    @Override
    public String answerNotFound(SecurityInfo securityInfo, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId, Long answerId) {
        return "Answer in platform " + securityInfo.getPlatformDeploymentId() + " and context " + securityInfo.getContextId()
                + " and experiment with id " + experimentId + " and condition id " + conditionId + " and treatment id " + treatmentId + " and assessment id " + assessmentId
                + " and question id " + questionId + " with id " + answerId + " not found.";
    }

}
