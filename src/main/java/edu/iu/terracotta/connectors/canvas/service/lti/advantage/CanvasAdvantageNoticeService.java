package edu.iu.terracotta.connectors.canvas.service.lti.advantage;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;

/**
 * Registers this tool as a handler for Canvas's LTI Advantage Platform Notification Service
 * course-copy notice (LtiContextCopyNotice), so NoticeController actually gets called - see
 * NoticeController for the inbound half of this feature.
 */
public interface CanvasAdvantageNoticeService {

    /**
     * A no-op unless the PLATFORM_NOTIFICATIONS feature is enabled for this deployment's platform
     * deployment (see FeatureService), or once already done for this deployment (see
     * ToolDeployment.noticeHandlerRegistered) - safe to call on every launch. A failed attempt is
     * retried on a cooldown rather than on every launch (see
     * ToolDeployment.noticeHandlerRegistrationAttemptedAt).
     */
    void ensureNoticeHandlerRegistered(ToolDeployment toolDeployment);

}
