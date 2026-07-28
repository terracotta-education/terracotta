package edu.iu.terracotta.service.app.messaging.impl.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchEmailProjection;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextResult;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItemValue;
import edu.iu.terracotta.dao.model.enums.messaging.MessageRecipientMatchType;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.dao.repository.messaging.conditional.MessageConditionalTextRepository;
import edu.iu.terracotta.dao.repository.messaging.piped.PipedTextItemRepository;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;

@SuppressWarnings("unchecked")
public class MessageSendServiceImplTest extends BaseTest {

    @Mock private LmsUserBatchRepository lmsUserBatchRepository;
    @Mock private MessageConditionalTextRepository conditionalTextRepository;
    @Mock private PipedTextItemRepository pipedTextItemRepository;
    @Mock private MessageRuleComparisonService ruleComparisonService;

    private MessageSendServiceImpl messageSendService;

    private PlatformDeployment platformDeployment;
    private LtiUserEntity owner;
    private LtiUserEntity recipient;
    private Participant participant;
    private Group group;
    private Condition condition;
    private ExposureGroupCondition egc;
    private MessageContainer container;
    private Message message;
    private LmsUser student;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        messageSendService = new MessageSendServiceImpl(
            lmsUserBatchRepository,
            ltiUserRepository,
            conditionalTextRepository,
            participantRepository,
            pipedTextItemRepository,
            ruleComparisonService,
            apiClient,
            lmsUtils
        );
        org.springframework.test.util.ReflectionTestUtils.setField(messageSendService, "batchSize", 500);
        org.springframework.test.util.ReflectionTestUtils.setField(messageSendService, "entityManager", entityManager);

        platformDeployment = PlatformDeployment.builder().keyId(100L).build();
        LtiContextEntity ltiContextEntity = LtiContextEntity.builder().context_memberships_url("https://lms/membership").build();
        Experiment experiment = Experiment.builder().experimentId(5L).platformDeployment(platformDeployment).ltiContextEntity(ltiContextEntity).build();
        condition = Condition.builder().conditionId(10L).defaultCondition(false).experiment(experiment).build();
        group = Group.builder().groupId(20L).build();
        egc = ExposureGroupCondition.builder().exposureGroupConditionId(30L).condition(condition).group(group).build();

        owner = LtiUserEntity.builder().userId(1L).lmsUserId("owner-lms").build();
        container = MessageContainer.builder().owner(owner).messages(new ArrayList<>()).build();

        MessageConfiguration configuration = MessageConfiguration.builder()
            .recipientMatchType(MessageRecipientMatchType.INCLUDE)
            .toConsentedOnly(false)
            .type(MessageType.EMAIL)
            .replyTo(new ArrayList<>())
            .build();

        message = Message.builder()
            .exposureGroupCondition(egc)
            .configuration(configuration)
            .container(container)
            .content(MessageContent.builder().build())
            .build();
        message.getContent().setUuid(UUID.randomUUID());

        container.getMessages().add(message);

        recipient = LtiUserEntity.builder().userId(2L).lmsUserId("s1").email("student@example.com").build();
        participant = Participant.builder().ltiUserEntity(recipient).consent(true).build();
        student = LmsUser.builder().id("s1").email("student@example.com").build();

        LmsUserBatchEmailProjection batchEmailProjection = mock(LmsUserBatchEmailProjection.class);
        when(batchEmailProjection.getEmail()).thenReturn("student@example.com");
        when(batchEmailProjection.getLmsUserId()).thenReturn("s1");

