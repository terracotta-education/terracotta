package edu.iu.terracotta.controller.lti;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.Claims;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.NoticeJwtDto;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.NoticeRequestDto;
import edu.iu.terracotta.connectors.generic.service.lti.LtiJwtService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiNoticeService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import edu.iu.terracotta.utils.LtiStrings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Receives LTI Advantage Platform Notification Service (PNS) notices - currently just Canvas's
 * "LtiContextCopyNotice" (course copy) - and reacts by re-running the obsolete-assignment check
 * for the affected course immediately, instead of waiting for the next time someone happens to
 * launch the tool there.
 *
 * Per the PNS spec, this endpoint must be public with no session/authentication of its own - the
 * notice's own signed JWT (verified below against the issuing platform's JWKS) is the only proof
 * of authenticity. See WebSecurityConfig's catch-all permitAll filter chain, which already covers
 * this path.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/notice")
@SuppressWarnings({"PMD.GuardLogStatement"})
public class NoticeController {

    private final LtiJwtService ltiJwtService;
    private final LtiNoticeService ltiNoticeService;
    private final AssignmentAsyncService assignmentAsyncService;

    @PostMapping
    public ResponseEntity<Void> receiveNotices(@RequestBody NoticeRequestDto noticeRequestDto) {
        CollectionUtils.emptyIfNull(noticeRequestDto.getNotices()).stream()
            .map(NoticeJwtDto::getJwt)
            .filter(StringUtils::isNotBlank)
            .forEach(this::processNotice);

        // per the PNS spec, this endpoint must not indicate anything about the outcome of
        // processing any individual notice - Canvas does not retry based on the response beyond
        // basic success/failure, and processing itself is async
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private void processNotice(String jwt) {
        Claims claims;

        try {
            claims = ltiJwtService.validateJWT(jwt).getPayload();
        } catch (Exception e) {
            log.error("Rejected an LTI notice: signature verification failed", e);
            return;
        }

        String noticeType = getNoticeType(claims);

        if (!LtiStrings.LTI_NOTICE_TYPE_COURSE_COPY.equals(noticeType)) {
            log.debug("Ignoring unsupported LTI notice type: [{}]", noticeType);
            return;
        }

        Optional<SecuredInfo> securedInfo = ltiNoticeService.resolveSecuredInfo(claims);

        if (securedInfo.isEmpty()) {
            log.warn("Could not resolve a course/acting-user for an LTI notice from issuer: [{}]", claims.getIssuer());
            return;
        }

        try {
            assignmentAsyncService.handleAssignmentTasksInLmsByContext(securedInfo.get());
        } catch (Exception e) {
            log.error("Error handling LTI notice (context ID: [{}])", securedInfo.get().getContextId(), e);
        }
    }

    private String getNoticeType(Claims claims) {
        Object notice = claims.get(LtiStrings.LTI_NOTICE);

        if (!(notice instanceof Map<?, ?> noticeMap)) {
            return null;
        }

        Object type = noticeMap.get(LtiStrings.LTI_NOTICE_TYPE);

        return type instanceof String ? (String) type : null;
    }

}
