package edu.iu.terracotta.service.app.distribute.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

public class ExperimentCopyNotificationServiceImplTest {

    @Mock private JavaMailSender javaMailSender;
    @Mock private LtiUserEntity instructor;

    private MimeMessage mimeMessage;
    private MockedStatic<SesClient> sesClientStatic;
    private SesClient sesClient;
    private ExperimentCopyNotificationServiceImpl experimentCopyNotificationService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(instructor.getEmail()).thenReturn("instructor@example.edu");
        when(instructor.getDisplayName()).thenReturn("Pat Instructor");

        sesClientStatic = mockStatic(SesClient.class);
        SesClientBuilder sesClientBuilder = mock(SesClientBuilder.class);
        sesClient = mock(SesClient.class);
        sesClientStatic.when(SesClient::builder).thenReturn(sesClientBuilder);
        when(sesClientBuilder.region(any())).thenReturn(sesClientBuilder);
        when(sesClientBuilder.build()).thenReturn(sesClient);
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenReturn(SendRawEmailResponse.builder().messageId("ses-id").build());

        experimentCopyNotificationService = new ExperimentCopyNotificationServiceImpl(javaMailSender);
        ReflectionTestUtils.setField(experimentCopyNotificationService, "from", "no-reply@mail.terracotta.education");
        ReflectionTestUtils.setField(experimentCopyNotificationService, "awsRegion", "us-east-2");
    }

    @AfterEach
    public void afterEach() {
        sesClientStatic.close();
    }

    @Test
    public void testNotifyLmsFailureSendsTheCopyNotificationEmail() throws Exception {
        experimentCopyNotificationService.notifyLmsFailure(instructor);

        ArgumentCaptor<SendRawEmailRequest> captor = ArgumentCaptor.forClass(SendRawEmailRequest.class);
        verify(sesClient).sendRawEmail(captor.capture());
        assertEquals(List.of("instructor@example.edu"), captor.getValue().destinations());
        assertEquals("Terracotta <no-reply@mail.terracotta.education>", captor.getValue().source());

        assertEquals("Terracotta experiment copy notification", mimeMessage.getSubject());
        String body = (String) mimeMessage.getContent();
        assertTrue(body.startsWith("Dear Pat Instructor,"));
        assertTrue(body.contains("There was an error recreating the Terracotta assignments in your newly-copied Canvas course. Please Login to the new Canvas course, re-launch Terracotta, and re-approve permissions."));
        assertTrue(body.trim().endsWith("Thank you,\nTerracotta Support"));
    }

    @Test
    public void testNotifyLmsFailureWithoutDisplayNameUsesGenericGreeting() throws Exception {
        when(instructor.getDisplayName()).thenReturn(null);

        experimentCopyNotificationService.notifyLmsFailure(instructor);

        assertTrue(((String) mimeMessage.getContent()).startsWith("Dear Instructor,"));
    }

    @Test
    public void testNotifyLmsFailureWithoutEmailSendsNothing() {
        when(instructor.getEmail()).thenReturn(" ");

        experimentCopyNotificationService.notifyLmsFailure(instructor);

        verify(sesClient, never()).sendRawEmail(any(SendRawEmailRequest.class));
    }

    @Test
    public void testNotifyLmsFailureNullInstructorSendsNothing() {
        assertDoesNotThrow(() -> experimentCopyNotificationService.notifyLmsFailure(null));

        verify(sesClient, never()).sendRawEmail(any(SendRawEmailRequest.class));
    }

    @Test
    public void testNotifyLmsFailureSendFailureIsLoggedNotThrown() {
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenThrow(new RuntimeException("SES down"));

        assertDoesNotThrow(() -> experimentCopyNotificationService.notifyLmsFailure(instructor));
    }

}
