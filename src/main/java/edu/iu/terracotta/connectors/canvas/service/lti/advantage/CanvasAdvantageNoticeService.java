package edu.iu.terracotta.connectors.canvas.service.lti.advantage;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;

/**
 * Registers this tool as a handler for Canvas's LTI Advantage Platform Notification Service
 * course-copy notice (LtiContextCopyNotice), so NoticeController actually gets called - see
 * NoticeController for the inbound half of this feature.
 */
public interface CanvasAdvantageNoticeService {

    /**
     * A no-op once already done for this deployment (see ToolDeployment.noticeHandlerRegistered)
     * - safe to call on every launch.
     */
    void ensureNoticeHandlerRegistered(ToolDeployment toolDeployment);

}