        // getRecipients loops until this returns empty; without the second thenReturn, Mockito would
        // keep repeating the first (non-empty) value forever, so this would spin infinitely.
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(List.of(participant))
            .thenReturn(List.of());
        when(apiClient.listUsersForCourse(any(), any(LtiUserEntity.class))).thenReturn(List.of(student));
        when(lmsUserBatchRepository.findBatchProjectionsByBatchIdAndEmailIn(any(UUID.class), any(), any())).thenReturn(List.of(batchEmailProjection));
        when(ltiUserRepository.findAllByEmailInAndPlatformDeployment_KeyId(any(), anyLong())).thenReturn(List.of(recipient));
        when(ruleComparisonService.getLmsSubmissions(message)).thenReturn(
            Map.of("q1", List.of(LmsSubmission.builder().userId("s1").build()))
        );
        when(ruleComparisonService.recipientRuleSetsMatch(any(List.class), anyMap())).thenReturn(true);
    }

    @Test
    public void testGetRecipientsNoParticipantsReturnsEmpty() throws Exception {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any(Pageable.class))).thenReturn(List.of());

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        // the LMS roster sync kicks off unconditionally before the participants check, so it still runs
        assertTrue(recipients.isEmpty());
        verify(apiClient).listUsersForCourse(any(), any(LtiUserEntity.class));
    }

    @Test
    public void testGetRecipientsNoStudentsReturnsEmpty() throws Exception {
        when(lmsUserBatchRepository.findBatchProjectionsByBatchIdAndEmailIn(any(UUID.class), any(), any())).thenReturn(List.of());

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertTrue(recipients.isEmpty());
    }

    @Test
    public void testGetRecipientsHappyPathIncludesRecipient() throws Exception {
        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertEquals(1, recipients.size());
        assertEquals(recipient, recipients.get(0));
        verify(ltiUserRepository, never()).save(any(LtiUserEntity.class));
    }

    @Test
    public void testGetRecipientsSetsLmsUserIdWhenMissing() throws Exception {
        LtiUserEntity recipientNoLmsId = LtiUserEntity.builder().userId(2L).lmsUserId(null).email("student@example.com").build();
        Participant participantNoLmsId = Participant.builder().ltiUserEntity(recipientNoLmsId).consent(true).build();
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(List.of(participantNoLmsId))
            .thenReturn(List.of());
        when(ltiUserRepository.findAllByEmailInAndPlatformDeployment_KeyId(any(), anyLong())).thenReturn(List.of(recipientNoLmsId));
        when(ltiUserRepository.save(any(LtiUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertEquals(1, recipients.size());
        assertEquals("s1", recipientNoLmsId.getLmsUserId());
        verify(ltiUserRepository).save(recipientNoLmsId);
    }

    @Test
    public void testGetRecipientsExcludesWhenNoTerracottaUserFound() throws Exception {
        when(ltiUserRepository.findAllByEmailInAndPlatformDeployment_KeyId(any(), anyLong())).thenReturn(List.of());

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertTrue(recipients.isEmpty());
    }

    @Test
    public void testGetRecipientsExcludesWhenWrongLmsUser() throws Exception {
        LtiUserEntity wrongLmsUser = LtiUserEntity.builder().userId(2L).lmsUserId("different-lms-id").email("student@example.com").build();
        when(ltiUserRepository.findAllByEmailInAndPlatformDeployment_KeyId(any(), anyLong())).thenReturn(List.of(wrongLmsUser));

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertTrue(recipients.isEmpty());
    }

    @Test
    public void testGetRecipientsExcludesWhenNoParticipantForUser() throws Exception {
        LtiUserEntity otherUser = LtiUserEntity.builder().userId(999L).lmsUserId("s1").build();
        Participant otherParticipant = Participant.builder().ltiUserEntity(otherUser).consent(true).build();
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(List.of(otherParticipant))
            .thenReturn(List.of());

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertTrue(recipients.isEmpty());
    }

    @Test
    public void testGetRecipientsExcludesWhenConsentedOnlyAndNotConsented() throws Exception {
        message.getConfiguration().setToConsentedOnly(true);
        participant.setConsent(false);

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertTrue(recipients.isEmpty());
    }

    @Test
    public void testGetRecipientsIncludesWhenConsentedOnlyAndConsented() throws Exception {
        message.getConfiguration().setToConsentedOnly(true);
        participant.setConsent(true);

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertEquals(1, recipients.size());
    }

    @Test
    public void testGetRecipientsExcludesWhenMultiVersionNoGroupAndNotDefault() throws Exception {
        container.getMessages().add(Message.builder().build());
        participant.setGroup(null);
        condition.setDefaultCondition(false);

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertTrue(recipients.isEmpty());
    }

    @Test
    public void testGetRecipientsIncludesWhenMultiVersionNoGroupButDefaultMessage() throws Exception {
        container.getMessages().add(Message.builder().build());
        participant.setGroup(null);
        condition.setDefaultCondition(true);

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertEquals(1, recipients.size());
    }

    @Test
    public void testGetRecipientsExcludesWhenGroupMismatch() throws Exception {
        container.getMessages().add(Message.builder().build());
        Group otherGroup = Group.builder().groupId(999L).build();
        participant.setGroup(otherGroup);

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertTrue(recipients.isEmpty());
    }

    @Test
    public void testGetRecipientsIncludesWhenGroupMatches() throws Exception {
        container.getMessages().add(Message.builder().build());
        participant.setGroup(group);

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertEquals(1, recipients.size());
    }

    @Test
    public void testGetRecipientsExcludesWhenExcludeMatchTypeAndRulesMatch() throws Exception {
        message.getConfiguration().setRecipientMatchType(MessageRecipientMatchType.EXCLUDE);
        when(ruleComparisonService.recipientRuleSetsMatch(any(List.class), anyMap())).thenReturn(true);

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertTrue(recipients.isEmpty());
    }

    @Test
    public void testGetRecipientsIncludesWhenExcludeMatchTypeAndRulesDoNotMatch() throws Exception {
        message.getConfiguration().setRecipientMatchType(MessageRecipientMatchType.EXCLUDE);
        when(ruleComparisonService.recipientRuleSetsMatch(any(List.class), anyMap())).thenReturn(false);

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertEquals(1, recipients.size());
    }

    @Test
    public void testGetRecipientsExcludesWhenIncludeMatchTypeAndRulesDoNotMatch() throws Exception {
        message.getConfiguration().setRecipientMatchType(MessageRecipientMatchType.INCLUDE);
        when(ruleComparisonService.recipientRuleSetsMatch(any(List.class), anyMap())).thenReturn(false);

        List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

        assertTrue(recipients.isEmpty());
    }

    @Test
    public void testGetRecipientsWrapsLmsSubmissionsException() throws Exception {
        when(ruleComparisonService.getLmsSubmissions(message)).thenThrow(new RuntimeException("lms down"));

        ApiException exception = assertThrows(ApiException.class, () -> messageSendService.getRecipients(message));

        assertTrue(exception.getMessage().contains("Error retrieving LMS submissions"));
    }

    @Test
    public void testParseMessageBodyEmailReturnsOuterHtml() throws Exception {
        message.getContent().setHtml("<p>Hello World</p>");
        message.getConfiguration().setType(MessageType.EMAIL);

        String result = messageSendService.parseMessageBody(message, recipient, Map.of());

        assertTrue(result.contains("Hello World"));
        assertTrue(result.contains("<html"));
    }

    @Test
    public void testParseMessageBodyConversationReturnsWholeText() throws Exception {
        message.getContent().setHtml("<p>Hello World</p>");
        message.getConfiguration().setType(MessageType.CONVERSATION);

        String result = messageSendService.parseMessageBody(message, recipient, Map.of(), false);

        assertEquals("Hello World", result);
    }

    @Test
    public void testParseMessageBodyConditionalTextMatchReplacesContent() throws Exception {
        UUID ctUuid = UUID.randomUUID();
        message.getContent().setHtml(
            "<p>Intro</p><conditional-text data-id=\"" + ctUuid + "\" data-label=\"conditional text: Label\">placeholder</conditional-text>"
        );
        MessageConditionalTextResult result = MessageConditionalTextResult.builder().html("<span>MatchedText</span>").build();
        MessageConditionalText conditionalText = MessageConditionalText.builder().result(result).build();
        conditionalText.setUuid(ctUuid);
        when(conditionalTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(any(UUID.class), any(UUID.class), anyString()))
            .thenReturn(java.util.Optional.of(conditionalText));
        when(ruleComparisonService.conditionalTextRuleSetsMatch(any(MessageConditionalText.class), anyMap())).thenReturn(true);

        String result2 = messageSendService.parseMessageBody(message, recipient, Map.of());

        assertTrue(result2.contains("MatchedText"));
        assertFalse(result2.contains("placeholder"));
    }

    @Test
    public void testParseMessageBodyConditionalTextNoMatchRemovesElement() throws Exception {
        UUID ctUuid = UUID.randomUUID();
        message.getContent().setHtml(
            "<p>Intro</p><conditional-text data-id=\"" + ctUuid + "\" data-label=\"conditional text: Label\">placeholder</conditional-text>"
        );
        MessageConditionalText conditionalText = MessageConditionalText.builder().result(MessageConditionalTextResult.builder().html("<span>x</span>").build()).build();
        conditionalText.setUuid(ctUuid);
        when(conditionalTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(any(UUID.class), any(UUID.class), anyString()))
            .thenReturn(java.util.Optional.of(conditionalText));
        when(ruleComparisonService.conditionalTextRuleSetsMatch(any(MessageConditionalText.class), anyMap())).thenReturn(false);

        String result = messageSendService.parseMessageBody(message, recipient, Map.of());

        assertFalse(result.contains("placeholder"));
        assertTrue(result.contains("Intro"));
    }

    @Test
    public void testParseMessageBodyConditionalTextNotFoundReturnsNull() throws Exception {
        UUID ctUuid = UUID.randomUUID();
        message.getContent().setHtml(
            "<conditional-text data-id=\"" + ctUuid + "\" data-label=\"conditional text: Label\">placeholder</conditional-text>"
        );
        when(conditionalTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(any(UUID.class), any(UUID.class), anyString()))
            .thenReturn(java.util.Optional.empty());

        String result = messageSendService.parseMessageBody(message, recipient, Map.of());

        assertNull(result);
    }

    @Test
    public void testParseMessageBodyPreviewUsesContentConditionalTextsList() throws Exception {
        UUID ctUuid = UUID.randomUUID();
        MessageConditionalTextResult result = MessageConditionalTextResult.builder().html("<span>PreviewMatch</span>").build();
        MessageConditionalText conditionalText = MessageConditionalText.builder().result(result).build();
        conditionalText.setUuid(ctUuid);
        message.getContent().setConditionalTexts(List.of(conditionalText));
        message.getContent().setHtml(
            "<conditional-text data-id=\"" + ctUuid + "\" data-label=\"conditional text: Label\">placeholder</conditional-text>"
        );
        when(ruleComparisonService.conditionalTextRuleSetsMatch(any(MessageConditionalText.class), anyMap())).thenReturn(true);

        String result2 = messageSendService.parseMessageBody(message, recipient, Map.of(), true);

        assertTrue(result2.contains("PreviewMatch"));
        verify(conditionalTextRepository, never()).findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(any(UUID.class), any(UUID.class), anyString());
    }

    @Test
    public void testParseMessageBodyPipedTextValueFoundReplacesElement() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        message.getContent().setHtml(
            "<piped-text data-id=\"" + itemUuid + "\" data-label=\"piped text: Label\">placeholder</piped-text>"
        );
        MessagePipedTextItemValue value = MessagePipedTextItemValue.builder().value("PipedValue").user(recipient).build();
        MessagePipedTextItem item = MessagePipedTextItem.builder().values(List.of(value)).build();
        item.setUuid(itemUuid);
        when(pipedTextItemRepository.findByUuidAndPipedText_UuidAndPipedText_Content_UuidAndPipedText_Content_Message_Container_Owner_LmsUserId(
            any(UUID.class), any(UUID.class), any(UUID.class), anyString()
        )).thenReturn(java.util.Optional.of(item));
        MessagePipedText pipedText = MessagePipedText.builder().build();
        pipedText.setUuid(UUID.randomUUID());
        message.getContent().setPipedText(pipedText);

        String result = messageSendService.parseMessageBody(message, recipient, Map.of());

        assertTrue(result.contains("PipedValue"));
        assertFalse(result.contains("placeholder"));
    }

    @Test
    public void testParseMessageBodyPipedTextValueMissingRemovesElement() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        message.getContent().setHtml(
            "<p>Intro</p><piped-text data-id=\"" + itemUuid + "\" data-label=\"piped text: Label\">placeholder</piped-text>"
        );
        MessagePipedTextItem item = MessagePipedTextItem.builder().values(List.of()).build();
        item.setUuid(itemUuid);
        when(pipedTextItemRepository.findByUuidAndPipedText_UuidAndPipedText_Content_UuidAndPipedText_Content_Message_Container_Owner_LmsUserId(
            any(UUID.class), any(UUID.class), any(UUID.class), anyString()
        )).thenReturn(java.util.Optional.of(item));
        MessagePipedText pipedText = MessagePipedText.builder().build();
        pipedText.setUuid(UUID.randomUUID());
        message.getContent().setPipedText(pipedText);

        String result = messageSendService.parseMessageBody(message, recipient, Map.of());

        assertFalse(result.contains("placeholder"));
        assertTrue(result.contains("Intro"));
    }

    @Test
    public void testParseMessageBodyPipedTextNotFoundReturnsNull() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        message.getContent().setHtml(
            "<piped-text data-id=\"" + itemUuid + "\" data-label=\"piped text: Label\">placeholder</piped-text>"
        );
        when(pipedTextItemRepository.findByUuidAndPipedText_UuidAndPipedText_Content_UuidAndPipedText_Content_Message_Container_Owner_LmsUserId(
            any(UUID.class), any(UUID.class), any(UUID.class), anyString()
        )).thenReturn(java.util.Optional.empty());
        MessagePipedText pipedText = MessagePipedText.builder().build();
        pipedText.setUuid(UUID.randomUUID());
        message.getContent().setPipedText(pipedText);

        String result = messageSendService.parseMessageBody(message, recipient, Map.of());

        assertNull(result);
    }

}
