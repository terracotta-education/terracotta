package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.SubmissionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = SubmissionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class SubmissionController {

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(SubmissionController.class);

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    SubmissionService submissionService;



    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions", method = RequestMethod.GET,
                    produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<SubmissionDto>> getSubmissionsByAssessment(@PathVariable("experiment_id") Long experimentId,
                                                                          @PathVariable("condition_id") Long conditionId,
                                                                          @PathVariable("treatment_id") Long treatmentId,
                                                                          @PathVariable("assessment_id") Long assessmentId,
                                                                          HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException, NoSubmissionsException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
            List<SubmissionDto> submissionDtoList = submissionService.getSubmissions(experimentId, securedInfo.getUserId(), assessmentId, student);
            if(submissionDtoList.isEmpty()){
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(submissionDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}",
                    method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<SubmissionDto> getSubmission(@PathVariable("experiment_id") Long experimentId,
                                                       @PathVariable("condition_id") Long conditionId,
                                                       @PathVariable("treatment_id") Long treatmentId,
                                                       @PathVariable("assessment_id") Long assessmentId,
                                                       @PathVariable("submission_id") Long submissionId,
                                                       @RequestParam(name = "question_submissions", defaultValue = "false") boolean questionSubmissions,
                                                       @RequestParam(name = "submission_comments", defaultValue = "false") boolean submissionComments,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException, NoSubmissionsException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
            Submission submission = submissionService.getSubmission(experimentId, securedInfo.getUserId(), submissionId, student);
            return new ResponseEntity<>(submissionService.toDto(submission, questionSubmissions, submissionComments), HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions",
                    method = RequestMethod.POST)
    public ResponseEntity<SubmissionDto> postSubmission(@PathVariable("experiment_id") Long experimentId,
                                                        @PathVariable("condition_id") Long conditionId,
                                                        @PathVariable("treatment_id") Long treatmentId,
                                                        @PathVariable("assessment_id") Long assessmentId,
                                                        @RequestBody SubmissionDto submissionDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException, InvalidUserException, ParticipantNotMatchingException, IdInPostException, DataServiceException {

        log.debug("Creating Submission: {}", submissionDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (submissionService.datesAllowed(experimentId,treatmentId, securedInfo)) {
            if (apijwtService.isLearnerOrHigher(securedInfo)) {
                boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
                SubmissionDto returnedDto = submissionService.postSubmission(submissionDto, experimentId, securedInfo.getUserId(), assessmentId, student);
                HttpHeaders headers = submissionService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, returnedDto.getSubmissionId());
                return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
            } else {
                return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
            }
        } else {
            return new ResponseEntity("Error 128: Assignment locked", HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}",
                    method = RequestMethod.PUT)
    public ResponseEntity<Void> updateSubmission(@PathVariable("experiment_id") Long experimentId,
                                                 @PathVariable("condition_id") Long conditionId,
                                                 @PathVariable("treatment_id") Long treatmentId,
                                                 @PathVariable("assessment_id") Long assessmentId,
                                                 @PathVariable("submission_id") Long submissionId,
                                                 @RequestBody SubmissionDto submissionDto,
                                                 HttpServletRequest req)
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException {

        log.debug("Updating submission with id {}", submissionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
            submissionService.updateSubmission(submissionId, submissionDto, student);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}",
                    method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteSubmission(@PathVariable("experiment_id") Long experimentId,
                                                 @PathVariable("condition_id") Long conditionId,
                                                 @PathVariable("treatment_id") Long treatmentId,
                                                 @PathVariable("assessment_id") Long assessmentId,
                                                 @PathVariable("submission_id") Long submissionId,
                                                 HttpServletRequest req)
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            try{
                submissionService.deleteById(submissionId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.warn(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}