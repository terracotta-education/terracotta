package edu.iu.terracotta.service.app.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

public class NotificationEmailSenderTest {

    @Mock private JavaMailSender javaMailSender;
    @Mock private LtiUserEntity recipient;

    private MimeMessage mimeMessage;
    private MockedStatic<SesClient> sesClientStatic;
    private SesClient sesClient;
    private NotificationEmailSender notificationEmailSender;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(recipient.getEmail()).thenReturn("instructor@example.edu");

        sesClientStatic = mockStatic(SesClient.class);
        SesClientBuilder sesClientBuilder = mock(SesClientBuilder.class);
        sesClient = mock(SesClient.class);
        sesClientStatic.when(SesClient::builder).thenReturn(sesClientBuilder);
        when(sesClientBuilder.region(any())).thenReturn(sesClientBuilder);
        when(sesClientBuilder.build()).thenReturn(sesClient);
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenReturn(SendRawEmailResponse.builder().messageId("ses-id").build());

        notificationEmailSender = new NotificationEmailSender(javaMailSender);
        ReflectionTestUtils.setField(notificationEmailSender, "from", "no-reply@mail.terracotta.education");
        ReflectionTestUtils.setField(notificationEmailSender, "awsRegion", "us-east-2");
    }

    @AfterEach
    public void afterEach() {
        sesClientStatic.close();
    }

    @Test
    public void testSendHtmlSendsAnHtmlEmailThroughSes() throws Exception {
        assertTrue(notificationEmailSender.sendHtml(recipient, "Subject line", "<p>Hello</p>", "test"));

        ArgumentCaptor<SendRawEmailRequest> captor = ArgumentCaptor.forClass(SendRawEmailRequest.class);
        verify(sesClient).sendRawEmail(captor.capture());
        assertEquals(List.of("instructor@example.edu"), captor.getValue().destinations());
        assertEquals("Terracotta <no-reply@mail.terracotta.education>", captor.getValue().source());
        assertEquals("Subject line", mimeMessage.getSubject());
        assertTrue(mimeMessage.getContentType().startsWith("text/html"));
        assertEquals("<p>Hello</p>", mimeMessage.getContent());
    }

    @Test
    public void testSendHtmlWithoutEmailSendsNothing() {
        when(recipient.getEmail()).thenReturn(" ");

        assertFalse(notificationEmailSender.sendHtml(recipient, "Subject", "<p>x</p>", "test"));
        verify(sesClient, never()).sendRawEmail(any(SendRawEmailRequest.class));
    }

    @Test
    public void testSendHtmlNullRecipientSendsNothing() {
        assertFalse(notificationEmailSender.sendHtml(null, "Subject", "<p>x</p>", "test"));
        verify(sesClient, never()).sendRawEmail(any(SendRawEmailRequest.class));
    }

    // a notice must never break the work it reports on
    @Test
    public void testSendHtmlFailureIsLoggedNotThrown() {
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenThrow(new RuntimeException("SES down"));

        assertFalse(notificationEmailSender.sendHtml(recipient, "Subject", "<p>x</p>", "test"));
    }

    @Test
    public void testGreetingNameIsEscapedAndDefaulted() {
        when(recipient.getDisplayName()).thenReturn("Pat <b>Instructor</b>");

        assertEquals("Pat &lt;b&gt;Instructor&lt;/b&gt;", NotificationEmailSender.greetingName(recipient));
        assertEquals("Instructor", NotificationEmailSender.greetingName(null));
    }

}
