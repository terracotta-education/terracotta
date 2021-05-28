package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Answer;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    AnswerService answerService;

    @Override
    public List<Question> findAllByAssessmentId(Long assessmentId) {
        return allRepositories.questionRepository.findByAssessment_AssessmentId(assessmentId);
    }

    @Override
    public QuestionDto toDto(Question question) {

        QuestionDto questionDto = new QuestionDto();
        questionDto.setQuestionId(question.getQuestionId());
        questionDto.setHtml(question.getHtml());
        List<AnswerDto> answers = new ArrayList<>();
        for(Answer answer : question.getAnswers()) {
            answers.add(answerService.toDto(answer));
        }
        questionDto.setAnswers(answers);
        questionDto.setPoints(question.getPoints());
        questionDto.setAssessmentId(question.getAssessment().getAssessmentId());

        return questionDto;
    }

    @Override
    public Question fromDto(QuestionDto questionDto) throws DataServiceException {

        Question question = new Question();
        question.setQuestionId(questionDto.getQuestionId());
        question.setHtml(questionDto.getHtml());
        question.setPoints(questionDto.getPoints());
        Optional<Assessment> assessment = allRepositories.assessmentRepository.findById(questionDto.getAssessmentId());
        if(assessment.isPresent()) {
            question.setAssessment(assessment.get());
        } else {
            throw new DataServiceException("The assessment for the question does not exist");
        }
        return question;
    }

    @Override
    public Question save(Question question) { return allRepositories.questionRepository.save(question); }

    @Override
    public Optional<Question> findById(Long id) { return allRepositories.questionRepository.findById(id); }

    @Override
    public void saveAndFlush(Question questionToChange) { allRepositories.questionRepository.saveAndFlush(questionToChange); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.questionRepository.deleteById(id); }

    @Override
    public boolean questionBelongsToAssessment(Long assessmentId, Long questionId) {
        return allRepositories.questionRepository.existsByAssessment_AssessmentIdAndQuestionId(assessmentId, questionId);
    }
}
