package edu.iu.terracotta.service.app.distribute.impl;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement")
public class ExperimentCopyNotificationServiceImpl implements ExperimentCopyNotificationService {

    static final String SUBJECT = "Terracotta experiment copy notification";
    static final String BODY = """
        Dear %s,

        There was an error recreating the Terracotta assignments in your newly-copied Canvas course. Please Login to the new Canvas course, re-launch Terracotta, and re-approve permissions.

        Thank you,
        Terracotta Support
        """;

    private final JavaMailSender javaMailSender;

    @Value("${app.messaging.email.from:no-reply@mail.terracotta.education}")
    private String from;

    @Value("${aws.ses.region:us-east-2}")
    private String awsRegion;

    @Override
    public void notifyLmsFailure(LtiUserEntity instructor) {
        if (instructor == null || StringUtils.isBlank(instructor.getEmail())) {
            log.warn("Not sending an experiment copy failure email to user ID: [{}] - no email address", instructor != null ? instructor.getUserId() : null);
            return;
        }

        try {
            String sender = String.format("Terracotta <%s>", from);
            String displayName = StringUtils.defaultIfBlank(instructor.getDisplayName(), "Instructor");

            MimeMessageHelper emailMessage = new MimeMessageHelper(javaMailSender.createMimeMessage(), false, "UTF-8");
            emailMessage.setFrom(sender);
            emailMessage.setTo(instructor.getEmail());
            emailMessage.setSubject(SUBJECT);
            emailMessage.setText(String.format(BODY, displayName), false);
            emailMessage.getMimeMessage().saveChanges();

            ByteArrayOutputStream messageOutputStream = new ByteArrayOutputStream();
            emailMessage.getMimeMessage().writeTo(messageOutputStream);

            SesClient sesClient = SesClient.builder()
                .region(Region.of(awsRegion))
                .build();

            sesClient.sendRawEmail(
                SendRawEmailRequest.builder()
                    .destinations(List.of(instructor.getEmail()))
                    .rawMessage(
                        RawMessage.builder()
                            .data(SdkBytes.fromByteArray(messageOutputStream.toByteArray()))
                            .build()
                    )
                    .source(sender)
                    .build()
            );

            log.info("Sent an experiment copy failure email to user ID: [{}]", instructor.getUserId());
        } catch (Exception e) {
            log.error("Error sending an experiment copy failure email to user ID: [{}]", instructor.getUserId(), e);
        }
    }

}
