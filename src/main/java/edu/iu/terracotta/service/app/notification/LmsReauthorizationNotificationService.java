package edu.iu.terracotta.service.app.notification;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;

/**
 * Tells an instructor, by email, that work Terracotta does on their behalf in the LMS failed
 * because their LMS API token no longer works, and that relaunching Terracotta will reconnect
 * it. For work that runs while they aren't there to see an error: scheduled messages, or a
 * student's check that uses the instructor's token.
 */
public interface LmsReauthorizationNotificationService {

    /**
     * @param failedWork what could not be done, as the second half of a sentence starting "so",
     *      e.g. "your scheduled message could not be sent". Plain text; it is HTML-escaped.
     */
    void notifyReauthorizationNeeded(LtiUserEntity instructor, String failedWork);

}
