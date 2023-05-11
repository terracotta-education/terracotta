package edu.iu.terracotta.service.common;

import edu.iu.terracotta.model.PlatformDeployment;

public interface LMSOAuthServiceManager {

    LMSOAuthService<?> getLMSOAuthService(PlatformDeployment platformDeployment);

    LMSOAuthService<?> getLMSOAuthService(long platformDeploymentId);

}
