package edu.iu.terracotta.service.app.messaging.impl.conversation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsConversation;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsCreateConversationOptions;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUtils;
import edu.iu.terracotta.dao.entity.messaging.attachment.MessageContentAttachment;
import edu.iu.terracotta.dao.entity.messaging.log.MessageLog;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.enums.messaging.MessageProcessingStatus;
import edu.iu.terracotta.dao.repository.messaging.log.MessageLogRepository;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.exceptions.messaging.MessageNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageSendConversationException;
import edu.iu.terracotta.service.app.messaging.MessageConversationService;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;
import edu.iu.terracotta.service.app.messaging.MessageSendService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class MessageConversationServiceImpl implements MessageConversationService {

    @Autowired private MessageLogRepository messageLogRepository;
    @Autowired private ApiClient apiClient;
    @Autowired private LmsUtils lmsUtils;
    @Autowired private MessageSendService messageSendService;
    @Autowired private MessageRuleComparisonService ruleComparisonService;

    @Override
    public void send(Message message) throws MessageNotMatchingException, MessageSendConversationException, TerracottaConnectorException {
        String lmsCourseId = lmsUtils.parseCourseId(message.getPlatformDeployment(), message.getExperiment().getLtiContextEntity().getContext_memberships_url());

        LtiUserEntity ltiUserEntity = null;
        List<LmsConversation> lmsConversations = null;
        String body = null;
        MessageLog messageLog = null;

        try {
            log.info("Sending conversations to LMS for LMS course ID: [{}] and instructor LMS ID: [{}]", lmsCourseId, message.getOwner().getLmsUserId());
            List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

            // get all uploaded attachment IDs
            List<String> attachmentIds = CollectionUtils.emptyIfNull(message.getContent().getAttachments()).stream()
                .map(MessageContentAttachment::getLmsId)
                .toList();

            Map<String, List<LmsSubmission>> lmsSubmissions;

            try {
                lmsSubmissions = ruleComparisonService.getLmsSubmissions(message);
            } catch (Exception e) {
                throw new MessageBodyParseException(String.format("Error retrieving LMS submissions for message ID: [%s]", message.getId()), e);
            }

            for (LtiUserEntity recipient : recipients) {
                log.info("Sending conversation message ID: [{}] to terracotta user ID: [{}]", message.getId(), recipient.getUserId());

                if (recipient.getLmsUserId() == null) {
                    String errorMessage = String.format("Skipping recipient with no required LMS user ID for terracotta user ID: [%s] and message ID: [%s]", recipient.getUserId(), message.getId());
                    log.warn(errorMessage);
                    messageLog = MessageLog.builder()
                        .body(body)
                        .errorMessage(errorMessage)
                        .message(message)
                        .recipient(recipient)
                        .status(MessageProcessingStatus.ERROR)
                        .build();
                    messageLogRepository.save(messageLog);

                    continue;
                }

                messageLog = null;
                lmsConversations = null;
                ltiUserEntity = recipient;

                Map<String, List<LmsSubmission>> participantSubmissions = lmsSubmissions.entrySet().stream()
                    .collect(
                        Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                .filter(lmsSubmission -> Strings.CS.equals(Long.toString(lmsSubmission.getUserId()), recipient.getLmsUserId()))
                                .toList()
                        )
                    );

                // handle conditional text / piped text placeholders in the message body
                body = messageSendService.parseMessageBody(
                    message,
                    recipient,
                    participantSubmissions
                );

                // send to LMS
                lmsConversations = apiClient.sendConversation(
                    LmsCreateConversationOptions.builder()
                        .attachmentIds(attachmentIds)
                        .body(body)
                        .forceNew(true)
                        .groupConversation(false)
                        .lmsUserId(recipient.getLmsUserId())
                        .subject(message.getConfiguration().getSubject())
                        .build(),
                    message.getOwner()
                );

                // add the message log
                messageLog = MessageLog.builder()
                    .body(body)
                    .message(message)
                    .recipient(recipient)
                    .remoteId(lmsConversations.get(0).getId())
                    .status(MessageProcessingStatus.SENT)
                    .build();
                messageLogRepository.save(messageLog);
            }

            log.info("Completed sending conversations to LMS for LMS course ID: [{}] and instructor LMS ID: [{}]", lmsCourseId, message.getOwner().getLmsUserId());
        } catch (Exception e) {
            String errorMessage = String.format("Error sending conversations to LMS for LMS course ID: [%s] and instructor LMS ID: [%s]", lmsCourseId, message.getOwner().getLmsUserId());
            log.error(errorMessage, e);

            if (lmsConversations != null) {
                if (messageLog == null) {
                    messageLog = MessageLog.builder()
                        .body(body)
                        .errorMessage(errorMessage)
                        .message(message)
                        .recipient(ltiUserEntity)
                        .status(MessageProcessingStatus.ERROR)
                        .build();
                } else {
                    messageLog.setStatus(MessageProcessingStatus.ERROR);
                }

                messageLogRepository.save(messageLog);
            }

            throw new MessageSendConversationException(String.format("Error sending conversations to LMS for LMS course ID: [{}] and instructor LMS ID: [{}]", lmsCourseId, message.getOwner().getLmsUserId()), e);
        }
    }

}
