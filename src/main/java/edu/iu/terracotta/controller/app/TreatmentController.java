package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping(value = TreatmentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class TreatmentController {

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(TreatmentController.class);

    @Autowired
    TreatmentService treatmentService;

    @Autowired
    AssignmentService assignmentService;

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    AssessmentService assessmentService;

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
            List<Treatment> treatmentList = treatmentService.findAllByConditionId(conditionId);

            if(treatmentList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<TreatmentDto> treatmentDtoList = new ArrayList<>();
            for(Treatment treatment : treatmentList) {
                treatmentDtoList.add(treatmentService.toDto(treatment, submissions));
            }
            return new ResponseEntity<>(treatmentDtoList, HttpStatus.OK);
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
            Optional<Treatment> treatmentSearchResult = treatmentService.findById(treatmentId);

            if(!treatmentSearchResult.isPresent()) {
                log.error("treatment in platform {} and context {} and experiment {} and condition {} with id {} not found",
                        securedInfo.getPlatformDeploymentId(), securedInfo.getContextId(), experimentId, conditionId, treatmentId);

                return new ResponseEntity("treatment in platform " + securedInfo.getPlatformDeploymentId()
                        + " and context " + securedInfo.getContextId() + " and experiment with id " + experimentId + " and condition id " + conditionId
                        + " with id " + treatmentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                TreatmentDto treatmentDto = treatmentService.toDto(treatmentSearchResult.get(), submissions);
                return new ResponseEntity<>(treatmentDto, HttpStatus.OK);
            }
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
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, ExperimentLockedException, AssessmentNotMatchingException {

        log.info("Creating Treatment: {}", treatmentDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            if(treatmentDto.getTreatmentId() != null) {
                log.error("Cannot include id in the POST endpoint. To modify existing treatments you must use PUT");
                return new ResponseEntity("Cannot include id in the POST endpoint. To modify existing treatments you must use PUT", HttpStatus.CONFLICT);
            }

            treatmentDto.setConditionId(conditionId);
            if (treatmentDto.getAssignmentId()==null){
                return new ResponseEntity("Unable to create Treatment: The assignmentId is mandatory", HttpStatus.BAD_REQUEST);
            }
            Treatment treatment;
            try{
                treatment = treatmentService.fromDto(treatmentDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Unable to create Treatment: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Treatment treatmentSaved = treatmentService.save(treatment);
            TreatmentDto returnedDto = treatmentService.toDto(treatmentSaved, false);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}")
                .buildAndExpand(experimentId, conditionId, treatment.getTreatmentId()).toUri());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateTreatment(@PathVariable("experiment_id") Long experimentId,
                                                @PathVariable("condition_id") Long conditionId,
                                                @PathVariable("treatment_id") Long treatmentId,
                                                @RequestBody TreatmentDto treatmentDto,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, TreatmentNotMatchingException, AssignmentNotMatchingException, DataServiceException {

        log.info("Updating treatment with id: {}", treatmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            Optional<Treatment> treatmentSearchResult = treatmentService.findById(treatmentId);

            if(!treatmentSearchResult.isPresent()) {
                log.error("Unable to update. Treatment with id {} not found.", treatmentId);
                return new ResponseEntity("Unable to update. Treatment with id " + treatmentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            Treatment treatmentToChange = treatmentSearchResult.get();
            if (treatmentDto.getAssignmentId()==null){
                return new ResponseEntity("Unable to create Treatment: The assignmentId is mandatory", HttpStatus.BAD_REQUEST);
            }
            Optional<Assignment> assignment = assignmentService.findById(treatmentDto.getAssignmentId());
            if (assignment.isPresent()) {
                apijwtService.assignmentAllowed(securedInfo, experimentId, treatmentDto.getAssignmentId());
                treatmentToChange.setAssignment(assignment.get());
            } else {
                throw new DataServiceException("The assignment for the treatment does not exist");
            }
            treatmentService.saveAndFlush(treatmentToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_FOUND_SUFFIX, HttpStatus.UNAUTHORIZED);
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
                Optional<Treatment> treatment = treatmentService.findById(treatmentId);
                if(treatment.isPresent()){
                    //this line deletes the corresponding assessment (if there is one)
                    treatment.get().setAssessment(null);
                    treatmentService.saveAndFlush(treatment.get());
                }
                treatmentService.deleteById(treatmentId);
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
