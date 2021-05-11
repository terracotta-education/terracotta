package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.dto.ExperimentDto;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExperimentService;
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
@RequestMapping(value = ExperimentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ExperimentController {

    static final Logger log = LoggerFactory.getLogger(ExperimentController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    ExperimentService experimentService;

    @Autowired
    APIJWTService apijwtService;


    /**
     * To show the experiment in a course (context) in a platform deployment.
     */
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<ExperimentDto>> allExperimentsByCourse(HttpServletRequest req) {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error("Can't extract the security elements from the token");
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

    List<Experiment> experimentList =
        experimentService.findAllByDeploymentIdAndCourseId(securityInfo.getPlatformDeploymentId(), securityInfo.getContextId());
        if (experimentList.isEmpty()) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
            // You many decide to return HttpStatus.NOT_FOUND
        }
        List<ExperimentDto> experimentDtos = new ArrayList<>();
        for (Experiment experiment:experimentList){
            experimentDtos.add(experimentService.toDto(experiment));
        }
        return new ResponseEntity<>(experimentDtos, HttpStatus.OK);
    }

    /**
     * To show the an specific experiment.
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<ExperimentDto> getExperiment(@PathVariable("id") long id, HttpServletRequest req) {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error("Can't extract the security elements from the token");
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        Optional<Experiment> experiment = experimentService.findOneByDeploymentIdAndCourseIdAndExperimentId(securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), id);

        if (!experiment.isPresent()) {
            log.error("experiment in platform {} and context {} with id {} not found.", securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), id);
            return new ResponseEntity("experiment in platform " + securityInfo.getPlatformDeploymentId() + " and context " + securityInfo.getContextId() + " with id " + id
                    + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
        } else {
            ExperimentDto experimentDto = experimentService.toDto(experiment.get());
            return new ResponseEntity<>(experimentDto, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<ExperimentDto> postExperiment(@RequestBody ExperimentDto experimentDto, UriComponentsBuilder ucBuilder, HttpServletRequest req) {
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error("Can't extract the security elements from the token");
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        log.info("Creating Experiment : {}", experimentDto);
        //We check that it does not exist
        if (experimentDto.getExperimentId()!=null){
            log.error("Unable to create. An experiment with that id already exists");
            return new ResponseEntity("Unable to create. An experiment with same id already exist.", HttpStatus.CONFLICT);
        }

        Experiment experiment = null;
        experimentDto = experimentService.fillContextInfo(experimentDto, securityInfo);
        try {
            experiment = experimentService.fromDto(experimentDto);
        } catch (DataServiceException e) {
            return new ResponseEntity("Unable to create the experiment:" + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        Experiment experimentSaved = experimentService.save(experiment);
        ExperimentDto returnedDto = experimentService.toDto(experimentSaved);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiment/{id}").buildAndExpand(experiment.getExperimentId()).toUri());
        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }


    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateExperiment(@PathVariable("id") Long id, @RequestBody ExperimentDto experimentDto, HttpServletRequest req) {
        log.info("Updating Experiment with id {}", id);
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error("Can't extract the security elements from the token");
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
        Optional<Experiment> experimentSearchResult = experimentService.findById(id);

        if (!experimentSearchResult.isPresent()) {
            log.error("Unable to update. Experiment with id {} not found.", id);
            return new ResponseEntity("Unable to update. Experiment with id " + id + TextConstants.NOT_FOUND_SUFFIX,
                    HttpStatus.NOT_FOUND);
        }
        Experiment experimentToChange = experimentSearchResult.get();
        experimentToChange.setDescription(experimentDto.getDescription());
        experimentToChange.setTitle(experimentDto.getTitle());
        if (experimentDto.getExposureType() != null) {
            experimentToChange.setExposureType(EnumUtils.getEnum(ExposureTypes.class,experimentDto.getExposureType()));
        }
        if (experimentDto.getDistributionType() != null) {
            experimentToChange.setDistributionType(EnumUtils.getEnum(DistributionTypes.class,experimentDto.getDistributionType()));
        }
        if (experimentDto.getParticipationType() != null) {
            experimentToChange.setParticipationType(EnumUtils.getEnum(ParticipationTypes.class,experimentDto.getParticipationType()));
        }
        experimentToChange.setStarted(experimentDto.getStarted());
        //TODO, check if we will modify "sub elements, as conditions... I don't think we need to do it and we should do
        //it on its own controller.
        experimentService.saveAndFlush(experimentToChange);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteExperiment(@PathVariable("id") Long id, @RequestBody ExperimentDto experimentDto) {
        try {
            experimentService.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.error(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

    }



}
