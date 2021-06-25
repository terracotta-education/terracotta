package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    public Optional<AnswerMcSubmission> findByIdMC(Long id) { return allRepositories.answerMcSubmissionRepository.findById(id); }

    @Override
    public void saveAndFlushMC(AnswerMcSubmission answerMcSubmission){ allRepositories.answerMcSubmissionRepository.saveAndFlush( answerMcSubmission); }

    @Override
    @Transactional
    public void saveAllAnswersMC(List<AnswerMcSubmission> answerMcSubmissions) { allRepositories.answerMcSubmissionRepository.saveAll(answerMcSubmissions); }

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
    public void saveAndFlushEssay(AnswerEssaySubmission answer) { allRepositories.answerEssaySubmissionRepository.saveAndFlush(answer); }

    @Override
    @Transactional
    public void saveAllAnswersEssay(List<AnswerEssaySubmission> answerList) { allRepositories.answerEssaySubmissionRepository.saveAll(answerList); }

    @Override
    public void deleteByIdEssay(Long id) { allRepositories.answerEssaySubmissionRepository.deleteByAnswerEssaySubmissionId(id); }

    @Override
    public boolean essayAnswerSubmissionBelongsToQuestionSubmission(Long questionSubmissionId, Long answerSubmissionId){
        return allRepositories.answerEssaySubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerEssaySubmissionId(questionSubmissionId, answerSubmissionId);
    }

    @Override
    public String answerSubmissionNotFound(SecurityInfo securityInfo, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId, Long answerSubmissionId){
        return "Answer submission in platform " + securityInfo.getPlatformDeploymentId() + " and context " + securityInfo.getContextId() + " and experiment with id " + experimentId + " and condition id " +
                conditionId + " and treatment id " + treatmentId + " and assessment id " + assessmentId + " and submission id " + submissionId + " and question submission id " + questionSubmissionId +
                " with id " + answerSubmissionId + " not found.";
    }

}
