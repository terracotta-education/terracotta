package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
import edu.iu.terracotta.utils.TextConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AnswerSubmissionServiceImpl implements AnswerSubmissionService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    QuestionSubmissionService questionSubmissionService;

    /*
    MULTIPLE CHOICE SUBMISSION METHODS
     */

    @Override
    public List<AnswerMcSubmission> findByQuestionSubmissionIdMC(Long questionSubmissionId){
        return allRepositories.answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmissionId);
    }

    @Override
    public List<AnswerSubmissionDto> getAnswerMcSubmissions(Long questionSubmissionId){
        List<AnswerMcSubmission> answerMcSubmissions = findByQuestionSubmissionIdMC(questionSubmissionId);
        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        for(AnswerMcSubmission answerMcSubmission : answerMcSubmissions){
            answerSubmissionDtoList.add(toDtoMC(answerMcSubmission));
        }
        return answerSubmissionDtoList;
    }

    @Override
    public AnswerSubmissionDto toDtoMC(AnswerMcSubmission answer){
        AnswerSubmissionDto answerSubmissionDto = new AnswerSubmissionDto();
        answerSubmissionDto.setAnswerSubmissionId(answer.getAnswerMcSubId());
        answerSubmissionDto.setQuestionSubmissionId(answer.getQuestionSubmission().getQuestionSubmissionId());
        if(answer.getAnswerMc() != null){
            answerSubmissionDto.setAnswerId(answer.getAnswerMc().getAnswerMcId());
        }
        return answerSubmissionDto;
    }

    @Override
    public AnswerMcSubmission fromDtoMC(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException {
        AnswerMcSubmission answerMcSubmission = new AnswerMcSubmission();
        answerMcSubmission.setAnswerMcSubId(answerSubmissionDto.getAnswerSubmissionId());
        if(answerSubmissionDto.getAnswerId() != null){
            Optional<AnswerMc> answerMc = allRepositories.answerMcRepository.findById(answerSubmissionDto.getAnswerId());
            if(answerMc.isPresent()){
                answerMcSubmission.setAnswerMc(answerMc.get());
            } else {
                throw new DataServiceException("The MC answer for the answer submission does not exist.");
            }
        }
        Optional<QuestionSubmission> questionSubmission = allRepositories.questionSubmissionRepository.findById(answerSubmissionDto.getQuestionSubmissionId());
        if(questionSubmission.isPresent()){
            answerMcSubmission.setQuestionSubmission(questionSubmission.get());
        } else {
            throw new DataServiceException("The question submission for the answer submission does not exist.");
        }
        return answerMcSubmission;
    }

    @Override
    public AnswerMcSubmission saveMC(AnswerMcSubmission answerMcSubmission){ return allRepositories.answerMcSubmissionRepository.save(answerMcSubmission); }

    @Override
    public AnswerMcSubmission getAnswerMcSubmission(Long answerSubmissionId) {
        return allRepositories.answerMcSubmissionRepository.findByAnswerMcSubId(answerSubmissionId);
    }

    @Override
    public void updateAnswerMcSubmission(Long id, AnswerSubmissionDto answerSubmissionDto) throws AnswerNotMatchingException {
        AnswerMcSubmission answerMcSubmission = getAnswerMcSubmission(id);
        Optional<AnswerMc> answerMc = allRepositories.answerMcRepository.findById(answerSubmissionDto.getAnswerId());
        if (answerMc.isPresent()) {
            answerMcSubmission.setAnswerMc(answerMc.get());
        } else{
            throw new AnswerNotMatchingException(TextConstants.ANSWER_NOT_MATCHING);
        }
        saveAndFlushMC(answerMcSubmission);
    }

    @Override
    public void saveAndFlushMC(AnswerMcSubmission answerMcSubmission){ allRepositories.answerMcSubmissionRepository.saveAndFlush( answerMcSubmission); }

    @Override
    public void deleteByIdMC(Long id) throws EmptyResultDataAccessException{ allRepositories.answerMcSubmissionRepository.deleteByAnswerMcSubId(id); }

    @Override
    public boolean mcAnswerSubmissionBelongsToQuestionSubmission(Long questionSubmissionId, Long answerMcSubmissionId){
        return allRepositories.answerMcSubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerMcSubId(questionSubmissionId, answerMcSubmissionId);
    }



    /*
    ESSAY SUBMISSION METHODS
     */
    @Override
    public List<AnswerEssaySubmission> findAllByQuestionSubmissionIdEssay(Long questionSubmissionId){
        return allRepositories.answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmissionId);
    }

    @Override
    public List<AnswerSubmissionDto> getAnswerEssaySubmissions(Long questionSubmissionId){
        List<AnswerEssaySubmission> answerEssaySubmissions = findAllByQuestionSubmissionIdEssay(questionSubmissionId);
        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        for(AnswerEssaySubmission answerEssaySubmission : answerEssaySubmissions){
            answerSubmissionDtoList.add(toDtoEssay(answerEssaySubmission));
        }
        return answerSubmissionDtoList;
    }

    @Override
    public AnswerSubmissionDto toDtoEssay(AnswerEssaySubmission answer){
        AnswerSubmissionDto answerSubmissionDto = new AnswerSubmissionDto();
        answerSubmissionDto.setAnswerSubmissionId(answer.getAnswerEssaySubmissionId());
        answerSubmissionDto.setQuestionSubmissionId(answer.getQuestionSubmission().getQuestionSubmissionId());
        answerSubmissionDto.setResponse(answer.getResponse());
        return answerSubmissionDto;
    }

    @Override
    public AnswerEssaySubmission fromDtoEssay(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException{
        AnswerEssaySubmission answerEssaySubmission = new AnswerEssaySubmission();
        answerEssaySubmission.setAnswerEssaySubmissionId(answerSubmissionDto.getAnswerSubmissionId());
        answerEssaySubmission.setResponse(answerSubmissionDto.getResponse());
        Optional<QuestionSubmission> questionSubmission = questionSubmissionService.findById(answerSubmissionDto.getQuestionSubmissionId());
        if(questionSubmission.isPresent()){
            answerEssaySubmission.setQuestionSubmission(questionSubmission.get());
        } else {
            throw new DataServiceException("Question submission for answer submission does not exist.");
        }
        return answerEssaySubmission;
    }

    @Override
    public AnswerEssaySubmission saveEssay(AnswerEssaySubmission answer){ return allRepositories.answerEssaySubmissionRepository.save(answer); }

    @Override
    public Optional<AnswerEssaySubmission> findByIdEssay(Long id) { return allRepositories.answerEssaySubmissionRepository.findById(id); }

    @Override
    public AnswerEssaySubmission getAnswerEssaySubmission(Long answerSubmissionId){
        return allRepositories.answerEssaySubmissionRepository.findByAnswerEssaySubmissionId(answerSubmissionId);
    }

    @Override
    public void updateAnswerEssaySubmission(Long id, AnswerSubmissionDto answerSubmissionDto){
        AnswerEssaySubmission answerEssaySubmission = getAnswerEssaySubmission(id);
        answerEssaySubmission.setResponse(answerSubmissionDto.getResponse());
        saveAndFlushEssay(answerEssaySubmission);
    }

    @Override
    public void saveAndFlushEssay(AnswerEssaySubmission answer) { allRepositories.answerEssaySubmissionRepository.saveAndFlush(answer); }

    @Override
    public void deleteByIdEssay(Long id) { allRepositories.answerEssaySubmissionRepository.deleteByAnswerEssaySubmissionId(id); }

    @Override
    public boolean essayAnswerSubmissionBelongsToQuestionSubmission(Long questionSubmissionId, Long answerSubmissionId){
        return allRepositories.answerEssaySubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerEssaySubmissionId(questionSubmissionId, answerSubmissionId);
    }

    @Override
    public String getAnswerType(Long questionSubmissionId){
        QuestionSubmission questionSubmission = allRepositories.questionSubmissionRepository.findByQuestionSubmissionId(questionSubmissionId);
        return questionSubmission.getQuestion().getQuestionType().toString();
    }

    @Override
    @Transactional
    public AnswerSubmissionDto postAnswerSubmission(String answerType, AnswerSubmissionDto answerSubmissionDto) throws DataServiceException, TypeNotSupportedException{
        switch(answerType){
            case "MC":
                AnswerMcSubmission answerMcSubmission;
                try {
                    answerMcSubmission = fromDtoMC(answerSubmissionDto);
                } catch (DataServiceException ex) {
                    throw new DataServiceException("Error 105: Unable to create answer submission: " + ex.getMessage());
                }
                return (toDtoMC(saveMC(answerMcSubmission)));
            case "ESSAY":
                AnswerEssaySubmission answerEssaySubmission;
                try{
                    answerEssaySubmission = fromDtoEssay(answerSubmissionDto);
                } catch (DataServiceException ex) {
                    throw new DataServiceException("Error 105: Unable to create answer submission: " + ex.getMessage());
                }
                return (toDtoEssay(saveEssay(answerEssaySubmission)));
            default: throw new TypeNotSupportedException("Error 103: Answer type not supported.");
        }
    }


    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId, Long answerSubmissionId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path(
                "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions/{answer_submission_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, answerSubmissionId).toUri());
        return headers;
    }
}