package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AnswerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
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
    public List<AnswerDto> findAllByQuestionIdMC(Long questionId, boolean student) {
        List<AnswerMc> answerList = allRepositories.answerMcRepository.findByQuestion_QuestionId(questionId);
        List<AnswerDto> answerDtoList = new ArrayList<>();
        if(!answerList.isEmpty()){
            for(AnswerMc answerMc : answerList){
                answerDtoList.add(toDtoMC(answerMc, student));
            }
        }
        return answerDtoList;
    }

    @Override
    public AnswerDto getAnswerMC(Long answerId, boolean student){
        AnswerMc answerMc = allRepositories.answerMcRepository.findByAnswerMcId(answerId);
        return toDtoMC(answerMc, student);
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
    public AnswerMc findByAnswerId(Long answerId) { return allRepositories.answerMcRepository.findByAnswerMcId(answerId); }

    @Override
    public Optional<AnswerMc> findByQuestionIdAndAnswerId(Long questionId, Long answerId) {
        return allRepositories.answerMcRepository.findByQuestion_QuestionIdAndAnswerMcId(questionId, answerId);
    }

    @Override
    public void saveAndFlushMC(AnswerMc answerToChange) { allRepositories.answerMcRepository.saveAndFlush(answerToChange); }

    @Override
    public AnswerMc updateAnswerMC(AnswerMc answerMc, AnswerDto answerDto){
        if(answerDto.getHtml() != null)
            answerMc.setHtml(answerDto.getHtml());
        if(answerDto.getAnswerOrder() != null)
            answerMc.setAnswerOrder(answerDto.getAnswerOrder());
        if(answerDto.getCorrect() != null)
            answerMc.setCorrect(answerDto.getCorrect());
        return answerMc;
    }

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
    public String answerNotFound(SecuredInfo securedInfo, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId, Long answerId) {
        return "Answer in platform " + securedInfo.getPlatformDeploymentId() + " and context " + securedInfo.getContextId()
                + " and experiment with id " + experimentId + " and condition id " + conditionId + " and treatment id " + treatmentId + " and assessment id " + assessmentId
                + " and question id " + questionId + " with id " + answerId + " not found.";
    }

    @Override
    public String getQuestionType(Long questionId){
        return allRepositories.questionRepository.findByQuestionId(questionId).getQuestionType().toString();
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId, Long answerId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path(
                "/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers/{answer_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, questionId, answerId).toUri());
        return headers;
    }

}
