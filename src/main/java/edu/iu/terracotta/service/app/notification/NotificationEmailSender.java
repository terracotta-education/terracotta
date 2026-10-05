package edu.iu.terracotta.service.app.notification;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

/**
 * Sends Terracotta's own notices to a single user (not the messaging feature's emails) as HTML
 * through SES. Failures are logged, never thrown: a notice must not break the work it reports on.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement")
public class NotificationEmailSender {

    public static final String SUPPORT_EMAIL = "info@terracotta.education";

    private final JavaMailSender javaMailSender;

    @Value("${app.messaging.email.from:no-reply@mail.terracotta.education}")
    private String from;

    @Value("${aws.ses.region:us-east-2}")
    private String awsRegion;

    /**
     * @param purpose a few words for the log lines, e.g. "experiment copy failure"
     * @return whether the email was handed to SES
     */
    public boolean sendHtml(LtiUserEntity recipient, String subject, String htmlBody, String purpose) {
        if (recipient == null || StringUtils.isBlank(recipient.getEmail())) {
            log.warn("Not sending a {} email to user ID: [{}] - no email address", purpose, recipient != null ? recipient.getUserId() : null);
            return false;
        }

        try {
            String sender = String.format("Terracotta <%s>", from);

            MimeMessageHelper emailMessage = new MimeMessageHelper(javaMailSender.createMimeMessage(), false, "UTF-8");
            emailMessage.setFrom(sender);
            emailMessage.setTo(recipient.getEmail());
            emailMessage.setSubject(subject);
            emailMessage.setText(htmlBody, true);
            emailMessage.getMimeMessage().saveChanges();

            ByteArrayOutputStream messageOutputStream = new ByteArrayOutputStream();
            emailMessage.getMimeMessage().writeTo(messageOutputStream);

            SesClient sesClient = SesClient.builder()
                .region(Region.of(awsRegion))
                .build();

            sesClient.sendRawEmail(
                SendRawEmailRequest.builder()
                    .destinations(List.of(recipient.getEmail()))
                    .rawMessage(
                        RawMessage.builder()
                            .data(SdkBytes.fromByteArray(messageOutputStream.toByteArray()))
                            .build()
                    )
                    .source(sender)
                    .build()
            );

            log.info("Sent a {} email to user ID: [{}]", purpose, recipient.getUserId());

            return true;
        } catch (Exception e) {
            log.error("Error sending a {} email to user ID: [{}]", purpose, recipient.getUserId(), e);

            return false;
        }
    }

    /** The recipient's display name, HTML-escaped for a greeting, or "Instructor" when unknown. */
    public static String greetingName(LtiUserEntity recipient) {
        return HtmlUtils.htmlEscape(StringUtils.defaultIfBlank(recipient != null ? recipient.getDisplayName() : null, "Instructor"));
    }

    public static String supportLinkHtml() {
        return String.format("<a href=\"mailto:%s\">%s</a>", SUPPORT_EMAIL, SUPPORT_EMAIL);
    }

}
