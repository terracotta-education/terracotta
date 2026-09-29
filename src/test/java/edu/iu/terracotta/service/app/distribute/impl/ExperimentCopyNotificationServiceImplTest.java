package edu.iu.terracotta.service.app.distribute.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.service.app.notification.NotificationEmailSender;

public class ExperimentCopyNotificationServiceImplTest {

    @Mock private NotificationEmailSender notificationEmailSender;
    @Mock private LtiUserEntity instructor;

    private ExperimentCopyNotificationServiceImpl experimentCopyNotificationService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        when(instructor.getDisplayName()).thenReturn("Pat Instructor");

        experimentCopyNotificationService = new ExperimentCopyNotificationServiceImpl(notificationEmailSender);
    }

    private String sentBody() {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationEmailSender).sendHtml(eq(instructor), eq("Terracotta experiment copy notification"), body.capture(), anyString());

        return body.getValue();
    }

    @Test
    public void testNotifyLmsFailureSendsTheCopyNotificationEmail() {
        experimentCopyNotificationService.notifyLmsFailure(instructor);

        String body = sentBody();
        assertTrue(body.contains("<p>Dear Pat Instructor,</p>"));
        assertTrue(body.contains("There was an error recreating the Terracotta assignments in your newly copied Canvas course. Please login to the new Canvas course, relaunch Terracotta, and reauthorize Terracotta to function in the LMS."));
        assertTrue(body.contains("<a href=\"mailto:info@terracotta.education\">info@terracotta.education</a>"));
        assertTrue(body.trim().endsWith("Thank you,<br>\nTerracotta Support</p>"));
    }

    @Test
    public void testNotifyLmsFailureWithoutDisplayNameUsesGenericGreeting() {
        when(instructor.getDisplayName()).thenReturn(null);

        experimentCopyNotificationService.notifyLmsFailure(instructor);

        assertTrue(sentBody().contains("<p>Dear Instructor,</p>"));
    }

    // the body is HTML, so a display name must not be able to inject markup
    @Test
    public void testNotifyLmsFailureEscapesTheDisplayName() {
        when(instructor.getDisplayName()).thenReturn("<script>x</script>");

        experimentCopyNotificationService.notifyLmsFailure(instructor);

        assertTrue(sentBody().contains("<p>Dear &lt;script&gt;x&lt;/script&gt;,</p>"));
    }

}
