package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.dto.ExposureDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.utils.TextConstants;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = ExposureController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ExposureController {

    static final Logger log = LoggerFactory.getLogger(ExposureController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    ExposureService exposureService;

    @Autowired
    APIJWTService apijwtService;



    @RequestMapping(value = "/{experiment_id}/exposures", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<ExposureDto>> allExposuresByExperiment(@PathVariable("experiment_id") Long experimentId,
                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            List<ExposureDto> exposureList = exposureService.getExposures(experimentId);
            if(exposureList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(exposureList, HttpStatus.OK);
        }else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<ExposureDto> getExposure(@PathVariable("experiment_id") long experimentId,
                                                   @PathVariable("exposure_id") long exposureId,
                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            ExposureDto exposureDto = exposureService.toDto(exposureService.getExposure(exposureId));
            return new ResponseEntity<>(exposureDto, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/exposures", method = RequestMethod.POST)
    public ResponseEntity<ExposureDto> postExposure(@PathVariable("experiment_id") Long experimentId,
                                                    @RequestBody ExposureDto exposureDto,
                                                    UriComponentsBuilder ucBuilder,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, TitleValidationException, IdInPostException, DataServiceException {

        log.debug("Creating Exposure : {}", exposureDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.experimentLocked(experimentId,true);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            ExposureDto returnedDto = exposureService.postExposure(exposureDto, experimentId);
            HttpHeaders headers = exposureService.buildHeaders(ucBuilder, experimentId, returnedDto.getExposureId());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateExposure(@PathVariable("experiment_id") Long experimentId,
                                               @PathVariable("exposure_id") Long exposureId,
                                               @RequestBody ExposureDto exposureDto,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException, TitleValidationException {

        log.debug("Updating exposure with id {}", exposureId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            exposureService.updateExposure(exposureId, exposureDto);
            return new ResponseEntity<>(HttpStatus.OK);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteExposure(@PathVariable("experiment_id") Long experimentId,
                                               @PathVariable("exposure_id") Long exposureId,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException, ExperimentLockedException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            try{
                exposureService.deleteById(exposureId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.warn(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}