package edu.iu.terracotta.service.app.messaging.impl.preview;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.model.dto.messaging.preview.MessagePreviewDto;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextService;
import edu.iu.terracotta.service.app.messaging.MessagePreviewService;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextService;
import edu.iu.terracotta.service.app.messaging.MessageRecipientRuleService;
import edu.iu.terracotta.service.app.messaging.MessageRecipientRuleSetService;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;
import edu.iu.terracotta.service.app.messaging.MessageSendService;
import io.jsonwebtoken.lang.Collections;

@Service
public class MessagePreviewServiceImpl implements MessagePreviewService {

    @Autowired private ParticipantRepository participantRepository;
    @Autowired private MessageConditionalTextService conditionalTextService;
    @Autowired private MessagePipedTextService pipedTextService;
    @Autowired private MessageSendService messageSendService;
    @Autowired private MessageRecipientRuleService recipientRuleService;
    @Autowired private MessageRecipientRuleSetService recipientRuleSetService;
    @Autowired private MessageRuleComparisonService ruleComparisonService;

    @Override
    public MessagePreviewDto preview(MessagePreviewDto messagePreviewDto, Message message) throws ParticipantNotMatchingException, MessageBodyParseException {
        Participant participant = participantRepository.findByUuid(messagePreviewDto.getId())
            .orElseThrow(() -> new ParticipantNotMatchingException(String.format("Participant not found with UUID: [{}]", messagePreviewDto.getId())));

        message.getContent().setHtml(messagePreviewDto.getBody());
        message.getContent().getConditionalTexts().clear();
        messagePreviewDto.getConditionalTexts()
            .forEach(
                conditionalTextDto ->
                    conditionalTextService.create(
                        conditionalTextDto,
                        message.getContent()
                    )
            );
        message.getContent().setPipedText(
            pipedTextService.fromDto(
                messagePreviewDto.getPipedText(),
                MessagePipedText.builder()
                    .content(message.getContent())
                    .build(),
                true
            )
        );
        message.getRuleSets().clear();
        messagePreviewDto.getRuleSets()
            .forEach(
                ruleSetDto -> {
                    recipientRuleSetService.create(ruleSetDto, message);
                    recipientRuleService.create(ruleSetDto.getRules(), message.getRuleSets().getLast());
                }
            );

        Map<String, List<LmsSubmission>> lmsSubmissions;

        try {
            if (CollectionUtils.isEmpty(message.getRuleSets()) && CollectionUtils.isEmpty(message.getContent().getConditionalTexts())) {
                // If there are no rule sets or conditional texts, we can skip fetching LMS submissions
                lmsSubmissions = Collections.emptyMap();
            } else {
                lmsSubmissions = ruleComparisonService.getLmsSubmissions(message);
            }
        } catch (Exception e) {
            throw new MessageBodyParseException(String.format("Error retrieving LMS submissions for message ID: [%s]", message.getId()), e);
        }

        Map<String, List<LmsSubmission>> participantSubmissions = lmsSubmissions.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream()
                        .filter(lmsSubmission -> Strings.CI.equals(lmsSubmission.getUserLoginId(), participant.getLtiUserEntity().getEmail()))
                        .toList()
                )
            );

        return MessagePreviewDto.builder()
            .id(participant.getUuid())
            .body(messageSendService.parseMessageBody(
                    message,
                    participant.getLtiUserEntity(),
                    participantSubmissions,
                    true
                )
            )
            .build();
    }

}
