package edu.iu.terracotta.service.app.messaging.impl.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.messaging.attachment.MessageContentAttachment;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.log.MessageLog;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;
import edu.iu.terracotta.dao.model.dto.messaging.send.MessageSendTestDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageProcessingStatus;
import edu.iu.terracotta.dao.repository.messaging.log.MessageLogRepository;
import edu.iu.terracotta.exceptions.messaging.MessageSendEmailException;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;
import edu.iu.terracotta.service.app.messaging.MessageSendService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

public class MessageEmailServiceImplTest extends BaseTest {

    @Mock private MessageLogRepository messageLogRepository;
    @Mock private MessageSendService messageSendService;
    @Mock private MessageRuleComparisonService ruleComparisonService;
    @Mock private JavaMailSender javaMailSender;

    @InjectMocks private MessageEmailServiceImpl messageEmailService;

    private Message message;
    private MimeMessage mimeMessage;
    private MockedStatic<SesClient> sesClientStatic;
    private SesClient sesClient;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        ReflectionTestUtils.setField(messageEmailService, "from", "no-reply@mail.terracotta.education");
        ReflectionTestUtils.setField(messageEmailService, "batchSize", 14);
        ReflectionTestUtils.setField(messageEmailService, "interval", 0);
        ReflectionTestUtils.setField(messageEmailService, "testSubject", "Test message from Terracotta");
        ReflectionTestUtils.setField(messageEmailService, "testBody", "Congratulations! You have successfully sent a test message.");
        ReflectionTestUtils.setField(messageEmailService, "awsRegion", "us-east-2");

        mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        when(ltiContextEntity.getTitle()).thenReturn("Course Title");
        when(ltiUserEntity.getLmsUserId()).thenReturn("lms-user-1");

        MessageContainer container = MessageContainer.builder().owner(ltiUserEntity).build();
        ExposureGroupCondition exposureGroupCondition = ExposureGroupCondition.builder()
            .condition(condition)
            .exposure(exposure)
            .build();
        MessageConfiguration configuration = MessageConfiguration.builder()
            .subject("Email Subject")
            .replyTo(new ArrayList<>())
            .build();
        MessageContent content = MessageContent.builder().attachments(Collections.emptyList()).build();

        message = Message.builder()
            .exposureGroupCondition(exposureGroupCondition)
            .configuration(configuration)
            .content(content)
            .container(container)
            .build();

        when(messageSendService.getRecipients(message)).thenReturn(new ArrayList<>(List.of(ltiUserEntity)));
        when(ruleComparisonService.getLmsSubmissions(message)).thenReturn(Map.of());
        when(messageSendService.parseMessageBody(eq(message), any(LtiUserEntity.class), anyMap(), eq(false), anyMap(), anyMap())).thenReturn("parsed body");

