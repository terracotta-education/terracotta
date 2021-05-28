package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.Answer;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.service.app.TreatmentService;
import edu.iu.terracotta.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping(value = AssessmentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AssessmentController {

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(AssessmentController.class);

    @Autowired
    AssessmentService assessmentService;

    @Autowired
    QuestionService questionService;

    @Autowired
    AnswerService answerService;

    @Autowired
    TreatmentService treatmentService;

    @Autowired
    APIJWTService apijwtService;


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<AssessmentDto>> getAssessmentByTreatment(@PathVariable("experiment_id") Long experimentId,
                                                                        @PathVariable("condition_id") Long conditionId,
                                                                        @PathVariable("treatment_id") Long treatmentId,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, TreatmentNotMatchingException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.treatmentAllowed(securityInfo, experimentId, conditionId, treatmentId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            List<Assessment> assessmentList = assessmentService.findAllByTreatmentId(treatmentId);

            if(assessmentList.isEmpty()) {
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            }
            List<AssessmentDto> assessmentDtos = new ArrayList<>();
            for(Assessment assessment : assessmentList){
                assessmentDtos.add(assessmentService.toDto(assessment));
            }
            return new ResponseEntity<>(assessmentDtos, HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}",
                    method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<AssessmentDto> getAssessment(@PathVariable("experiment_id") Long experimentId,
                                                       @PathVariable("condition_id") Long conditionId,
                                                       @PathVariable("treatment_id") Long treatmentId,
                                                       @PathVariable("assessment_id") Long assessmentId,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssessmentNotMatchingException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            Optional<Assessment> assessmentSearchResult = assessmentService.findById(assessmentId);

            if(!assessmentSearchResult.isPresent()) {
                log.error("assessment in platform {} and context {} and experiment {} and condition {} and treatment {} with id {} not found",
                        securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), experimentId, conditionId, treatmentId, assessmentId);
                return new ResponseEntity("Assessment in platform " + securityInfo.getPlatformDeploymentId() +
                        " and context " + securityInfo.getContextId() + " and experiment with id " + experimentId + " and condition id " + conditionId +
                        " and treatment id " + treatmentId + " with id " + assessmentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                AssessmentDto assessmentDto = assessmentService.toDto(assessmentSearchResult.get());
                return new ResponseEntity<>(assessmentDto, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments", method = RequestMethod.POST)
    public ResponseEntity<AssessmentDto> postAssessment(@PathVariable("experiment_id") Long experimentId,
                                                        @PathVariable("condition_id") Long conditionId,
                                                        @PathVariable("treatment_id") Long treatmentId,
                                                        @RequestBody AssessmentDto assessmentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, BadTokenException {

        log.info("Creating Assessment: {}", assessmentDto);
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.treatmentAllowed(securityInfo, experimentId, conditionId, treatmentId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            if(assessmentDto.getAssessmentId() != null) {
                log.error("Cannot include id in the POST endpoint. To modify existing assessment you must use PUT");
                return new ResponseEntity("Cannot include id in the POST endpoint. To modify existing assessment you must use PUT", HttpStatus.CONFLICT);
            }

            assessmentDto.setTreatmentId(treatmentId);
            Assessment assessment = null;
            try {
                assessment = assessmentService.fromDto(assessmentDto);
                assessment.setQuestions(new ArrayList<>());
            } catch (DataServiceException ex) {
                return new ResponseEntity("Unable to create Assessment: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Assessment assessmentSaved = assessmentService.save(assessment);
            Optional<Treatment> treatment = treatmentService.findById(treatmentId);
            if(treatment.isPresent()){
                Treatment treatment1 = treatment.get();
                treatment1.setAssessment(assessmentSaved);
                treatmentService.saveAndFlush(treatment1);
            }
            AssessmentDto returnedDto = assessmentService.toDto(assessmentSaved);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}")
                    .buildAndExpand(assessment.getTreatment().getCondition().getExperiment().getExperimentId(), assessment.getTreatment().getCondition().getConditionId(),
                                    assessment.getTreatment().getTreatmentId(), assessment.getAssessmentId()).toUri());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateAssessment(@PathVariable("experiment_id") Long experimentId,
                                                 @PathVariable("condition_id") Long conditionId,
                                                 @PathVariable("treatment_id") Long treatmentId,
                                                 @PathVariable("assessment_id") Long assessmentId,
                                                 @RequestBody AssessmentDto assessmentDto,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException {

        log.info("Updating assessment with id: {}", assessmentId);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            Optional<Assessment> assessmentSearchResult = assessmentService.findById(assessmentId);

            if(!assessmentSearchResult.isPresent()) {
                log.error("Unable to update. Assessment with id {} not found.", assessmentId);
                return new ResponseEntity("Unable to update. Assessment with id " + assessmentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            Assessment assessmentToChange = assessmentSearchResult.get();
            assessmentToChange.setHtml(assessmentDto.getHtml());
            assessmentToChange.setTitle(assessmentDto.getTitle());

            assessmentService.saveAndFlush(assessmentToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteAssessment(@PathVariable("experiment_id") Long experimentId,
                                                 @PathVariable("condition_id") Long conditionId,
                                                 @PathVariable("treatment_id") Long treatmentId,
                                                 @PathVariable("assessment_id") Long assessmentId,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            try{
                Optional<Treatment> treatment = treatmentService.findById(treatmentId);
                if(treatment.isPresent()){
                    Treatment treatment1 = treatment.get();
                    //this line deletes the assessment from DB
                    treatment1.setAssessment(null);
                    treatmentService.saveAndFlush(treatment1);
                }
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.error(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    /**
     *Begin endpoints for Question
     */

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions",
                    method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<QuestionDto>> getQuestionsByAssessment(@PathVariable("experiment_id") Long experimentId,
                                                                      @PathVariable("condition_id") Long conditionId,
                                                                      @PathVariable("treatment_id") Long treatmentId,
                                                                      @PathVariable("assessment_id") Long assessmentId,
                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        //TODO I've noticed that in these methods, security info isn't actually used.
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            List<Question> questionList = questionService.findAllByAssessmentId(assessmentId);

            if(questionList.isEmpty()) {
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            }
            List<QuestionDto> questionDtos = new ArrayList<>();
            for(Question question : questionList) {
                questionDtos.add(questionService.toDto(question));
            }
            return new ResponseEntity<>(questionDtos, HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}",
            method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<QuestionDto> getQuestion(@PathVariable("experiment_id") Long experimentId,
                                                   @PathVariable("condition_id") Long conditionId,
                                                   @PathVariable("treatment_id") Long treatmentId,
                                                   @PathVariable("assessment_id") Long assessmentId,
                                                   @PathVariable("question_id") Long questionId,
                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securityInfo, assessmentId, questionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)){
            Optional<Question> questionSearchResult = questionService.findById(questionId);

            if(!questionSearchResult.isPresent()) {
                log.error("question in platform {} and context {} and experiment {} and condition {} and treatment {} and assessment {} with id {} not found",
                        securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), experimentId, conditionId, treatmentId, assessmentId, questionId);
                return new ResponseEntity("Question in platform " + securityInfo.getPlatformDeploymentId() + " and context "+ securityInfo.getContextId()
                + " and experiment with id " + experimentId + " and condition id " + conditionId + " and treatment id " + treatmentId + " and assessment id " + assessmentId
                + " with id " + questionId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                QuestionDto questionDto = questionService.toDto(questionSearchResult.get());
                return new ResponseEntity<>(questionDto, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions", method = RequestMethod.POST)
    public ResponseEntity<QuestionDto> postQuestion(@PathVariable("experiment_id") Long experimentId,
                                                    @PathVariable("condition_id") Long conditionId,
                                                    @PathVariable("treatment_id") Long treatmentId,
                                                    @PathVariable("assessment_id") Long assessmentId,
                                                    @RequestBody QuestionDto questionDto,
                                                    UriComponentsBuilder ucBuilder,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            if(questionDto.getQuestionId() != null) {
                log.error("Cannot include id in the POST endpoint. To modify existing question you must use PUT");
                return new ResponseEntity("Cannot include id in the POST endpoint. To modify existing question you must use PUT", HttpStatus.CONFLICT);
            }

            questionDto.setAssessmentId(assessmentId);
            Question question = null;
            try {
                question = questionService.fromDto(questionDto);
                question.setAnswers(new ArrayList<>());
            } catch (DataServiceException ex) {
                return new ResponseEntity("Unable to create Question: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Question questionSaved = questionService.save(question);
            QuestionDto returnedDto = questionService.toDto(questionSaved);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}")
                    .buildAndExpand(question.getAssessment().getTreatment().getCondition().getExperiment().getExperimentId(), question.getAssessment().getTreatment().getCondition().getConditionId(),
                            question.getAssessment().getTreatment().getTreatmentId(), question.getAssessment().getAssessmentId(), question.getQuestionId()).toUri());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}",
                    method = RequestMethod.PUT)
    public ResponseEntity<Void> updateQuestion(@PathVariable("experiment_id") Long experimentId,
                                               @PathVariable("condition_id") Long conditionId,
                                               @PathVariable("treatment_id") Long treatmentId,
                                               @PathVariable("assessment_id") Long assessmentId,
                                               @PathVariable("question_id") Long questionId,
                                               @RequestBody QuestionDto questionDto,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException {

        log.info("Updating question with id: {}", questionId);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securityInfo, assessmentId, questionId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            Optional<Question> questionSearchResult = questionService.findById(questionId);

            if(!questionSearchResult.isPresent()) {
                log.error("Unable to update. Question with id {} not found.", questionId);
                return new ResponseEntity("Unable to update. Question with id " + questionId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            Question questionToChange = questionSearchResult.get();
            questionToChange.setHtml(questionDto.getHtml());
            questionToChange.setPoints(questionDto.getPoints());

            questionService.saveAndFlush(questionToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}",
                    method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteQuestion(@PathVariable("experiment_id") Long experimentId,
                                               @PathVariable("condition_id") Long conditionId,
                                               @PathVariable("treatment_id") Long treatmentId,
                                               @PathVariable("assessment_id") Long assessmentId,
                                               @PathVariable("question_id") Long questionId,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securityInfo, assessmentId, questionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            try{
                questionService.deleteById(questionId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.error(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    /**
     * Begin endpoints for Answer
     */

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers",
            method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<AnswerDto>> getAnswersByQuestion(@PathVariable("experiment_id") Long experimentId,
                                                                @PathVariable("condition_id") Long conditionId,
                                                                @PathVariable("treatment_id") Long treatmentId,
                                                                @PathVariable("assessment_id") Long assessmentId,
                                                                @PathVariable("question_id") Long questionId,
                                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securityInfo, assessmentId, questionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            List<Answer> answerList = answerService.findAllByQuestionId(questionId);

            if(answerList.isEmpty()) {
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            }
            List<AnswerDto> answerDtos= new ArrayList<>();
            for(Answer answer : answerList) {
                answerDtos.add(answerService.toDto(answer));
            }
            return new ResponseEntity<>(answerDtos, HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers/{answer_id}",
                    method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<AnswerDto> getAnswer(@PathVariable("experiment_id") Long experimentId,
                                               @PathVariable("condition_id") Long conditionId,
                                               @PathVariable("treatment_id") Long treatmentId,
                                               @PathVariable("assessment_id") Long assessmentId,
                                               @PathVariable("question_id") Long questionId,
                                               @PathVariable("answer_id") Long answerId,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, AnswerNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.answerAllowed(securityInfo, assessmentId, questionId, answerId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            Optional<Answer> answerSearchResult = answerService.findById(answerId);

            if(!answerSearchResult.isPresent()) {
                log.error("answer in platform {} and context {} and experiment {} and condition {} and treatment {} and assessment {} and question {}with id {} not found",
                        securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), experimentId, conditionId, treatmentId, assessmentId, questionId, answerId);
                return new ResponseEntity("Answer in platform " + securityInfo.getPlatformDeploymentId() + " and context " + securityInfo.getContextId()
                        + " and experiment with id " + experimentId + " and condition id " + conditionId + " and treatment id " + treatmentId + " and assessment id " + assessmentId
                        + " and question id " + questionId + " with id " + answerId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                AnswerDto answerDto = answerService.toDto(answerSearchResult.get());
                return new ResponseEntity<>(answerDto, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers",
                    method = RequestMethod.POST)
    public ResponseEntity<AnswerDto> postAnswer(@PathVariable("experiment_id") Long experimentId,
                                                @PathVariable("condition_id") Long conditionId,
                                                @PathVariable("treatment_id") Long treatmentId,
                                                @PathVariable("assessment_id") Long assessmentId,
                                                @PathVariable("question_id") Long questionId,
                                                @RequestBody AnswerDto answerDto,
                                                UriComponentsBuilder ucBuilder,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securityInfo, assessmentId, questionId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            if(answerDto.getAnswerId() != null){
                log.error("Cannot include id in the POST endpoint. To modify existing answer you must use PUT");
                return new ResponseEntity("Cannot include id in the POST endpoint. To modify existing question you must use PUT", HttpStatus.CONFLICT);
            }

            answerDto.setQuestionId(questionId);
            Answer answer = null;
            try {
                answer = answerService.fromDto(answerDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Unable to create Answer: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Answer answerSaved = answerService.save(answer);
            AnswerDto returnedDto = answerService.toDto(answerSaved);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers/{answer_id}")
                    .buildAndExpand(answer.getQuestion().getAssessment().getTreatment().getCondition().getExperiment().getExperimentId(),
                            answer.getQuestion().getAssessment().getTreatment().getCondition(), answer.getQuestion().getAssessment().getTreatment().getTreatmentId(),
                            answer.getQuestion().getAssessment().getAssessmentId(), answer.getQuestion().getQuestionId(), answer.getAnswerId()).toUri());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers/{answer_id}",
                    method = RequestMethod.PUT)
    public ResponseEntity<Void> updateAnswer(@PathVariable("experiment_id") Long experimentId,
                                             @PathVariable("condition_id") Long conditionId,
                                             @PathVariable("treatment_id") Long treatmentId,
                                             @PathVariable("assessment_id") Long assessmentId,
                                             @PathVariable("question_id") Long questionId,
                                             @PathVariable("answer_id") Long answerId,
                                             @RequestBody AnswerDto answerDto,
                                             HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, AnswerNotMatchingException, BadTokenException {

        log.info("Updating answer with id: {}", answerId);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.answerAllowed(securityInfo, assessmentId, questionId, answerId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            Optional<Answer> answerSearchResult = answerService.findById(answerId);

            if(!answerSearchResult.isPresent()) {
                log.error("Unable to update. Answer with id {} not found.", answerId);
                return new ResponseEntity("Unable to update. Answer with id {} not found " + answerId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            Answer answerToChange = answerSearchResult.get();
            answerToChange.setHtml(answerDto.getHtml());
            answerToChange.setCorrect(answerDto.getCorrect());

            answerService.saveAndFlush(answerToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers/{answer_id}",
                    method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteAnswer(@PathVariable("experiment_id") Long experimentId,
                                             @PathVariable("condition_id") Long conditionId,
                                             @PathVariable("treatment_id") Long treatmentId,
                                             @PathVariable("assessment_id") Long assessmentId,
                                             @PathVariable("question_id") Long questionId,
                                             @PathVariable("answer_id") Long answerId,
                                             HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, AnswerNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.answerAllowed(securityInfo, assessmentId, questionId, answerId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            try{
                answerService.deleteById(answerId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.error(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}