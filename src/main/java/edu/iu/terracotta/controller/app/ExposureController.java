package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.dto.ExposureDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.ExperimentService;
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

    @RequestMapping(value = "/{experiment_id}/exposures/", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<ExposureDto>> allExposuresByExperiment(@PathVariable("experiment_id") Long experimentId, HttpServletRequest req) {
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if(securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        //TODO should this be Learner or higher? Is there a reason a student would need to see exposure type?
        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            List<Exposure> exposureList =
                    exposureService.findAllByExperimentId(experimentId);
            if(exposureList.isEmpty()) {
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            }
            List<ExposureDto> exposureDtos = new ArrayList<>();
            for(Exposure exposure : exposureList) {
                exposureDtos.add(exposureService.toDto(exposure));
            }
            return new ResponseEntity<>(exposureDtos, HttpStatus.OK);
        }else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/exposures/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<ExposureDto> getExposure(@PathVariable("experiment_id") long experimentId, @PathVariable("id") long exposureId, HttpServletRequest req) {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if(securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            Optional<Exposure> exposure = exposureService.findOneByExposureId(exposureId);

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

    @RequestMapping(value = "/{experiment_id}/exposures/", method = RequestMethod.POST)
    public ResponseEntity<ExposureDto> postExposure(@PathVariable("experiment_id") Long experimentId, @RequestBody ExposureDto exposureDto, UriComponentsBuilder ucBuilder, HttpServletRequest req) {

        log.info("Creating Exposure : {}", exposureDto);
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if(securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securityInfo.getPlatformDeploymentId(), securityInfo.getContextId())){
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING , HttpStatus.UNAUTHORIZED);
        }

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            if(exposureDto.getExposureId() != null) {
                log.error("Cannot include id in the POST endpoint. To modify existing exposures you must use PUT");
                return new ResponseEntity("Cannot include id in the POST endpoint. To modify existing exposures you must use PUT", HttpStatus.CONFLICT);
            }

            exposureDto.setExperimentId(experimentId);
            Exposure exposure = null;
            try{
                exposure = exposureService.fromDto(exposureDto);
            } catch (DataServiceException e) {
                return new ResponseEntity("Unable to create exposure:" + e.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Exposure exposureSaved = exposureService.save(exposure);
            ExposureDto returnedDto = exposureService.toDto(exposureSaved);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiment/{experiment_id}/exposures/{id}").buildAndExpand(exposure.getExperiment().getExperimentId(), exposure.getExposureId()).toUri());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/exposures/{id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateExposure(@PathVariable("experiment_id") Long experimentId, @PathVariable("id") Long id, @RequestBody ExposureDto exposureDto, HttpServletRequest req) {

        log.info("Updating exposure with id {}", id);
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if(securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securityInfo.getPlatformDeploymentId(), securityInfo.getContextId())){
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING , HttpStatus.UNAUTHORIZED);
        }

        if(!exposureService.exposureBelongsToExperiment(experimentId,id)) {
            return new ResponseEntity("You do not have permission to change this experiment", HttpStatus.UNAUTHORIZED);
        }

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            Optional<Exposure> exposureSearchResult = exposureService.findById(id);

            if(!exposureSearchResult.isPresent()) {
                log.error("Unable to update. Exposure with id {} not found.", id);
                return new ResponseEntity("Unable to update. Exposure with id  " + id + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            Exposure exposureToChange = exposureSearchResult.get();
            exposureToChange.setTitle(exposureDto.getTitle());

            exposureService.saveAndFlush(exposureToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/exposures/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteExposure(@PathVariable("experiment_id") Long experimentId, @PathVariable("id") Long id, HttpServletRequest req) {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if(securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securityInfo.getPlatformDeploymentId(), securityInfo.getContextId())){
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING , HttpStatus.UNAUTHORIZED);
        }
        if(!exposureService.exposureBelongsToExperiment(experimentId, id)) {
            return new ResponseEntity("You do not have permission to delete this experiment", HttpStatus.UNAUTHORIZED);
        }
        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            try{
                exposureService.deleteById(id);
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
