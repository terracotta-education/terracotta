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
    static final String SUPPORT_EMAIL = "info@terracotta.education";
    static final String BODY = """
        <p>Dear %s,</p>
        <p>There was an error recreating the Terracotta assignments in your newly copied Canvas course. Please login to the new Canvas course, relaunch Terracotta, and reauthorize Terracotta to function in the LMS.</p>
        <p>If you have further questions, please send them to <a href="mailto:%s">%s</a>.</p>
        <p>Thank you,<br>
        Terracotta Support</p>
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
            emailMessage.setText(String.format(BODY, displayName, SUPPORT_EMAIL, SUPPORT_EMAIL), true);
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
