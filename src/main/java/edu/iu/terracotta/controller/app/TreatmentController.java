package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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



    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<TreatmentDto>> allTreatmentsByCondition(@PathVariable("experiment_id") Long experimentId,
                                                                       @PathVariable("condition_id") Long conditionId,
                                                                       @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, AssessmentNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId,conditionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            List<TreatmentDto> treatmentList = treatmentService.getTreatments(conditionId, submissions);
            if(treatmentList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(treatmentList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
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
            TreatmentDto treatmentDto = treatmentService.toDto(treatmentService.getTreatment(treatmentId), submissions);
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
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, ExperimentLockedException, AssessmentNotMatchingException, IdInPostException, ExceedingLimitException, DataServiceException {

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


    /*
    Currently there is no functionality with the PUT endpoint. It is here in case there is need in the future.
     */
    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateTreatment(@PathVariable("experiment_id") Long experimentId,
                                                @PathVariable("condition_id") Long conditionId,
                                                @PathVariable("treatment_id") Long treatmentId,
                                                @RequestBody TreatmentDto treatmentDto,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, TreatmentNotMatchingException {

        log.debug("Updating treatment with id: {}", treatmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity("Currently there is no functionality at this endpoint.", HttpStatus.OK);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
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
                    AssessmentNotMatchingException, IdInPostException, ExceedingLimitException, DataServiceException {

        log.debug("Duplicating Treatment ID: {}", treatmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if(!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        TreatmentDto returnedDto = treatmentService.duplicateTreatment(treatmentId);
        HttpHeaders headers = treatmentService.buildHeaders(ucBuilder, experimentId, conditionId, returnedDto.getTreatmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

}