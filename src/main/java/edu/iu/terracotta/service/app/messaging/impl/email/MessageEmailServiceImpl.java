package edu.iu.terracotta.service.app.messaging.impl.email;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUtils;
import edu.iu.terracotta.dao.entity.messaging.attachment.MessageContentAttachment;
import edu.iu.terracotta.dao.entity.messaging.log.MessageLog;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.dto.messaging.send.MessageSendTestDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageProcessingStatus;
import edu.iu.terracotta.dao.repository.messaging.log.MessageLogRepository;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.exceptions.messaging.MessageNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageSendEmailException;
import edu.iu.terracotta.service.app.messaging.MessageEmailService;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;
import edu.iu.terracotta.service.app.messaging.MessageSendService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class MessageEmailServiceImpl implements MessageEmailService {

    @Autowired private MessageLogRepository messageLogRepository;
    @Autowired private MessageSendService messageSendService;
    @Autowired private MessageRuleComparisonService ruleComparisonService;
    @Autowired private JavaMailSender javaMailSender;
    @Autowired private LmsUtils lmsUtils;

    @Value("${app.messaging.email.from:no-reply@mail.terracotta.education}")
    private String from;

    @Value("${app.messaging.email.batch.size:14}")
    private int batchSize;

    @Value("${app.messaging.email.interval.seconds:1}")
    private int interval;

    @Value("${app.messaging.email.test.subject:Test message from Terracotta}")
    private String testSubject;

    @Value("${app.messaging.email.test.body:Congratulations! You have successfully sent a test message.}")
    private String testBody;

    @Value("${aws.region:US_EAST_2}")
    private String awsRegion;

    @Override
    public void send(Message message) throws MessageNotMatchingException, MessageSendEmailException, TerracottaConnectorException {
        LtiUserEntity ltiUserEntity = null;
        MimeMessageHelper emailMessage = null;
        String body = null;
        MessageLog messageLog = null;

        try {
            log.info("Sending emails for message ID: [{}]", message.getId());
            List<LtiUserEntity> recipients = messageSendService.getRecipients(message);

            emailMessage = new MimeMessageHelper(
                javaMailSender.createMimeMessage(),
                true
            );
            emailMessage.setFrom(sender(message));
            emailMessage.setSubject(message.getSubject());

            List<String> messageReplyTos = CollectionUtils.emptyIfNull(message.getReplyTo()).stream()
                .map(replyTo -> replyTo.getEmail())
                .collect(Collectors.toList());

            emailMessage.getMimeMessage().setReplyTo(
                CollectionUtils.emptyIfNull(messageReplyTos).stream()
                    .map(
                        replyTo -> {
                            try {
                                return new InternetAddress(replyTo);
                            } catch (AddressException e) {
                                log.error("Error converting reply-to address: [{}]", replyTo, e);
                                return null;
                            }
                        }
                    )
                    .filter(Objects::nonNull)
                    .toArray(replyTo -> new InternetAddress[replyTo])
            );

            // add any attachments to the email (stored in the LMS)
            List<Map<String, ByteArrayResource>> attachments = new ArrayList<>();

            for (MessageContentAttachment attachment : message.getContent().getAttachments()) {
                try (InputStream inputStream = URI.create(attachment.getUrl()).toURL().openStream()) {
                    attachments.add(
                        Collections.singletonMap(
                            attachment.getFilename(),
                            new ByteArrayResource(
                                IOUtils.toByteArray(inputStream)
                            )
                        )
                    );
                    inputStream.close();
                } catch (Exception e) {
                    log.error("Error retrieving file ID: [{}] from the LMS for message with ID: [{}]", attachment.getId(), message.getId(), e);
                }
            }

            for (Map<String, ByteArrayResource> attachment : attachments) {
                for (Map.Entry<String, ByteArrayResource> a : attachment.entrySet()) {
                    try {
                        emailMessage.addAttachment(a.getKey(), a.getValue());
                    } catch (Exception e) {
                        log.error("Error attaching file to email message with ID: [{}]", message.getId(), e);
                    }
                }
            }

            // create recipient batch lists to avoid sending too many emails at once
            List<List<LtiUserEntity>> recipientBatches = new ArrayList<>();
            int listSize = recipients.size();

            for (int i = 0; i < listSize; i += batchSize) {
                int endIndex = Math.min(i + batchSize, listSize);
                List<LtiUserEntity> recipientBatch = recipients.subList(i, endIndex);
                recipientBatches.add(new ArrayList<>(recipientBatch));
            }

            Map<String, List<LmsSubmission>> lmsSubmissions;

            try {
                lmsSubmissions = ruleComparisonService.getLmsSubmissions(message);
            } catch (Exception e) {
                throw new MessageBodyParseException(String.format("Error retrieving LMS submissions for message ID: [%s]", message.getId()), e);
            }

            // send each batch of emails with a delay
            for (List<LtiUserEntity> recipientBatch : recipientBatches) {
                try {
                    // pause before sending the next batch; AWS SES rate limit is 14 per second
                    Thread.sleep(interval * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                for (LtiUserEntity recipient : recipientBatch) {
                    log.info("Sending email message to terracotta user ID: [{}]", recipient.getUserId());
                    messageLog = null;
                    Map<String, List<LmsSubmission>> participantSubmissions = lmsSubmissions.entrySet().stream()
                        .collect(
                            Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream()
                                    .filter(lmsSubmission -> Strings.CS.equals(lmsSubmission.getUserId(), recipient.getLmsUserId()))
                                    .toList()
                            )
                        );
                    // handle conditional text / piped text placeholders in the message body
                    body = messageSendService.parseMessageBody(
                        message,
                        recipient,
                        participantSubmissions
                    );
                    sendEmailWithSES(message, body, emailMessage, recipient, messageLog);
                }
            }

            log.info("Completed sending emails for message ID: [{}]", message.getId());
        } catch (Exception e) {
            log.error("Error sending email for message ID: [{}] ", message.getId(), e);

            if (emailMessage != null) {
                messageLog = MessageLog.builder()
                    .body(body)
                    .message(message)
                    .recipient(ltiUserEntity)
                    .status(MessageProcessingStatus.ERROR)
                    .build();

                messageLogRepository.save(messageLog);
            }

            throw new MessageSendEmailException(
                String.format(
                    "Error sending email for LMS course ID: [%s] and instructor LMS ID: [%s]",
                    lmsUtils.parseCourseId(message.getPlatformDeployment(), message.getExperiment().getLtiContextEntity().getContext_memberships_url()),
                    message.getOwner().getLmsUserId()
                ),
                e
            );
        }
    }

    @Override
    public void sendTest(Message message, MessageSendTestDto messageSendTestDto) throws MessageSendEmailException, TerracottaConnectorException {
        log.info("Sending test email to: [{}] for message ID: [{}]", messageSendTestDto.getTo(), message.getId());
        MimeMessageHelper emailMessage = null;
        MessageLog messageLog = null;

        try {
            emailMessage = new MimeMessageHelper(
                javaMailSender.createMimeMessage(),
                true
            );

            emailMessage.setFrom(sender(message));
            emailMessage.setSubject(testSubject);

            sendEmailWithSES(
                message,
                testBody,
                emailMessage,
                LtiUserEntity.builder()
                    .email(messageSendTestDto.getTo())
                    .build(),
                messageLog,
                true
            );
            log.info("Completed sending test email to [{}] for message ID: [{}]", messageSendTestDto.getTo(), message.getId());
        } catch (Exception e) {
            log.error("Error sending test email to: [{}] for message ID: [{}] ", messageSendTestDto.getTo(), message.getId(), e);

            if (emailMessage != null) {
                messageLog = MessageLog.builder()
                    .body(messageSendTestDto.getMessage())
                    .message(message)
                    .recipient(message.getOwner())
                    .status(MessageProcessingStatus.ERROR)
                    .build();

                messageLogRepository.save(messageLog);
            }

            throw new MessageSendEmailException(
                String.format(
                    "Error sending test email for LMS course ID: [%s] and instructor LMS ID: [%s]",
                    lmsUtils.parseCourseId(message.getPlatformDeployment(), message.getExperiment().getLtiContextEntity().getContext_memberships_url()),
                    message.getOwner().getLmsUserId()
                ),
                e
            );
        }
    }

    private void sendEmailWithSES(Message message, String body, MimeMessageHelper emailMessage, LtiUserEntity recipient, MessageLog messageLog) throws MessagingException, IOException {
        sendEmailWithSES(message, body, emailMessage, recipient, messageLog, false);
    }

    private void sendEmailWithSES(Message message, String body, MimeMessageHelper emailMessage, LtiUserEntity recipient, MessageLog messageLog, boolean isTest) throws MessagingException, IOException {
        emailMessage.setTo(recipient.getEmail());
        emailMessage.setText(
            body,
            true
        );
        emailMessage.getMimeMessage().saveChanges();

        ByteArrayOutputStream messageOutputStream = new ByteArrayOutputStream();
        emailMessage.getMimeMessage().writeTo(messageOutputStream);

        SesClient sesClient = SesClient.builder()
            .region(Region.US_EAST_2)
            .build();
        SendRawEmailRequest sendRawEmailRequest = SendRawEmailRequest.builder()
            .destinations(List.of(recipient.getEmail()))
            .rawMessage(
                RawMessage.builder()
                    .data(SdkBytes.fromByteArray(messageOutputStream.toByteArray()))
                    .build()
            )
            .source(sender(message))
            .build();

        // send the email via AWS SES
        SendRawEmailResponse sendRawEmailResponse = sesClient.sendRawEmail(sendRawEmailRequest);

        if (isTest) {
            // do not log the test message
            return;
        }

        // add the message log
        messageLog = MessageLog.builder()
            .body(body)
            .message(message)
            .recipient(recipient)
            .remoteId(sendRawEmailResponse.messageId())
            .status(MessageProcessingStatus.SENT)
            .build();
        messageLogRepository.save(messageLog);
    }

    private String sender(Message message) {
        return String.format(
            "%s <%s>",
            message.getExperiment().getLtiContextEntity().getTitle(),
            from
        );
    }

}
