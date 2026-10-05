package edu.iu.terracotta.service.app.distribute;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;

/**
 * Emails an instructor when automatically recreating their experiments in a newly copied course
 * (see ExperimentCopyCandidateService.recreateForContext) fails in the LMS - recreation runs in
 * the background right after the copy, so there's no live session to report it in.
 */
public interface ExperimentCopyNotificationService {

    /**
     * Best effort: a failure to send is logged, never thrown, so it can't interrupt recreation.
     */
    void notifyLmsFailure(LtiUserEntity instructor);

}
