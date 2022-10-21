package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.WrongValueException;
import edu.iu.terracotta.model.app.dto.ExperimentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExportService;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.ZipUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
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

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = ExperimentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ExperimentController {

    private static final Logger log = LoggerFactory.getLogger(ExperimentController.class);
    public static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    private ExperimentService experimentService;

    @Autowired
    private ExportService exportService;

    @Autowired
    private APIJWTService apijwtService;


    /**
     * To show the experiment in a course (context) in a platform deployment.
     */
    @GetMapping
    public ResponseEntity<List<ExperimentDto>> allExperimentsByCourse(HttpServletRequest req) throws BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);

        if (securedInfo == null){
            throw new BadTokenException(TextConstants.BAD_TOKEN);
        }

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<ExperimentDto> experimentList = experimentService.getExperiments(securedInfo.getPlatformDeploymentId(), securedInfo.getContextId());

        if (experimentList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(experimentList, HttpStatus.OK);
    }

    /**
     * To show the an specific experiment.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExperimentDto> getExperiment(@PathVariable long id,
                                                       @RequestParam(name = "conditions", defaultValue = "false") boolean conditions,
                                                       @RequestParam(name = "exposures", defaultValue = "false") boolean exposures,
                                                       @RequestParam(name = "participants", defaultValue = "false") boolean participants,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, id);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        ExperimentDto experimentDto = experimentService.toDto(experimentService.getExperiment(id), conditions, exposures, participants);

        return new ResponseEntity<>(experimentDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ExperimentDto> postExperiment(@RequestBody ExperimentDto experimentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws BadTokenException, TitleValidationException, IdInPostException, DataServiceException {
        log.debug("Creating Experiment : {}", experimentDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);

        if (securedInfo ==null){
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
            throws ExperimentNotMatchingException, BadTokenException, WrongValueException, TitleValidationException, ParticipantNotUpdatedException {
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
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, id);
        apijwtService.experimentLocked(id,true);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            experimentService.deleteById(id, securedInfo);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{id}/zip", produces = "application/zip")
    public ResponseEntity<ByteArrayResource> downloadZip(@PathVariable long experimentId,
                                                         HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, IOException, CanvasApiException, ParticipantNotUpdatedException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Map<String, List<String[]>> csvFiles = exportService.getCsvFiles(experimentId, securedInfo);
        Map<String, String> jsonFiles = exportService.getJsonFiles(experimentId);
        Map<String, String> readMeFile = exportService.getReadMeFile();

        return new ResponseEntity<>(ZipUtil.generateZipFile(csvFiles, jsonFiles,readMeFile), HttpStatus.OK);
    }

}
