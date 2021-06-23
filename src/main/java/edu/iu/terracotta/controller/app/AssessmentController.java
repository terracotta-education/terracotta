package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.TreatmentService;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
    TreatmentService treatmentService;

    @Autowired
    SubmissionService submissionService;

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
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<AssessmentDto> assessmentDtoList = new ArrayList<>();
            for(Assessment assessment : assessmentList){
                assessmentDtoList.add(assessmentService.toDto(assessment,false, false));
            }
            return new ResponseEntity<>(assessmentDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}",
                    method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<AssessmentDto> getAssessment(@PathVariable("experiment_id") Long experimentId,
                                                       @PathVariable("condition_id") Long conditionId,
                                                       @PathVariable("treatment_id") Long treatmentId,
                                                       @PathVariable("assessment_id") Long assessmentId,
                                                       @RequestParam(name = "questions", defaultValue = "false") boolean questions,
                                                       @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
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
                AssessmentDto assessmentDto = assessmentService.toDto(assessmentSearchResult.get(), questions, submissions);
                return new ResponseEntity<>(assessmentDto, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
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
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            assessmentDto.setTreatmentId(treatmentId);
            if(assessmentDto.getNumOfSubmissions() == null) {
                assessmentDto.setNumOfSubmissions(1);
            }
            //TODO how to check if autoSubmit is not present in the dto
            assessmentDto.setAutoSubmit(true);
            Assessment assessment;
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
            AssessmentDto returnedDto = assessmentService.toDto(assessmentSaved, false,false);

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
            assessmentToChange.setAutoSubmit(assessmentDto.getAutoSubmit());
            assessmentToChange.setNumOfSubmissions(assessmentDto.getNumOfSubmissions());

            assessmentService.saveAndFlush(assessmentToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @Transactional
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
                Optional<Assessment> assessment = assessmentService.findById(assessmentId);
                if(assessment.isPresent()){
                    for(Submission submission : assessment.get().getSubmissions()){
                        submissionService.deleteById(submission.getSubmissionId());
                    }
                }
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
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            List<Question> questionList = questionService.findAllByAssessmentId(assessmentId);

            if(questionList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<QuestionDto> questionDtoList = new ArrayList<>();
            for(Question question : questionList) {
                questionDtoList.add(questionService.toDto(question, false));
            }
            return new ResponseEntity<>(questionDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
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
                                                   @RequestParam(name = "answers", defaultValue = "false") boolean answers,
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
                QuestionDto questionDto = questionService.toDto(questionSearchResult.get(), answers);
                return new ResponseEntity<>(questionDto, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions", method = RequestMethod.POST)
    public ResponseEntity<QuestionDto> postQuestion(@PathVariable("experiment_id") Long experimentId,
                                                    @PathVariable("condition_id") Long conditionId,
                                                    @PathVariable("treatment_id") Long treatmentId,
                                                    @PathVariable("assessment_id") Long assessmentId,
                                                    @RequestParam(name = "submissions", defaultValue = "false") boolean answers,
                                                    @RequestBody QuestionDto questionDto,
                                                    UriComponentsBuilder ucBuilder,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException {

        log.info("Creating Question: {}", questionDto);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            if(questionDto.getQuestionId() != null) {
                log.error("Cannot include id in the POST endpoint. To modify existing question you must use PUT");
                return new ResponseEntity("Cannot include id in the POST endpoint. To modify existing question you must use PUT", HttpStatus.CONFLICT);
            }

            questionDto.setAssessmentId(assessmentId);
            Question question;
            try {
                if(questionDto.getQuestionType() == null){
                    return new ResponseEntity("Must include a question type in the post.", HttpStatus.BAD_REQUEST);
                }
                if(!EnumUtils.isValidEnum(QuestionTypes.class, questionDto.getQuestionType())){
                    return new ResponseEntity("Please use a supported question type.", HttpStatus.BAD_REQUEST);
                }
                question = questionService.fromDto(questionDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Unable to create Question: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Question questionSaved = questionService.save(question);
            QuestionDto returnedDto = questionService.toDto(questionSaved, answers);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}")
                    .buildAndExpand(question.getAssessment().getTreatment().getCondition().getExperiment().getExperimentId(), question.getAssessment().getTreatment().getCondition().getConditionId(),
                            question.getAssessment().getTreatment().getTreatmentId(), question.getAssessment().getAssessmentId(), question.getQuestionId()).toUri());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateQuestions(@PathVariable("experiment_id") Long experimentId,
                                                @PathVariable("condition_id") Long conditionId,
                                                @PathVariable("treatment_id") Long treatmentId,
                                                @PathVariable("assessment_id") Long assessmentId,
                                                @RequestBody List<QuestionDto> questionDtoList,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException, DataServiceException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            List<Question> questionList = new ArrayList<>();

            for(QuestionDto questionDto : questionDtoList){
                apijwtService.questionAllowed(securityInfo, assessmentId, questionDto.getQuestionId());
                Optional<Question> question = questionService.findById(questionDto.getQuestionId());
                if(question.isPresent()){
                    Question questionToChange = question.get();
                    questionToChange.setHtml(questionDto.getHtml());
                    questionToChange.setQuestionOrder(questionDto.getQuestionOrder());
                    questionToChange.setPoints(questionDto.getPoints());
                    questionList.add(questionToChange);
                }
            }
            try{
                questionService.saveAllQuestions(questionList);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception ex) {
                throw new DataServiceException("An error occurred trying to update the question list. No questions were updated. " + ex.getMessage());
            }
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
            questionToChange.setQuestionOrder(questionDto.getQuestionOrder());
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

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
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
}