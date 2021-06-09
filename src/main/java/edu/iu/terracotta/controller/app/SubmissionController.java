package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            List<Submission> submissionList = submissionService.findAllByAssessmentId(assessmentId);

            if(submissionList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<SubmissionDto> submissionDtoList = new ArrayList<>();
            for(Submission submission : submissionList) {
                submissionDtoList.add(submissionService.toDto(submission, false,false));
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securityInfo, assessmentId, submissionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            Optional<Submission> submissionSearchResult = submissionService.findById(submissionId);

            if(!submissionSearchResult.isPresent()) {
                log.error("Submission in platform {} and context {} and experiment {} and condition {} and treatment {}  and assessment {} with id {} not found",
                        securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), experimentId, conditionId, treatmentId, assessmentId, submissionId);
                return new ResponseEntity("Submission in platform " + securityInfo.getPlatformDeploymentId() + " and context " + securityInfo.getContextId() +
                        " and experiment with id " + experimentId + " and condition id " + conditionId + " and treatment id " + treatmentId + " and assessment id " +
                        assessmentId + " with id " + submissionId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                SubmissionDto submissionDto = submissionService.toDto(submissionSearchResult.get(), questionSubmissions, submissionComments);
                return new ResponseEntity<>(submissionDto, HttpStatus.OK);
            }
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
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException {

        log.info("Creating Submission: {}", submissionDto);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            if(submissionDto.getSubmissionId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            submissionDto.setAssessmentId(assessmentId);
            Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securityInfo.getUserId());
            submissionDto.setParticipantId(participant.getParticipantId());
            Submission submission;
            try {
                if(submissionDto.getAlteredCalculatedGrade() != null || submissionDto.getTotalAlteredGrade() != null){
                    if(!apijwtService.isInstructorOrHigher(securityInfo)){
                        return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS + " Students cannot alter the grades.", HttpStatus.UNAUTHORIZED);
                    }
                }
                submission = submissionService.fromDto(submissionDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Unable to create Submission: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Submission submissionSaved = submissionService.save(submission);
            SubmissionDto returnedDto = submissionService.toDto(submissionSaved, false,false);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}")
                    .buildAndExpand(submission.getAssessment().getTreatment().getCondition().getExperiment().getExperimentId(), submission.getAssessment().getTreatment().getCondition().getConditionId(),
                                    submission.getAssessment().getTreatment().getTreatmentId(), submission.getAssessment().getAssessmentId(), submission.getSubmissionId()).toUri());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
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

        log.info("Updating submission with id {}", submissionId);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securityInfo, assessmentId, submissionId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            Optional<Submission> submissionSearchResult = submissionService.findById(submissionId);

            if(!submissionSearchResult.isPresent()){
                log.error("Unable to update. Submission with id {} not found.", submissionId);
                return new ResponseEntity("Unable to update. Submission with id " + submissionId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            Submission submissionToChange = submissionSearchResult.get();
            submissionToChange.setAlteredCalculatedGrade(submissionDto.getAlteredCalculatedGrade());
            submissionToChange.setTotalAlteredGrade(submissionDto.getTotalAlteredGrade());
            submissionToChange.setLateSubmission(submissionDto.getLateSubmission());
            submissionToChange.setDateSubmitted(submissionDto.getDateSubmitted());

            submissionService.saveAndFlush(submissionToChange);
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

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securityInfo, assessmentId, submissionId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            try{
                submissionService.deleteById(submissionId);
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
