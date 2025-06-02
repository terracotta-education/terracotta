package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;

public interface ComponentUtils {

    int calculateNextOrder(long exposureId, LtiUserEntity owner);

}
