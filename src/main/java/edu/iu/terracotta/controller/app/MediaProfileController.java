package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.*;
import edu.iu.terracotta.model.app.dto.media.MediaEventDto;
import edu.iu.terracotta.model.events.Event;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.MediaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = MediaProfileController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class MediaProfileController {

    static final Logger LOGGER = LoggerFactory.getLogger(MediaProfileController.class);
    static final String REQUEST_ROOT = "api/experiments";


    @Autowired
    private MediaService mediaService;

    @Autowired
    private APIJWTService apijwtService;


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/" +
            "assessments/{assessment_id}/submissions/{submission_id}/media_event", method = RequestMethod.POST)
    public ResponseEntity postMediaEvent(@PathVariable("experiment_id") Long experimentId,
                                         @PathVariable("condition_id") Long conditionId,
                                         @PathVariable("treatment_id") Long treatmentId,
                                         @PathVariable("assessment_id") Long assessmentId,
                                         @PathVariable("submission_id") Long submissionId,
                                         @RequestBody MediaEventDto mediaEventDto,
                                         UriComponentsBuilder ucBuilder,
                                         HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, IdInPostException, DataServiceException,
            TreatmentNotMatchingException, ParameterMissingException, SubmissionNotMatchingException, NoSubmissionsException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        Event event = mediaService.fromDto(mediaEventDto, securedInfo, experimentId, submissionId);
        mediaService.save(event);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);

    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/" +
            "assessments/{assessment_id}/submissions/{submission_id}/media_events", method = RequestMethod.GET)
    public ResponseEntity<List<MediaEventDto>> updateMediaEvent(@PathVariable("experiment_id") Long experimentId,
                                                                @PathVariable("condition_id") Long conditionId,
                                                                @PathVariable("treatment_id") Long treatmentId,
                                                                @PathVariable("assessment_id") Long assessmentId,
                                                                @PathVariable("submission_id") Long submissionId,
                                                                UriComponentsBuilder ucBuilder,
                                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException, TitleValidationException,
            TreatmentNotMatchingException, SubmissionNotMatchingException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);
        return new ResponseEntity<>(HttpStatus.OK);

    }


}
