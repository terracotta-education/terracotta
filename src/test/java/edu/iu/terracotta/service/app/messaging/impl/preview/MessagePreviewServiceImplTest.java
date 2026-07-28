package edu.iu.terracotta.service.app.messaging.impl.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextDto;
import edu.iu.terracotta.dao.model.dto.messaging.preview.MessagePreviewDto;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleDto;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleSetDto;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextService;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextService;
import edu.iu.terracotta.service.app.messaging.MessageRecipientRuleService;
import edu.iu.terracotta.service.app.messaging.MessageRecipientRuleSetService;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;
import edu.iu.terracotta.service.app.messaging.MessageSendService;

@SuppressWarnings("unchecked")
public class MessagePreviewServiceImplTest extends BaseTest {

    @Mock private MessageConditionalTextService conditionalTextService;
    @Mock private MessagePipedTextService pipedTextService;
    @Mock private MessageSendService messageSendService;
    @Mock private MessageRecipientRuleService recipientRuleService;
    @Mock private MessageRecipientRuleSetService recipientRuleSetService;
    @Mock private MessageRuleComparisonService ruleComparisonService;

    @InjectMocks private MessagePreviewServiceImpl messagePreviewService;

    private Message message;
    private MessagePreviewDto messagePreviewDto;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        message = Message.builder()
            .content(
                MessageContent.builder().build()
            )
            .build();

        messagePreviewDto = MessagePreviewDto.builder()
            .id(UUID.randomUUID())
            .body("body html")
            .conditionalTexts(List.of())
            .ruleSets(List.of())
            .build();

        when(participantRepository.findByUuid(any(UUID.class))).thenReturn(Optional.of(participant));
        when(participant.getUuid()).thenReturn(messagePreviewDto.getId());
    }

    @Test
    public void testPreviewNoRuleSetsNoConditionalTexts() throws ParticipantNotMatchingException, MessageBodyParseException, IOException, ApiException, TerracottaConnectorException {
        when(messageSendService.parseMessageBody(eq(message), eq(ltiUserEntity), anyMap(), eq(true))).thenReturn("parsed body");

        MessagePreviewDto result = messagePreviewService.preview(messagePreviewDto, message);

        assertEquals(participant.getUuid(), result.getId());
        assertEquals("parsed body", result.getBody());
        assertEquals("body html", message.getContent().getHtml());
        verify(ruleComparisonService, never()).getLmsSubmissions(any(Message.class));
    }

    @Test
    public void testPreviewParticipantNotFound() {
        when(participantRepository.findByUuid(any(UUID.class))).thenReturn(Optional.empty());

        Exception exception = assertThrows(ParticipantNotMatchingException.class, () -> { messagePreviewService.preview(messagePreviewDto, message); });

        assertEquals("Participant not found with UUID: [{}]", exception.getMessage());
    }

    @Test
    public void testPreviewWithConditionalTexts() throws ParticipantNotMatchingException, MessageBodyParseException, IOException, ApiException, TerracottaConnectorException {
        messagePreviewDto.setConditionalTexts(
            List.of(
                MessageConditionalTextDto.builder().build()
            )
        );
        doAnswer(invocation -> {
            MessageContent content = invocation.getArgument(1);
            content.getConditionalTexts().add(MessageConditionalText.builder().content(content).build());

            return null;
        }).when(conditionalTextService).create(any(MessageConditionalTextDto.class), any(MessageContent.class));
        when(ruleComparisonService.getLmsSubmissions(any(Message.class))).thenReturn(Map.of());
        when(messageSendService.parseMessageBody(eq(message), eq(ltiUserEntity), anyMap(), eq(true))).thenReturn("parsed body");

        MessagePreviewDto result = messagePreviewService.preview(messagePreviewDto, message);

        assertEquals("parsed body", result.getBody());
        verify(conditionalTextService, times(1)).create(any(MessageConditionalTextDto.class), eq(message.getContent()));
        verify(ruleComparisonService, times(1)).getLmsSubmissions(message);
    }

    @Test
    public void testPreviewWithRuleSetsFiltersSubmissionsByParticipant() throws ParticipantNotMatchingException, MessageBodyParseException, IOException, ApiException, TerracottaConnectorException {
        MessageRecipientRuleDto ruleDto = MessageRecipientRuleDto.builder().build();
        MessageRecipientRuleSetDto ruleSetDto = MessageRecipientRuleSetDto.builder()
            .rules(List.of(ruleDto))
            .build();
        messagePreviewDto.setRuleSets(List.of(ruleSetDto));

        doAnswer(invocation -> {
            Message m = invocation.getArgument(1);
            m.getRuleSets().add(MessageRecipientRuleSet.builder().message(m).build());

            return null;
        }).when(recipientRuleSetService).create(any(MessageRecipientRuleSetDto.class), any(Message.class));

        when(ltiUserEntity.getLmsUserId()).thenReturn("12345");

        LmsSubmission matchingSubmission = LmsSubmission.builder().userId("12345").build();
        LmsSubmission otherSubmission = LmsSubmission.builder().userId("67890").build();
        when(ruleComparisonService.getLmsSubmissions(any(Message.class))).thenReturn(Map.of("1", List.of(matchingSubmission, otherSubmission)));
        when(messageSendService.parseMessageBody(eq(message), eq(ltiUserEntity), anyMap(), eq(true))).thenReturn("parsed body");

        MessagePreviewDto result = messagePreviewService.preview(messagePreviewDto, message);

        assertEquals("parsed body", result.getBody());
        assertEquals(1, message.getRuleSets().size());
        verify(recipientRuleService, times(1)).create(eq(List.of(ruleDto)), any(MessageRecipientRuleSet.class));

        ArgumentCaptor<Map<String, List<LmsSubmission>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messageSendService).parseMessageBody(eq(message), eq(ltiUserEntity), captor.capture(), eq(true));
        assertEquals(1, captor.getValue().get("1").size());
        assertTrue(captor.getValue().get("1").contains(matchingSubmission));
    }

    @Test
    public void testPreviewLmsSubmissionsFetchExceptionWrapsInMessageBodyParseException() throws IOException, ApiException, TerracottaConnectorException {
        MessageRecipientRuleSetDto ruleSetDto = MessageRecipientRuleSetDto.builder().rules(List.of()).build();
        messagePreviewDto.setRuleSets(List.of(ruleSetDto));

        doAnswer(invocation -> {
            Message m = invocation.getArgument(1);
            m.getRuleSets().add(MessageRecipientRuleSet.builder().message(m).build());

            return null;
        }).when(recipientRuleSetService).create(any(MessageRecipientRuleSetDto.class), any(Message.class));

        when(ruleComparisonService.getLmsSubmissions(any(Message.class))).thenThrow(new IOException("lms down"));

        Exception exception = assertThrows(MessageBodyParseException.class, () -> { messagePreviewService.preview(messagePreviewDto, message); });

        assertTrue(exception.getMessage().contains("Error retrieving LMS submissions for message ID"));
    }

}