        sesClientStatic = mockStatic(SesClient.class);
        SesClientBuilder sesClientBuilder = mock(SesClientBuilder.class);
        sesClient = mock(SesClient.class);
        sesClientStatic.when(SesClient::builder).thenReturn(sesClientBuilder);
        when(sesClientBuilder.region(any())).thenReturn(sesClientBuilder);
        when(sesClientBuilder.build()).thenReturn(sesClient);
    }

    @AfterEach
    public void afterEach() {
        sesClientStatic.close();
    }

    @Test
    public void testSendSuccessSingleRecipient() throws Exception {
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenReturn(SendRawEmailResponse.builder().messageId("ses-id-1").build());

        messageEmailService.send(message);

        ArgumentCaptor<MessageLog> captor = ArgumentCaptor.forClass(MessageLog.class);
        verify(messageLogRepository, times(1)).save(captor.capture());
        MessageLog savedLog = captor.getValue();
        assertEquals(MessageProcessingStatus.SENT, savedLog.getStatus());
        assertEquals("ses-id-1", savedLog.getRemoteId());
        assertEquals(ltiUserEntity, savedLog.getRecipient());
        assertEquals("parsed body", savedLog.getBody());
    }

    @Test
    public void testSendMultipleRecipientsBatched() throws Exception {
        ReflectionTestUtils.setField(messageEmailService, "batchSize", 1);
        LtiUserEntity recipient2 = mock(LtiUserEntity.class);
        when(recipient2.getLmsUserId()).thenReturn("lms-user-2");
        when(recipient2.getEmail()).thenReturn("second@terracotta.edu");
        when(messageSendService.getRecipients(message)).thenReturn(new ArrayList<>(List.of(ltiUserEntity, recipient2)));
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenReturn(SendRawEmailResponse.builder().messageId("ses-id-1").build());

        messageEmailService.send(message);

        verify(messageLogRepository, times(2)).save(any(MessageLog.class));
        verify(sesClient, times(2)).sendRawEmail(any(SendRawEmailRequest.class));
    }

    @Test
    public void testSendNoRecipients() throws Exception {
        when(messageSendService.getRecipients(message)).thenReturn(Collections.emptyList());

        messageEmailService.send(message);

        verify(messageLogRepository, never()).save(any(MessageLog.class));
        verify(sesClient, never()).sendRawEmail(any(SendRawEmailRequest.class));
    }

    @Test
    public void testSendLmsSubmissionsFailureWrapsExceptionAndLogsError() throws Exception {
        when(ruleComparisonService.getLmsSubmissions(message)).thenThrow(new RuntimeException("boom"));

        MessageSendEmailException ex = assertThrows(
            MessageSendEmailException.class,
            () -> messageEmailService.send(message)
        );

        assertTrue(ex.getCause().getMessage().contains("Error retrieving LMS submissions"));

        ArgumentCaptor<MessageLog> captor = ArgumentCaptor.forClass(MessageLog.class);
        verify(messageLogRepository, times(1)).save(captor.capture());
        assertEquals(MessageProcessingStatus.ERROR, captor.getValue().getStatus());
    }

    @Test
    public void testSendInvalidReplyToAddressIsSkippedButSendSucceeds() throws Exception {
        message.getConfiguration().getReplyTo().add(MessageEmailReplyTo.builder().email("valid@terracotta.edu").build());
        message.getConfiguration().getReplyTo().add(MessageEmailReplyTo.builder().email("foo<bar").build());
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenReturn(SendRawEmailResponse.builder().messageId("ses-id-2").build());

        messageEmailService.send(message);

        assertEquals(1, mimeMessage.getReplyTo().length);
        verify(messageLogRepository, times(1)).save(any(MessageLog.class));
    }

    @Test
    public void testSendAttachmentRetrievalFailureIsSkippedButSendSucceeds() throws Exception {
        MessageContent contentWithBadAttachment = MessageContent.builder()
            .attachments(List.of(MessageContentAttachment.builder().filename("bad.txt").url("file:///nonexistent-terracotta-file.txt").build()))
            .build();
        message.setContent(contentWithBadAttachment);
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenReturn(SendRawEmailResponse.builder().messageId("ses-id-3").build());

        messageEmailService.send(message);

        ArgumentCaptor<MessageLog> captor = ArgumentCaptor.forClass(MessageLog.class);
        verify(messageLogRepository, times(1)).save(captor.capture());
        assertEquals(MessageProcessingStatus.SENT, captor.getValue().getStatus());
    }

    @Test
    public void testSendSesFailureLogsErrorAndThrows() throws Exception {
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenThrow(SdkClientException.create("ses unreachable"));

        MessageSendEmailException ex = assertThrows(
            MessageSendEmailException.class,
            () -> messageEmailService.send(message)
        );

        assertTrue(ex.getMessage().contains("Error sending email"));
        ArgumentCaptor<MessageLog> captor = ArgumentCaptor.forClass(MessageLog.class);
        verify(messageLogRepository, times(1)).save(captor.capture());
        assertEquals(MessageProcessingStatus.ERROR, captor.getValue().getStatus());
    }

    @Test
    public void testSendTestSuccessDoesNotLog() throws Exception {
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenReturn(SendRawEmailResponse.builder().messageId("ses-test-id").build());
        MessageSendTestDto dto = MessageSendTestDto.builder().to("test@terracotta.edu").message("test body").subject("ignored").build();

        messageEmailService.sendTest(message, dto);

        assertEquals("Test message from Terracotta", mimeMessage.getSubject());
        verify(messageLogRepository, never()).save(any(MessageLog.class));
    }

    @Test
    public void testSendTestSesFailureLogsErrorAndThrows() throws Exception {
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenThrow(SdkClientException.create("ses unreachable"));
        MessageSendTestDto dto = MessageSendTestDto.builder().to("test@terracotta.edu").message("test body").subject("ignored").build();

        MessageSendEmailException ex = assertThrows(
            MessageSendEmailException.class,
            () -> messageEmailService.sendTest(message, dto)
        );

        assertTrue(ex.getMessage().contains("Error sending test email"));
        ArgumentCaptor<MessageLog> captor = ArgumentCaptor.forClass(MessageLog.class);
        verify(messageLogRepository, times(1)).save(captor.capture());
        assertEquals(MessageProcessingStatus.ERROR, captor.getValue().getStatus());
        assertEquals("test body", captor.getValue().getBody());
    }

}
