package edu.iu.terracotta.controller.app.preview;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.preview.TreatmentPreview;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.preview.TreatmentPreviewDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.service.app.preview.TreatmentPreviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping(PreviewController.REQUEST_ROOT)
@SuppressWarnings({"PMD.GuardLogStatement"})
public class PreviewController {

    private final ApiJwtService apiJwtService;
    private final ExperimentService experimentService;
    private final TreatmentPreviewService treatmentPreviewService;

    public static final String REQUEST_ROOT = "preview/experiments/{experimentId}";

    @GetMapping("/conditions/{conditionId}/treatments/{treatmentId}")
    public String getTreatmentPreview(@PathVariable("experimentId") UUID experimentUuid, @PathVariable long conditionId, @PathVariable long treatmentId, @RequestParam String ownerId, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, NumberFormatException, TerracottaConnectorException, ExposureNotMatchingException, ConditionNotMatchingException, TreatmentNotMatchingException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        TreatmentPreview treatmentPreview = treatmentPreviewService.create(treatmentId, experimentId, conditionId, ownerId);

        // experimentUuid (not the internal experimentId) - this redirect builds a NEW outbound
        // URL the browser follows directly, so it must never leak the internal numeric id, same
        // as the DTO/controller boundary elsewhere. condition/treatment stay numeric here until
        // their own uuid conversion groups land.
        return String.format("redirect:/app/app.html?treatmentPreview=true&experiment=%s&condition=%s&treatment=%s&previewId=%s&ownerId=%s", experimentUuid, conditionId, treatmentId, treatmentPreview.getUuid(), ownerId);
    }

    @GetMapping("/conditions/{conditionId}/treatments/{treatmentId}/id/{previewId}")
    public ResponseEntity<TreatmentPreviewDto> getTreatmentPreviewId(@PathVariable("experimentId") UUID experimentUuid, @PathVariable long conditionId, @PathVariable long treatmentId, @PathVariable UUID previewId, @RequestParam String ownerId, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, NumberFormatException, TerracottaConnectorException, ExposureNotMatchingException, ConditionNotMatchingException, TreatmentNotMatchingException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        SecuredInfo securedInfo = apiJwtService.extractValues(req,false);

        try {
            return new ResponseEntity<>(treatmentPreviewService.getTreatmentPreview(previewId, treatmentId, experimentId, conditionId, ownerId, securedInfo), HttpStatus.OK);
        } catch (TreatmentNotMatchingException | AssessmentNotMatchingException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/conditions/{conditionId}/treatments/{treatmentId}/complete")
    public String getTreatmentPreviewComplete(@PathVariable("experimentId") UUID experimentUuid, @PathVariable long conditionId, @PathVariable long treatmentId, @RequestParam String ownerId, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, NumberFormatException, TerracottaConnectorException, ExposureNotMatchingException, ConditionNotMatchingException, TreatmentNotMatchingException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        return "redirect:/app/app.html?treatmentPreview=true&complete=true";
    }

}
