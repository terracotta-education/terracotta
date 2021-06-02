package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Answer;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.dto.AnswerDto;
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

    @Override
    public List<Answer> findAllByQuestionId(Long questionId) {
        return allRepositories.answerRepository.findByQuestion_QuestionId(questionId);
    }

    @Override
    public AnswerDto toDto(Answer answer) {
        AnswerDto answerDto = new AnswerDto();
        answerDto.setAnswerId(answer.getAnswerId());
        answerDto.setHtml(answer.getHtml());
        answerDto.setCorrect(answer.getCorrect());
        answerDto.setAnswerOrder(answer.getAnswerOrder());
        answerDto.setQuestionId(answer.getQuestion().getQuestionId());

        return answerDto;
    }

    @Override
    public Answer fromDto(AnswerDto answerDto) throws DataServiceException {

        Answer answer = new Answer();
        answer.setAnswerId(answerDto.getAnswerId());
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
    public Answer save(Answer answer) { return allRepositories.answerRepository.save(answer); }

    @Override
    public Optional<Answer> findById(Long id) { return allRepositories.answerRepository.findById(id); }

    @Override
    public void saveAndFlush(Answer answerTOChange) { allRepositories.answerRepository.saveAndFlush(answerTOChange); }

    @Override
    @Transactional
    public void saveAllAnswers(List<Answer> answerList) { allRepositories.answerRepository.saveAll(answerList); }

    @Override
    public void deleteById(Long id) { allRepositories.answerRepository.deleteById(id); }

    @Override
    public boolean answerBelongsToAssessmentAndQuestion(Long assessmentId, Long questionId, Long answerId) {
        return allRepositories.answerRepository.existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerId(assessmentId, questionId, answerId);
    }
}
