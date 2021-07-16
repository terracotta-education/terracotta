package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.QuestionService;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

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
    public QuestionDto toDto(Question question, boolean answers, boolean student) {

        QuestionDto questionDto = new QuestionDto();
        questionDto.setQuestionId(question.getQuestionId());
        questionDto.setHtml(question.getHtml());
        questionDto.setQuestionOrder(question.getQuestionOrder());
        questionDto.setPoints(question.getPoints());
        questionDto.setAssessmentId(question.getAssessment().getAssessmentId());
        questionDto.setQuestionType(question.getQuestionType().name());
        if (answers) {
            switch (question.getQuestionType()) {
                case MC:
                    questionDto.setAnswers(answerService.findAllByQuestionIdMC(question.getQuestionId(), student));
                    break;
            }
        }
        return questionDto;
    }

    @Override
    public Question fromDto(QuestionDto questionDto) throws DataServiceException {

        Question question = new Question();
        question.setQuestionId(questionDto.getQuestionId());
        question.setHtml(questionDto.getHtml());
        question.setPoints(questionDto.getPoints());
        question.setQuestionOrder(questionDto.getQuestionOrder());
        question.setQuestionType(EnumUtils.getEnum(QuestionTypes.class, questionDto.getQuestionType()));
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
    @Transactional
    public void saveAllQuestions(List<Question> questionList) { allRepositories.questionRepository.saveAll(questionList); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        allRepositories.questionRepository.deleteByQuestionId(id);
    }

    @Override
    public boolean questionBelongsToAssessment(Long assessmentId, Long questionId) {
        return allRepositories.questionRepository.existsByAssessment_AssessmentIdAndQuestionId(assessmentId, questionId);
    }

    @Override
    public Question findByQuestionId(Long id) {
        return allRepositories.questionRepository.findByQuestionId(id);
    }
}
