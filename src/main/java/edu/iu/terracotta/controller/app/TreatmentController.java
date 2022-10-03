package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMismatchException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@SuppressWarnings({"rawtypes","unchecked"})
@RequestMapping(value = TreatmentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class TreatmentController {

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(TreatmentController.class);

    @Autowired
    private TreatmentService treatmentService;

    @Autowired
    private APIJWTService apijwtService;



    @GetMapping(value = "/{experimentId}/conditions/{conditionId}/treatments", produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<TreatmentDto>> allTreatmentsByCondition(@PathVariable long experimentId,
                                                                       @PathVariable long conditionId,
                                                                       @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, AssessmentNotMatchingException, NumberFormatException, CanvasApiException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId,conditionId);

        if(!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<TreatmentDto> treatmentList = treatmentService.getTreatments(conditionId, securedInfo.getCanvasCourseId(), securedInfo.getPlatformDeploymentId(), submissions);

        if(treatmentList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(treatmentList, HttpStatus.OK);
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<TreatmentDto> getTreatment(@PathVariable("experiment_id") Long experimentId,
                                                     @PathVariable("condition_id") Long conditionId,
                                                     @PathVariable("treatment_id") Long treatmentId,
                                                     @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                     HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, TreatmentNotMatchingException, AssessmentNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);
        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            TreatmentDto treatmentDto = treatmentService.toDto(treatmentService.getTreatment(treatmentId), submissions, true);
            return new ResponseEntity<>(treatmentDto, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments", method = RequestMethod.POST)
    public ResponseEntity<TreatmentDto> postTreatment(@PathVariable("experiment_id") Long experimentId,
                                                      @PathVariable("condition_id") Long conditionId,
                                                      @RequestBody TreatmentDto treatmentDto,
                                                      UriComponentsBuilder ucBuilder,
                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, ExperimentLockedException, AssessmentNotMatchingException, IdInPostException, ExceedingLimitException, DataServiceException, TreatmentNotMatchingException {

        log.debug("Creating Treatment: {}", treatmentDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            TreatmentDto returnedDto = treatmentService.postTreatment(treatmentDto, conditionId);
            HttpHeaders headers = treatmentService.buildHeaders(ucBuilder, experimentId, conditionId, returnedDto.getTreatmentId());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @PutMapping("/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}")
    public ResponseEntity<Void> updateTreatment(@PathVariable long experimentId,
                                                @PathVariable long conditionId,
                                                @PathVariable long treatmentId,
                                                @RequestBody TreatmentDto treatmentDto,
                                                @RequestParam(name = "questions", defaultValue = "true") boolean questions,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, TreatmentNotMatchingException, IdInPostException, DataServiceException,
                ExceedingLimitException, AssessmentNotMatchingException, IdMissingException, IdMismatchException,
                TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
                CanvasApiException, AssignmentNotEditedException, NegativePointsException, QuestionNotMatchingException {

        log.debug("Updating treatment with id: {}", treatmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);

        if(!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity(treatmentService.putTreatment(treatmentDto, treatmentId, securedInfo, questions), HttpStatus.OK);
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteTreatment(@PathVariable("experiment_id") Long experimentId,
                                                @PathVariable("condition_id") Long conditionId,
                                                @PathVariable("treatment_id") Long treatmentId,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, TreatmentNotMatchingException, ExperimentLockedException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            try{
                treatmentService.deleteById(treatmentId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.warn(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/duplicate")
    public ResponseEntity<TreatmentDto> duplicateTreatment(@PathVariable Long experimentId,
                                                          @PathVariable Long conditionId,
                                                          @PathVariable Long treatmentId,
                                                          UriComponentsBuilder ucBuilder,
                                                          HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, ExperimentLockedException,
                    AssessmentNotMatchingException, IdInPostException, ExceedingLimitException, DataServiceException, NumberFormatException, CanvasApiException, TreatmentNotMatchingException {

        log.debug("Duplicating Treatment ID: {}", treatmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if(!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        TreatmentDto returnedDto = treatmentService.duplicateTreatment(treatmentId, securedInfo.getCanvasCourseId(), securedInfo.getPlatformDeploymentId());
        HttpHeaders headers = treatmentService.buildHeaders(ucBuilder, experimentId, conditionId, returnedDto.getTreatmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

}