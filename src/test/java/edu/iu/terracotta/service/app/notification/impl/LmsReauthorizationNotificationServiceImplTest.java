package edu.iu.terracotta.service.app.notification.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.service.app.notification.NotificationEmailSender;

public class LmsReauthorizationNotificationServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-29T15:00:00Z");

    @Mock private NotificationEmailSender notificationEmailSender;
    @Mock private LtiUserEntity instructor;
    @Mock private PlatformDeployment platformDeployment;

    private Clock clock;
    private LmsReauthorizationNotificationServiceImpl service;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        when(instructor.getUserId()).thenReturn(7L);
        when(instructor.getDisplayName()).thenReturn("Pat Instructor");
        when(instructor.getPlatformDeployment()).thenReturn(platformDeployment);
        when(platformDeployment.getLmsConnector()).thenReturn(LmsConnector.CANVAS);
        when(notificationEmailSender.sendHtml(any(), anyString(), anyString(), anyString())).thenReturn(true);

        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        service = new LmsReauthorizationNotificationServiceImpl(notificationEmailSender, clock);
    }

    @Test
    public void testNotifyReauthorizationNeededSaysWhatFailedAndHowToFixIt() {
        service.notifyReauthorizationNeeded(instructor, "your scheduled message could not be sent");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationEmailSender).sendHtml(eq(instructor), eq("Terracotta needs you to reconnect your Canvas account"), body.capture(), anyString());
        assertTrue(body.getValue().contains("<p>Dear Pat Instructor,</p>"));
        assertTrue(body.getValue().contains("Terracotta can no longer reach your Canvas account, so your scheduled message could not be sent. Please log in to Canvas, relaunch Terracotta in your course, and reauthorize Terracotta to function in the LMS."));
        assertTrue(body.getValue().contains("<a href=\"mailto:info@terracotta.education\">info@terracotta.education</a>"));
    }

    // failedWork can carry a message's subject, which the instructor typed
    @Test
    public void testNotifyReauthorizationNeededEscapesTheFailedWork() {
        service.notifyReauthorizationNeeded(instructor, "your message \"<b>Week 3</b>\" could not be sent");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationEmailSender).sendHtml(any(), anyString(), body.capture(), anyString());
        assertTrue(body.getValue().contains("&quot;&lt;b&gt;Week 3&lt;/b&gt;&quot;"));
    }

    @Test
    public void testNotifyReauthorizationNeededNamesAGenericLmsWhenItIsNotCanvasOrBrightspace() {
        when(platformDeployment.getLmsConnector()).thenReturn(LmsConnector.GENERIC);

        service.notifyReauthorizationNeeded(instructor, "something could not be done");

        verify(notificationEmailSender).sendHtml(any(), eq("Terracotta needs you to reconnect your LMS account"), anyString(), anyString());
    }

    // a dead token fails every scheduled message and student check until the instructor relaunches
    @Test
    public void testNotifyReauthorizationNeededSendsAtMostOneEmailADay() {
        service.notifyReauthorizationNeeded(instructor, "a");
        when(clock.instant()).thenReturn(NOW.plus(Duration.ofHours(23)));
        service.notifyReauthorizationNeeded(instructor, "b");

        verify(notificationEmailSender, times(1)).sendHtml(any(), anyString(), anyString(), anyString());

        when(clock.instant()).thenReturn(NOW.plus(Duration.ofHours(25)));
        service.notifyReauthorizationNeeded(instructor, "c");

        verify(notificationEmailSender, times(2)).sendHtml(any(), anyString(), anyString(), anyString());
    }

    // a send that failed (e.g. no email address) doesn't start the quiet period
    @Test
    public void testNotifyReauthorizationNeededRetriesWhenTheLastSendFailed() {
        when(notificationEmailSender.sendHtml(any(), anyString(), anyString(), anyString())).thenReturn(false, true);

        service.notifyReauthorizationNeeded(instructor, "a");
        service.notifyReauthorizationNeeded(instructor, "b");

        verify(notificationEmailSender, times(2)).sendHtml(any(), anyString(), anyString(), anyString());
    }

    @Test
    public void testNotifyReauthorizationNeededIgnoresAMissingInstructor() {
        service.notifyReauthorizationNeeded(null, "a");

        verify(notificationEmailSender, never()).sendHtml(any(), anyString(), anyString(), anyString());
    }

}
