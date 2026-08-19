package edu.iu.terracotta.controller.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.NoticeJwtDto;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.NoticeRequestDto;
import edu.iu.terracotta.connectors.generic.service.lti.LtiJwtService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiNoticeService;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import edu.iu.terracotta.utils.LtiStrings;

public class NoticeControllerTest {

    @Mock private LtiJwtService ltiJwtService;
    @Mock private LtiNoticeService ltiNoticeService;
    @Mock private AssignmentAsyncService assignmentAsyncService;

    private NoticeController noticeController;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        noticeController = new NoticeController(ltiJwtService, ltiNoticeService, assignmentAsyncService);
    }

    @SuppressWarnings("unchecked")
    private Jws<Claims> jwsOf(Claims claims) {
        Jws<Claims> jws = mock(Jws.class);
        when(jws.getPayload()).thenReturn(claims);

        return jws;
    }

    private Claims claimsWithNoticeType(String noticeType) {
        return Jwts.claims()
            .issuer("https://platform.example.com")
            .add(LtiStrings.LTI_NOTICE, Map.of(LtiStrings.LTI_NOTICE_TYPE, noticeType))
            .build();
    }

    private NoticeRequestDto requestWith(String... jwts) {
        return NoticeRequestDto.builder()
            .notices(
                List.of(jwts).stream()
                    .map(jwt -> NoticeJwtDto.builder().jwt(jwt).build())
                    .toList()
            )
            .build();
    }

    @Test
    public void testReceiveNoticesEmptyListDoesNothing() {
        assertEquals(200, noticeController.receiveNotices(NoticeRequestDto.builder().build()).getStatusCode().value());

        verify(ltiJwtService, never()).validateJWT(anyString());
    }

    @Test
    public void testReceiveNoticesSkipsBlankJwtEntries() {
        assertEquals(200, noticeController.receiveNotices(requestWith("", "   ")).getStatusCode().value());

        verify(ltiJwtService, never()).validateJWT(anyString());
    }

    @Test
    public void testReceiveNoticesInvalidSignatureIsSkippedAndStillReturnsOk() {
        when(ltiJwtService.validateJWT("bad-jwt")).thenThrow(new RuntimeException("bad signature"));

        assertEquals(200, noticeController.receiveNotices(requestWith("bad-jwt")).getStatusCode().value());

        verify(ltiNoticeService, never()).resolveSecuredInfo(any());
    }

    @Test
    public void testReceiveNoticesIgnoresUnsupportedNoticeType() {
        Claims claims = claimsWithNoticeType("LtiHelloWorldNotice");
        Jws<Claims> jws = jwsOf(claims);
        when(ltiJwtService.validateJWT("jwt-1")).thenReturn(jws);

        noticeController.receiveNotices(requestWith("jwt-1"));

        verify(ltiNoticeService, never()).resolveSecuredInfo(any());
    }

    @Test
    public void testReceiveNoticesCourseCopyWithNoResolvedContextIsSkipped() throws Exception {
        Claims claims = claimsWithNoticeType(LtiStrings.LTI_NOTICE_TYPE_COURSE_COPY);
        Jws<Claims> jws = jwsOf(claims);
        when(ltiJwtService.validateJWT("jwt-1")).thenReturn(jws);
        when(ltiNoticeService.resolveSecuredInfo(claims)).thenReturn(Optional.empty());

        noticeController.receiveNotices(requestWith("jwt-1"));

        verify(assignmentAsyncService, never()).handleAssignmentTasksInLmsByContext(any());
    }

    @Test
    public void testReceiveNoticesCourseCopyTriggersObsoleteAssignmentCheck() throws Exception {
        Claims claims = claimsWithNoticeType(LtiStrings.LTI_NOTICE_TYPE_COURSE_COPY);
        SecuredInfo securedInfo = SecuredInfo.builder().contextId(42L).build();
        Jws<Claims> jws = jwsOf(claims);
        when(ltiJwtService.validateJWT("jwt-1")).thenReturn(jws);
        when(ltiNoticeService.resolveSecuredInfo(claims)).thenReturn(Optional.of(securedInfo));

        assertEquals(200, noticeController.receiveNotices(requestWith("jwt-1")).getStatusCode().value());

        verify(assignmentAsyncService).handleAssignmentTasksInLmsByContext(securedInfo);
    }

    @Test
    public void testReceiveNoticesExceptionFromAsyncCallIsCaughtAndStillReturnsOk() throws Exception {
        Claims claims = claimsWithNoticeType(LtiStrings.LTI_NOTICE_TYPE_COURSE_COPY);
        SecuredInfo securedInfo = SecuredInfo.builder().contextId(42L).build();
        Jws<Claims> jws = jwsOf(claims);
        when(ltiJwtService.validateJWT("jwt-1")).thenReturn(jws);
        when(ltiNoticeService.resolveSecuredInfo(claims)).thenReturn(Optional.of(securedInfo));
        doThrow(new DataServiceException("fail")).when(assignmentAsyncService).handleAssignmentTasksInLmsByContext(securedInfo);

        assertEquals(200, noticeController.receiveNotices(requestWith("jwt-1")).getStatusCode().value());
    }

    @Test
    public void testReceiveNoticesProcessesMultipleNoticesIndependently() throws Exception {
        Claims unsupported = claimsWithNoticeType("LtiHelloWorldNotice");
        Claims courseCopy = claimsWithNoticeType(LtiStrings.LTI_NOTICE_TYPE_COURSE_COPY);
        SecuredInfo securedInfo = SecuredInfo.builder().contextId(42L).build();
        Jws<Claims> jwsUnsupported = jwsOf(unsupported);
        when(ltiJwtService.validateJWT("jwt-unsupported")).thenReturn(jwsUnsupported);
        Jws<Claims> jwsCourseCopy = jwsOf(courseCopy);
        when(ltiJwtService.validateJWT("jwt-course-copy")).thenReturn(jwsCourseCopy);
        when(ltiNoticeService.resolveSecuredInfo(courseCopy)).thenReturn(Optional.of(securedInfo));

        noticeController.receiveNotices(requestWith("jwt-unsupported", "jwt-course-copy"));

        verify(assignmentAsyncService).handleAssignmentTasksInLmsByContext(securedInfo);
        verify(ltiNoticeService, never()).resolveSecuredInfo(unsupported);
    }

}
