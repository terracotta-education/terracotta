package edu.iu.terracotta.service.common.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.canvas.impl.CanvasOAuthServiceImpl;
import edu.iu.terracotta.service.common.LMSOAuthService;
import edu.iu.terracotta.service.common.LMSOAuthServiceManager;

@Service
public class LMSOAuthServiceManagerImpl implements LMSOAuthServiceManager {

    @Autowired
    private CanvasOAuthServiceImpl canvasOAuthService;

    @Autowired
    AllRepositories allRepositories;

    @Override
    public LMSOAuthService<?> getLMSOAuthService(PlatformDeployment platformDeployment) {
        if (canvasOAuthService.isConfigured(platformDeployment)) {
            return canvasOAuthService;
        }

        return null;
    }

    @Override
    public LMSOAuthService<?> getLMSOAuthService(long platformDeploymentId) {
        PlatformDeployment platformDeployment = allRepositories.platformDeploymentRepository.getReferenceById(platformDeploymentId);

        return getLMSOAuthService(platformDeployment);
    }

}
