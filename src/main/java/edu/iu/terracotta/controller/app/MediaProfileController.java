package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.media.MediaEventDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParameterMissingException;
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

import jakarta.servlet.http.HttpServletRequest;

@Controller
@SuppressWarnings({"rawtypes"})
@RequestMapping(value = MediaProfileController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class MediaProfileController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/questions/{questionId}/media_event";

    @Autowired private MediaService mediaService;
    @Autowired private ApiJwtService apijwtService;

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
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, IdInPostException, DataServiceException,
            TreatmentNotMatchingException, ParameterMissingException, SubmissionNotMatchingException, NoSubmissionsException, QuestionNotMatchingException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        mediaService.fromDto(mediaEventDto, securedInfo, experimentId, submissionId, questionId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
