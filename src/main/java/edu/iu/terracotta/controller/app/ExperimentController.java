package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.ExperimentDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentStartedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.WrongValueException;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = ExperimentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ExperimentController {

    public static final String REQUEST_ROOT = "api/experiments";

    @Autowired private ExperimentService experimentService;
    @Autowired private ApiJwtService apijwtService;

    /**
     * To show the experiment in a course (context) in a platform deployment.
     * @throws TerracottaConnectorException
     * @throws NumberFormatException
     */
    @GetMapping
    public ResponseEntity<List<ExperimentDto>> allExperimentsByCourse(HttpServletRequest req) throws BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);

        if (securedInfo == null) {
            throw new BadTokenException(TextConstants.BAD_TOKEN);
        }

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<ExperimentDto> experimentList = experimentService.getExperiments(securedInfo, true);

        if (experimentList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(experimentList, HttpStatus.OK);
    }

    /**
     * To show the an specific experiment.
          * @throws TerracottaConnectorException
          * @throws NumberFormatException
          */
         @GetMapping("/{id}")
         public ResponseEntity<ExperimentDto> getExperiment(@PathVariable long id,
                                                            @RequestParam(name = "conditions", defaultValue = "false") boolean conditions,
                                                            @RequestParam(name = "exposures", defaultValue = "false") boolean exposures,
                                                            @RequestParam(name = "participants", defaultValue = "false") boolean participants,
                                                            HttpServletRequest req)
                 throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, id);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        ExperimentDto experimentDto = experimentService.toDto(experimentService.getExperiment(id), conditions, exposures, participants, securedInfo);

        return new ResponseEntity<>(experimentDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ExperimentDto> postExperiment(@RequestBody ExperimentDto experimentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws BadTokenException, TitleValidationException, IdInPostException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        log.debug("Creating Experiment with title : {}", experimentDto.getTitle());
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);

        if (securedInfo ==null) {
            throw new BadTokenException(TextConstants.BAD_TOKEN);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (experimentDto.getExperimentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        //if an empty experiment has been created but not used, use it.
        ExperimentDto existingEmpty = experimentService.getEmptyExperiment(securedInfo, experimentDto);

        if (existingEmpty != null) {
            experimentService.copyDto(existingEmpty, experimentDto);
            HttpHeaders headers = experimentService.buildHeaders(ucBuilder, existingEmpty.getExperimentId());

            return new ResponseEntity<>(existingEmpty, headers, HttpStatus.ALREADY_REPORTED);
        }

        ExperimentDto returnedDto = experimentService.postExperiment(experimentDto, securedInfo);
        HttpHeaders headers = experimentService.buildHeaders(ucBuilder, returnedDto.getExperimentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateExperiment(@PathVariable long id,
                                                 @RequestBody ExperimentDto experimentDto,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, WrongValueException, TitleValidationException, ParticipantNotUpdatedException,
                    DataServiceException, ExperimentStartedException, IOException, NumberFormatException, TerracottaConnectorException {
        log.debug("Updating Experiment with id {}", id);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, id);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        experimentService.updateExperiment(id, securedInfo.getContextId(), experimentDto, securedInfo);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperiment(@PathVariable long id,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, IOException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, id);
        apijwtService.experimentLocked(id,true);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            experimentService.deleteById(id, securedInfo);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
