package edu.iu.terracotta.connectors.canvas.service.lti.advantage.impl;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.canvas.dao.model.lti.advantage.CanvasNoticeHandlerDto;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.ToolDeploymentRepository;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageConnectorHelper;
import edu.iu.terracotta.connectors.canvas.service.lti.advantage.CanvasAdvantageNoticeService;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class CanvasAdvantageNoticeServiceImpl implements CanvasAdvantageNoticeService {

    // per Canvas's Notice Handlers API docs; not an AGS/NRPS scope, so it doesn't belong in
    // LtiAgsScope
    private static final String NOTICE_HANDLERS_SCOPE = "https://purl.imsglobal.org/spec/lti/scope/noticehandlers";
    private static final String NOTICE_TYPE_COURSE_COPY = "LtiContextCopyNotice";
    // a failure here is usually a per-institution Canvas Developer Key that doesn't have the
    // noticehandlers scope granted yet, which only that institution's admin can fix - back off
    // instead of retrying (and logging) on every single launch until they do
    private static final Duration RETRY_COOLDOWN = Duration.ofHours(24);

    private final AdvantageConnectorHelper advantageConnectorHelper;
    private final ToolDeploymentRepository toolDeploymentRepository;
    private final FeatureService featureService;

    @Override
    @Async
    public void ensureNoticeHandlerRegistered(ToolDeployment toolDeployment) {
        if (!featureService.isFeatureEnabled(FeatureType.PLATFORM_NOTIFICATIONS, toolDeployment.getPlatformDeployment().getKeyId())) {
            return;
        }

        if (toolDeployment.isNoticeHandlerRegistered()) {
            return;
        }

        if (withinCooldown(toolDeployment.getNoticeHandlerRegistrationAttemptedAt())) {
            return;
        }

        Long contextExternalToolId = parseContextExternalToolId(toolDeployment.getLtiDeploymentId());

        if (contextExternalToolId == null) {
            log.warn(
                "Could not parse a context_external_tool_id from deployment_id: [{}] - skipping notice handler registration",
                toolDeployment.getLtiDeploymentId()
            );

            return;
        }

        try {
            LtiToken ltiToken = advantageConnectorHelper.getToken(toolDeployment.getPlatformDeployment(), NOTICE_HANDLERS_SCOPE);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, TextConstants.BEARER + ltiToken.getAccess_token());
            headers.setContentType(MediaType.APPLICATION_JSON);

            CanvasNoticeHandlerDto noticeHandler = CanvasNoticeHandlerDto.builder()
                .notice_type(NOTICE_TYPE_COURSE_COPY)
                .handler(toolDeployment.getPlatformDeployment().getLocalUrl() + "/notice")
                .build();

            String url = String.format(
                "%s/api/lti/notice-handlers/%d",
                toolDeployment.getPlatformDeployment().getBaseUrl(),
                contextExternalToolId
            );

            advantageConnectorHelper.createRestTemplate().exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(noticeHandler, headers),
                CanvasNoticeHandlerDto.class
            );

            toolDeployment.setNoticeHandlerRegistered(true);
            toolDeployment.setNoticeHandlerRegistrationAttemptedAt(Timestamp.from(Instant.now()));
            toolDeploymentRepository.save(toolDeployment);

            log.info("Registered LTI notice handler for deployment ID: [{}] (context_external_tool_id: [{}])", toolDeployment.getDeploymentId(), contextExternalToolId);
        } catch (Exception e) {
            // best-effort: a failed registration just means notices won't arrive for this
            // deployment yet - the launch this ran alongside is unaffected either way. Record the
            // attempt so we back off for RETRY_COOLDOWN instead of retrying (and logging) on
            // every single subsequent launch (noticeHandlerRegistered is only set true on success)
            toolDeployment.setNoticeHandlerRegistrationAttemptedAt(Timestamp.from(Instant.now()));
            toolDeploymentRepository.save(toolDeployment);

            log.warn("Error registering LTI notice handler for deployment ID: [{}]: {}", toolDeployment.getDeploymentId(), e.getMessage());
        }
    }

    private boolean withinCooldown(Timestamp lastAttemptedAt) {
        return lastAttemptedAt != null && lastAttemptedAt.toInstant().isAfter(Instant.now().minus(RETRY_COOLDOWN));
    }

    // Canvas's deployment_id claim is formatted "{context_external_tool_id}:{opaque_hash}" - the
    // numeric prefix is the same id Canvas's own REST API (both /external_tools and
    // /notice-handlers) uses to identify this specific tool installation
    private Long parseContextExternalToolId(String ltiDeploymentId) {
        if (StringUtils.isBlank(ltiDeploymentId)) {
            return null;
        }

        String prefix = StringUtils.substringBefore(ltiDeploymentId, ":");

        if (!StringUtils.isNumeric(prefix)) {
            return null;
        }

        try {
            return Long.valueOf(prefix);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
