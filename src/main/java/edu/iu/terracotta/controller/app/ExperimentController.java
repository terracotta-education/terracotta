package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.WrongValueException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.dto.ExperimentDto;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
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
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<ExperimentDto>> allExperimentsByCourse(HttpServletRequest req) throws BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        if (securedInfo ==null){
            throw new BadTokenException(TextConstants.BAD_TOKEN);
        }

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            List<Experiment> experimentList =
                    experimentService.findAllByDeploymentIdAndCourseId(
                            securedInfo.getPlatformDeploymentId(), securedInfo.getContextId());
            if (experimentList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                // You many decide to return HttpStatus.NOT_FOUND
            }
            List<ExperimentDto> experimentDtoList = new ArrayList<>();
            for (Experiment experiment : experimentList) {
                experimentDtoList.add(experimentService.toDto(experiment, false, false, false));
            }
            return new ResponseEntity<>(experimentDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * To show the an specific experiment.
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<ExperimentDto> getExperiment(@PathVariable("id") long id,
                                                       @RequestParam(name = "conditions", defaultValue = "false") boolean conditions,
                                                       @RequestParam(name = "exposures", defaultValue = "false") boolean exposures,
                                                       @RequestParam(name = "participants", defaultValue = "false") boolean participants,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, id);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            Optional<Experiment> experiment =
                    experimentService.findOneByDeploymentIdAndCourseIdAndExperimentId(
                            securedInfo.getPlatformDeploymentId(), securedInfo.getContextId(), id);

            if (!experiment.isPresent()) {
                log.error(
                        "experiment in platform {} and context {} with id {} not found.",
                        securedInfo.getPlatformDeploymentId(),
                        securedInfo.getContextId(),
                        id);
                return new ResponseEntity(
                        "experiment in platform "
                                + securedInfo.getPlatformDeploymentId()
                                + " and context "
                                + securedInfo.getContextId()
                                + " with id "
                                + id
                                + TextConstants.NOT_FOUND_SUFFIX,
                        HttpStatus.NOT_FOUND);
            } else {
                ExperimentDto experimentDto = experimentService.toDto(experiment.get(), conditions, exposures, participants);
                return new ResponseEntity<>(experimentDto, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<ExperimentDto> postExperiment(@RequestBody ExperimentDto experimentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws BadTokenException {
        log.debug("Creating Experiment : {}", experimentDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        if (securedInfo ==null){
            throw new BadTokenException(TextConstants.BAD_TOKEN);
        }

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            // We check that it does not exist
            if (experimentDto.getExperimentId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }
            ExperimentDto existingEmpty = experimentService.getEmptyExperiment(securedInfo, experimentDto);
            if (existingEmpty!=null){
                experimentService.copyDto(existingEmpty, experimentDto);
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(
                        ucBuilder
                                .path("/api/experiment/{id}")
                                .buildAndExpand(existingEmpty.getExperimentId())
                                .toUri());
                return new ResponseEntity<>(existingEmpty, headers, HttpStatus.ALREADY_REPORTED);
            }
            if(!StringUtils.isBlank(experimentDto.getTitle())){
                if(experimentDto.getTitle().length() > 255){
                    return new ResponseEntity("Experiment title must be 255 characters or less.", HttpStatus.BAD_REQUEST);
                }
                if(experimentService.titleAlreadyExists(experimentDto.getTitle(), securedInfo.getContextId(), 0l)){
                    return new ResponseEntity("Unable to create the experiment. An experiment with title \"" + experimentDto.getTitle() + "\" already exists in this course.", HttpStatus.CONFLICT);
                }
            }

            Experiment experiment;
            experimentDto = experimentService.fillContextInfo(experimentDto, securedInfo);
            try {
                if(!StringUtils.isBlank(experimentDto.getTitle())){
                    if(experimentService.titleAlreadyExists(experimentDto.getTitle(), securedInfo.getContextId(), 0l)){
                        return new ResponseEntity("Unable to create the experiment. An experiment with title \"" + experimentDto.getTitle() + "\" already exists in this course.", HttpStatus.CONFLICT);
                    }
                }
                experiment = experimentService.fromDto(experimentDto);
            } catch (DataServiceException e) {
                return new ResponseEntity(
                        "Unable to create the experiment:" + e.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Experiment experimentSaved = experimentService.save(experiment);
            ExperimentDto returnedDto = experimentService.toDto(experimentSaved, false, false, false);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(
                    ucBuilder
                            .path("/api/experiment/{id}")
                            .buildAndExpand(experiment.getExperimentId())
                            .toUri());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateExperiment(@PathVariable("id") Long id,
                                                 @RequestBody ExperimentDto experimentDto,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, WrongValueException {
        log.info("Updating Experiment with id {}", id);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, id);

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            Optional<Experiment> experimentSearchResult = experimentService.findById(id);

            if (!experimentSearchResult.isPresent()) {
                log.error("Unable to update. Experiment with id {} not found.", id);
                return new ResponseEntity(
                        "Unable to update. Experiment with id " + id + TextConstants.NOT_FOUND_SUFFIX,
                        HttpStatus.NOT_FOUND);
            }
            Experiment experimentToChange = experimentSearchResult.get();
            if(StringUtils.isAllBlank(experimentDto.getTitle()) && StringUtils.isAllBlank(experimentToChange.getTitle())){
                return new ResponseEntity("Please give the experiment a title.", HttpStatus.CONFLICT);
            }
            if(!StringUtils.isBlank(experimentDto.getTitle())){
                if(experimentDto.getTitle().length() > 255){
                    return new ResponseEntity("Experiment title must be 255 characters or less.", HttpStatus.BAD_REQUEST);
                }
                if(experimentService.titleAlreadyExists(experimentDto.getTitle(), securedInfo.getContextId(), id)){
                    return new ResponseEntity("Unable to create the experiment. An experiment with title \"" + experimentDto.getTitle() + "\" already exists in this course.", HttpStatus.CONFLICT);
                }
            }
            experimentToChange.setTitle(experimentDto.getTitle());
            experimentToChange.setDescription(experimentDto.getDescription());
            if (experimentToChange.getStarted()!=null
                    && (!experimentDto.getExposureType().equals(experimentToChange.getExposureType().name()))){
                throw new WrongValueException("The experiment has started. The Exposure Type can't be changed");
            }
            if (experimentToChange.getStarted()!=null
                    && (!experimentDto.getDistributionType().equals(experimentToChange.getDistributionType().name()))){
                throw new WrongValueException("The experiment has started. The Distribution Type can't be changed");
            }
            if (experimentToChange.getStarted()!=null
                    && (!experimentDto.getParticipationType().equals(experimentToChange.getParticipationType().name()))
                    && experimentToChange.getParticipationType().equals(ParticipationTypes.CONSENT)){
                throw new WrongValueException("The experiment has started. The Participation Type can't be changed from 'Consent' to " + experimentDto.getParticipationType());
            }
            if (experimentDto.getExposureType() != null) {
                if (EnumUtils.isValidEnum(ExposureTypes.class, experimentDto.getExposureType())) {
                experimentToChange.setExposureType(
                        EnumUtils.getEnum(ExposureTypes.class, experimentDto.getExposureType()));
                } else {
                    throw new WrongValueException(experimentDto.getExposureType() + " is not a valid Exposure value");
                }
            }
            if (experimentDto.getDistributionType() != null) {
                if (EnumUtils.isValidEnum(DistributionTypes.class, experimentDto.getDistributionType())) {
                    experimentToChange.setDistributionType(
                            EnumUtils.getEnum(DistributionTypes.class, experimentDto.getDistributionType()));
                } else {
                    throw new WrongValueException(experimentDto.getDistributionType() + " is not a valid Distribution value");
                }
            }
            if (experimentDto.getParticipationType() != null) {
                if (EnumUtils.isValidEnum(ParticipationTypes.class, experimentDto.getParticipationType())) {
                experimentToChange.setParticipationType(
                        EnumUtils.getEnum(ParticipationTypes.class, experimentDto.getParticipationType()));
                } else {
                    throw new WrongValueException(experimentDto.getParticipationType() + " is not a valid Participation value");
                }
            }
            experimentToChange.setStarted(experimentDto.getStarted());

            experimentService.saveAndFlush(experimentToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteExperiment(@PathVariable("id") Long id,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, id);
        apijwtService.experimentLocked(id,true);

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            try {
                experimentService.deleteById(id);
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
