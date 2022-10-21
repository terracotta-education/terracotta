package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParameterMissingException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.dto.media.MediaEventDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = MediaProfileController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class MediaProfileController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/questions/{questionId}/media_event";

    @Autowired
    private MediaService mediaService;

    @Autowired
    private APIJWTService apijwtService;

    @PostMapping
    public ResponseEntity postMediaEvent(@PathVariable long experimentId,
                                         @PathVariable long conditionId,
                                         @PathVariable long treatmentId,
                                         @PathVariable long assessmentId,
                                         @PathVariable long submissionId,
                                         @PathVariable long questionId,
                                         @RequestBody MediaEventDto mediaEventDto,
                                         UriComponentsBuilder ucBuilder,
                                         HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, TreatmentNotMatchingException, ParameterMissingException, SubmissionNotMatchingException,
                NoSubmissionsException, QuestionNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        mediaService.fromDto(mediaEventDto, securedInfo, experimentId, submissionId, questionId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
