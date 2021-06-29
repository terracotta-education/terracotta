package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.dto.ExposureDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.StringUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping(value = ExposureController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ExposureController {

    static final Logger log = LoggerFactory.getLogger(ExposureController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    ExposureService exposureService;

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    ExperimentService experimentService;

    @RequestMapping(value = "/{experiment_id}/exposures", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<ExposureDto>> allExposuresByExperiment(@PathVariable("experiment_id") Long experimentId,
                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            List<Exposure> exposureList =
                    exposureService.findAllByExperimentId(experimentId);
            if(exposureList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<ExposureDto> exposureDtoList = new ArrayList<>();
            for(Exposure exposure : exposureList) {
                exposureDtoList.add(exposureService.toDto(exposure));
            }
            return new ResponseEntity<>(exposureDtoList, HttpStatus.OK);
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
            Optional<Exposure> exposure = exposureService.findById(exposureId);

            if(!exposure.isPresent()) {
                log.error("exposure {} in experiment {} not found.", exposureId, experimentId);
                return new ResponseEntity("exposure " + exposureId + " in experiment " + experimentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                ExposureDto exposureDto = exposureService.toDto(exposure.get());
                return new ResponseEntity<>(exposureDto, HttpStatus.OK);
            }
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/exposures", method = RequestMethod.POST)
    public ResponseEntity<ExposureDto> postExposure(@PathVariable("experiment_id") Long experimentId,
                                                    @RequestBody ExposureDto exposureDto,
                                                    UriComponentsBuilder ucBuilder,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException {

        log.info("Creating Exposure : {}", exposureDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.experimentLocked(experimentId,true);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            if(exposureDto.getExposureId() != null) {
                log.error("Cannot include id in the POST endpoint. To modify existing exposures you must use PUT");
                return new ResponseEntity("Cannot include id in the POST endpoint. To modify existing exposures you must use PUT", HttpStatus.CONFLICT);
            }

            if(!StringUtils.isAllBlank(exposureDto.getTitle()) && exposureDto.getTitle().length() > 255){
                return new ResponseEntity("Title must be 255 characters or less.", HttpStatus.BAD_REQUEST);
            }
            exposureDto.setExperimentId(experimentId);
            Exposure exposure;
            try{
                exposure = exposureService.fromDto(exposureDto);
            } catch (DataServiceException e) {
                return new ResponseEntity("Unable to create exposure:" + e.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Exposure exposureSaved = exposureService.save(exposure);
            ExposureDto returnedDto = exposureService.toDto(exposureSaved);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/exposures/{id}").buildAndExpand(experimentId, exposure.getExposureId()).toUri());
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
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException {

        log.info("Updating exposure with id {}", exposureId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            Optional<Exposure> exposureSearchResult = exposureService.findById(exposureId);

            if(!exposureSearchResult.isPresent()) {
                log.error("Unable to update. Exposure with id {} not found.", exposureId);
                return new ResponseEntity("Unable to update. Exposure with id  " + exposureId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            if(StringUtils.isAllBlank(exposureDto.getTitle()) && StringUtils.isAllBlank(exposureSearchResult.get().getTitle())){
                return new ResponseEntity("Please give the exposure a title.", HttpStatus.CONFLICT);
            }
            if(!StringUtils.isAllBlank(exposureDto.getTitle()) && exposureDto.getTitle().length() > 255) {
                return new ResponseEntity("Title must be 255 characters or less.", HttpStatus.BAD_REQUEST);
            }
            Exposure exposureToChange = exposureSearchResult.get();
            exposureToChange.setTitle(exposureDto.getTitle());

            exposureService.saveAndFlush(exposureToChange);
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
                log.error(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}
