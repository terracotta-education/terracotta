package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.model.dto.ExposureDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentStartedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.EmptyResultDataAccessException;
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
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = ExposureController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ExposureController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/exposures";

    private final ExposureService exposureService;
    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;

    @GetMapping
    public ResponseEntity<List<ExposureDto>> allExposuresByExperiment(@PathVariable("experimentId") UUID experimentUuid, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<ExposureDto> exposureList = exposureService.getExposures(experimentId);

        if (exposureList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(exposureList, HttpStatus.OK);
    }

    @GetMapping("/{exposureId}")
    public ResponseEntity<ExposureDto> getExposure(@PathVariable("experimentId") UUID experimentUuid,
                                                   @PathVariable("exposureId") UUID exposureUuid,
                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long exposureId = exposureService.getExposureIdByUuid(exposureUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        ExposureDto exposureDto = exposureService.toDto(exposureService.getExposure(exposureId));

        return new ResponseEntity<>(exposureDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ExposureDto> postExposure(@PathVariable("experimentId") UUID experimentUuid,
                                                    @RequestBody ExposureDto exposureDto,
                                                    UriComponentsBuilder ucBuilder,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, TitleValidationException, IdInPostException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        log.debug("Creating Exposure for experiment ID: {}", experimentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.experimentLocked(experimentId,true);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        ExposureDto returnedDto = exposureService.postExposure(exposureDto, experimentId);
        HttpHeaders headers = exposureService.buildHeaders(ucBuilder, experimentUuid, returnedDto.getExposureId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PostMapping("/create")
    public ResponseEntity<Void> createExposures(@PathVariable("experimentId") UUID experimentUuid, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, DataServiceException, ExperimentStartedException, NumberFormatException, TerracottaConnectorException {

        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.experimentLocked(experimentId,true);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        exposureService.createExposures(experimentId);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{exposureId}")
    public ResponseEntity<Void> updateExposure(@PathVariable("experimentId") UUID experimentUuid,
                                               @PathVariable("exposureId") UUID exposureUuid,
                                               @RequestBody ExposureDto exposureDto,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException, TitleValidationException, NumberFormatException, TerracottaConnectorException {
        long exposureId = exposureService.getExposureIdByUuid(exposureUuid);
        log.debug("Updating exposure with id {}", exposureId);
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        exposureService.updateExposure(exposureId, exposureDto);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{exposureId}")
    public ResponseEntity<Void> deleteExposure(@PathVariable("experimentId") UUID experimentUuid,
                                               @PathVariable("exposureId") UUID exposureUuid,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException, ExperimentLockedException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long exposureId = exposureService.getExposureIdByUuid(exposureUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            exposureService.deleteById(exposureId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
