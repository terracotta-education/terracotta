package edu.iu.terracotta.connectors.canvas.service.lti.advantage.impl;

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

    private final AdvantageConnectorHelper advantageConnectorHelper;
    private final ToolDeploymentRepository toolDeploymentRepository;

    @Override
    @Async
    public void ensureNoticeHandlerRegistered(ToolDeployment toolDeployment) {
        if (toolDeployment.isNoticeHandlerRegistered()) {
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
            toolDeploymentRepository.save(toolDeployment);

            log.info("Registered LTI notice handler for deployment ID: [{}] (context_external_tool_id: [{}])", toolDeployment.getDeploymentId(), contextExternalToolId);
        } catch (Exception e) {
            // best-effort: a failed registration just means notices won't arrive for this
            // deployment yet - the launch this ran alongside is unaffected either way, and the
            // next launch will simply retry (noticeHandlerRegistered is only set true on success)
            log.error("Error registering LTI notice handler for deployment ID: [{}]", toolDeployment.getDeploymentId(), e);
        }
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
