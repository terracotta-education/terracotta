package edu.iu.terracotta.service.app.messaging.impl.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsConversation;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.messaging.attachment.MessageContentAttachment;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.log.MessageLog;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.model.enums.messaging.MessageProcessingStatus;
import edu.iu.terracotta.dao.repository.messaging.log.MessageLogRepository;
import edu.iu.terracotta.exceptions.messaging.MessageSendConversationException;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;
import edu.iu.terracotta.service.app.messaging.MessageSendService;

public class MessageConversationServiceImplTest extends BaseTest {

    @Mock private MessageLogRepository messageLogRepository;
    @Mock private MessageSendService messageSendService;
    @Mock private MessageRuleComparisonService ruleComparisonService;

    private MessageConversationServiceImpl messageConversationService;
    private Message message;
    private LtiUserEntity recipient;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // apiClient/lmsUtils are ApiClient/interface-typed mocks already provided by BaseServiceTest;
        // constructing manually avoids @InjectMocks picking the wrong concrete ApiClient mock
        messageConversationService = new MessageConversationServiceImpl(
            messageLogRepository,
            apiClient,
            lmsUtils,
            messageSendService,
            ruleComparisonService
        );

        recipient = LtiUserEntity.builder()
            .lmsUserId("lms-user-1")
            .build();

        MessageContainer container = MessageContainer.builder().owner(ltiUserEntity).build();
        ExposureGroupCondition exposureGroupCondition = ExposureGroupCondition.builder()
            .condition(condition)
            .exposure(exposure)
            .build();
        MessageConfiguration configuration = MessageConfiguration.builder().subject("subject").build();
        MessageContent content = MessageContent.builder().attachments(Collections.emptyList()).build();

        message = Message.builder()
            .exposureGroupCondition(exposureGroupCondition)
            .configuration(configuration)
            .content(content)
            .container(container)
            .build();

        when(messageSendService.getRecipients(message)).thenReturn(new ArrayList<>(List.of(recipient)));
        when(ruleComparisonService.getLmsSubmissions(message)).thenReturn(Map.of());
        when(messageSendService.parseMessageBody(eq(message), any(LtiUserEntity.class), anyMap(), eq(false), anyMap(), anyMap())).thenReturn("parsed body");
    }

    @Test
    public void testSendSuccessSingleRecipient() throws Exception {
        LmsConversation lmsConversation = LmsConversation.builder().id("conv-1").build();
        when(apiClient.sendConversation(any(), eq(ltiUserEntity))).thenReturn(List.of(lmsConversation));

        messageConversationService.send(message);

        ArgumentCaptor<MessageLog> captor = ArgumentCaptor.forClass(MessageLog.class);
        verify(messageLogRepository, times(1)).save(captor.capture());
        MessageLog savedLog = captor.getValue();
        assertEquals(MessageProcessingStatus.SENT, savedLog.getStatus());
        assertEquals("conv-1", savedLog.getRemoteId());
        assertEquals(recipient, savedLog.getRecipient());
        assertEquals("parsed body", savedLog.getBody());
    }

    @Test
    public void testSendMultipleRecipients() throws Exception {
        LtiUserEntity recipient2 = LtiUserEntity.builder().lmsUserId("lms-user-2").build();
        when(messageSendService.getRecipients(message)).thenReturn(new ArrayList<>(List.of(recipient, recipient2)));
        when(apiClient.sendConversation(any(), eq(ltiUserEntity)))
            .thenReturn(List.of(LmsConversation.builder().id("conv-1").build()))
            .thenReturn(List.of(LmsConversation.builder().id("conv-2").build()));

        messageConversationService.send(message);

        verify(messageLogRepository, times(2)).save(any(MessageLog.class));
        verify(apiClient, times(2)).sendConversation(any(), eq(ltiUserEntity));
    }

    @Test
    public void testSendNoRecipients() throws Exception {
        when(messageSendService.getRecipients(message)).thenReturn(Collections.emptyList());

        messageConversationService.send(message);

        verify(messageLogRepository, never()).save(any(MessageLog.class));
        verify(apiClient, never()).sendConversation(any(), any());
    }

    @Test
    public void testSendRecipientWithNullLmsUserIdSkippedWithErrorLog() throws Exception {
        LtiUserEntity noLmsIdRecipient = mock(LtiUserEntity.class);
        when(noLmsIdRecipient.getLmsUserId()).thenReturn(null);
        when(messageSendService.getRecipients(message)).thenReturn(new ArrayList<>(List.of(noLmsIdRecipient)));

        messageConversationService.send(message);

        ArgumentCaptor<MessageLog> captor = ArgumentCaptor.forClass(MessageLog.class);
        verify(messageLogRepository, times(1)).save(captor.capture());
        assertEquals(MessageProcessingStatus.ERROR, captor.getValue().getStatus());
        assertTrue(captor.getValue().getErrorMessage().contains("no required LMS user ID"));
        verify(apiClient, never()).sendConversation(any(), any());
    }

    @Test
    public void testSendLmsSubmissionsFailureWrapsExceptionAndSkipsLog() throws Exception {
        when(ruleComparisonService.getLmsSubmissions(message)).thenThrow(new RuntimeException("boom"));

        MessageSendConversationException ex = assertThrows(
            MessageSendConversationException.class,
            () -> messageConversationService.send(message)
        );

        assertTrue(ex.getCause().getMessage().contains("Error retrieving LMS submissions"));
        verify(messageLogRepository, never()).save(any(MessageLog.class));
    }

    @Test
    public void testSendApiClientThrowsSkipsMessageLog() throws Exception {
        doThrow(new TerracottaConnectorException("lms down")).when(apiClient).sendConversation(any(), eq(ltiUserEntity));

        assertThrows(
            MessageSendConversationException.class,
            () -> messageConversationService.send(message)
        );

        verify(messageLogRepository, never()).save(any(MessageLog.class));
    }

    @Test
    public void testSendEmptyConversationListCausesErrorLogAndException() throws Exception {
        when(apiClient.sendConversation(any(), eq(ltiUserEntity))).thenReturn(Collections.emptyList());

        assertThrows(
            MessageSendConversationException.class,
            () -> messageConversationService.send(message)
        );

        ArgumentCaptor<MessageLog> captor = ArgumentCaptor.forClass(MessageLog.class);
        verify(messageLogRepository, times(1)).save(captor.capture());
        assertEquals(MessageProcessingStatus.ERROR, captor.getValue().getStatus());
        assertEquals(recipient, captor.getValue().getRecipient());
    }

    @Test
    public void testSendAttachmentIdsPassedToOptions() throws Exception {
        MessageContent contentWithAttachment = MessageContent.builder()
            .attachments(List.of(MessageContentAttachment.builder().lmsId("attach-1").build()))
            .build();
        message.setContent(contentWithAttachment);
        when(apiClient.sendConversation(any(), eq(ltiUserEntity))).thenReturn(List.of(LmsConversation.builder().id("conv-1").build()));

        messageConversationService.send(message);

        verify(apiClient).sendConversation(
            argThat(options -> options.getAttachmentIds().contains("attach-1")),
            eq(ltiUserEntity)
        );
    }

}
