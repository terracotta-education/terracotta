package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
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
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.MediaService;
import edu.iu.terracotta.service.app.TreatmentService;

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
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes"})
@RequestMapping(value = MediaProfileController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class MediaProfileController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/questions/{questionId}/media_event";

    private final MediaService mediaService;
    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;
    private final ConditionService conditionService;
    private final TreatmentService treatmentService;

    @PostMapping
    public ResponseEntity postMediaEvent(@PathVariable("experimentId") UUID experimentUuid,
                                         @PathVariable("conditionId") UUID conditionUuid,
                                         @PathVariable("treatmentId") UUID treatmentUuid,
                                         @PathVariable long assessmentId,
                                         @PathVariable long submissionId,
                                         @PathVariable long questionId,
                                         @RequestBody MediaEventDto mediaEventDto,
                                         UriComponentsBuilder ucBuilder,
                                         HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, ExperimentLockedException, IdInPostException, DataServiceException,
            TreatmentNotMatchingException, ParameterMissingException, SubmissionNotMatchingException, NoSubmissionsException, QuestionNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        mediaService.fromDto(mediaEventDto, securedInfo, experimentId, submissionId, questionId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
