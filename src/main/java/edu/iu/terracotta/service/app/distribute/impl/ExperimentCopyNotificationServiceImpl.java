package edu.iu.terracotta.service.app.distribute.impl;

import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyNotificationService;
import edu.iu.terracotta.service.app.notification.NotificationEmailSender;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperimentCopyNotificationServiceImpl implements ExperimentCopyNotificationService {

    static final String SUBJECT = "Terracotta experiment copy notification";
    static final String BODY = """
        <p>Dear %s,</p>
        <p>There was an error recreating the Terracotta assignments in your newly copied Canvas course. Please login to the new Canvas course, relaunch Terracotta, and reauthorize Terracotta to function in the LMS.</p>
        <p>If you have further questions, please send them to %s.</p>
        <p>Thank you,<br>
        Terracotta Support</p>
        """;

    private final NotificationEmailSender notificationEmailSender;

    @Override
    public void notifyLmsFailure(LtiUserEntity instructor) {
        notificationEmailSender.sendHtml(
            instructor,
            SUBJECT,
            String.format(BODY, NotificationEmailSender.greetingName(instructor), NotificationEmailSender.supportLinkHtml()),
            "experiment copy failure"
        );
    }

}
