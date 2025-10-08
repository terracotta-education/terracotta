package edu.iu.terracotta.controller.app.integrations;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.iu.terracotta.dao.entity.integrations.IntegrationError;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.integrations.IntegrationScoreService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/integrations")
@SuppressWarnings({"PMD.GuardLogStatement"})
public class IntegrationsController {

    @Autowired private IntegrationScoreService integrationScoreService;

    @GetMapping
    public String score(@RequestParam(name = "launch_token", required = false) String launchToken, @RequestParam(required = false) String score, HttpServletRequest req) throws IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        HttpStatus status = HttpStatus.OK;
        String errorCode = null;
        Optional<String> previewTokenClient = integrationScoreService.getPreviewTokenClient(launchToken);
        String url = null;
        IntegrationError integrationError = null;

        try {
            url = String.format(
                "%s?%s",
                URLEncoder.encode(req.getRequestURL().toString(), StandardCharsets.UTF_8.toString()),
                StringUtils.isNotBlank(req.getQueryString()) ? URLEncoder.encode(req.getQueryString(), StandardCharsets.UTF_8.toString()) : ""
            );

            integrationScoreService.score(launchToken, score, previewTokenClient);
        } catch (IntegrationTokenNotFoundException e) {
            integrationError = IntegrationError.from(e.getMessage());
            errorCode = integrationError.getCode();
            status = HttpStatus.NOT_FOUND;
        } catch (IntegrationTokenAlreadyRedeemedException | IntegrationTokenExpiredException | IntegrationTokenInvalidException | DataServiceException | RuntimeException | UnsupportedEncodingException e) {
            integrationError = IntegrationError.from(e.getMessage());
            errorCode = integrationError.getCode();
            status = HttpStatus.BAD_REQUEST;
        }

        if (StringUtils.isNotBlank(errorCode)) {
            log.error(
                "launch_token: [{}], score: [{}], error code: [{}], status: [{}], moreAttemptsAvailable: [{}]",
                launchToken,
                score,
                errorCode,
                status,
                integrationError.isMoreAttemptsAvailable()
            );
        }

        return String.format(
            "redirect:/app/app.html?integration=true&status=%s&preview=%s&client=%s&launch_token=%s&score=%s&url=%s&errorCode=%s&moreAttemptsAvailable=%s",
            status.name(),
            previewTokenClient.isPresent(),
            previewTokenClient.isPresent() ? previewTokenClient.get() : null,
            launchToken,
            score,
            url,
            errorCode,
            integrationError != null && integrationError.isMoreAttemptsAvailable() || false
        );
    }

    @GetMapping("/preview")
    public String preview(@RequestParam(required = false) String url) {
        HttpStatus status = HttpStatus.OK;
        String errorCode = null;

        try {
            if (StringUtils.isBlank(url)) {
                throw new IllegalArgumentException("Preview URL cannot be null");
            }

            log.info("Launching instructor preview with URL: [{}]", new String(Base64.getDecoder().decode(url)));
        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST;
            log.error("url: [{}] errorCode: [{}], status: [{}]", url, errorCode, status, e);
        }

        return String.format("redirect:/app/app.html?integration=true&preview=true&previewUrl=%s&status=%s", url != null ? url : "", status.name());
    }

}
