package edu.iu.terracotta.service.app.notification.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.service.app.notification.LmsReauthorizationNotificationService;
import edu.iu.terracotta.service.app.notification.NotificationEmailSender;

@Service
public class LmsReauthorizationNotificationServiceImpl implements LmsReauthorizationNotificationService {

    static final String SUBJECT = "Terracotta needs you to reconnect your %s account";
    static final String BODY = """
        <p>Dear %s,</p>
        <p>Terracotta can no longer reach your %s account, so %s. Please log in to %s, relaunch Terracotta in your course, and reauthorize Terracotta to function in the LMS.</p>
        <p>If you have further questions, please send them to %s.</p>
        <p>Thank you,<br>
        Terracotta Support</p>
        """;

    // one reminder a day is enough; a dead token fails every scheduled message and student check
    // until the instructor relaunches. Kept per server, so a multi-server deployment can send one
    // per server - acceptable for a reminder.
    static final Duration QUIET_PERIOD = Duration.ofHours(24);

    private final NotificationEmailSender notificationEmailSender;
    private final Clock clock;
    private final Map<Long, Instant> lastSentByUserId = new ConcurrentHashMap<>();

    @Autowired
    public LmsReauthorizationNotificationServiceImpl(NotificationEmailSender notificationEmailSender) {
        this(notificationEmailSender, Clock.systemUTC());
    }

    LmsReauthorizationNotificationServiceImpl(NotificationEmailSender notificationEmailSender, Clock clock) {
        this.notificationEmailSender = notificationEmailSender;
        this.clock = clock;
    }

    @Override
    public void notifyReauthorizationNeeded(LtiUserEntity instructor, String failedWork) {
        if (instructor == null) {
            return;
        }

        Instant now = clock.instant();
        Instant previous = lastSentByUserId.get(instructor.getUserId());

        if (previous != null && previous.plus(QUIET_PERIOD).isAfter(now)) {
            return;
        }

        String lms = lmsName(instructor);
        boolean sent = notificationEmailSender.sendHtml(
            instructor,
            String.format(SUBJECT, lms),
            String.format(BODY, NotificationEmailSender.greetingName(instructor), lms, HtmlUtils.htmlEscape(failedWork), lms, NotificationEmailSender.supportLinkHtml()),
            "LMS reauthorization"
        );

        if (sent) {
            lastSentByUserId.put(instructor.getUserId(), now);
        }
    }

    private String lmsName(LtiUserEntity instructor) {
        LmsConnector connector = instructor.getPlatformDeployment() != null ? instructor.getPlatformDeployment().getLmsConnector() : null;

        if (connector == LmsConnector.CANVAS || connector == LmsConnector.BRIGHTSPACE) {
            return connector.title();
        }

        return "LMS";
    }

}
