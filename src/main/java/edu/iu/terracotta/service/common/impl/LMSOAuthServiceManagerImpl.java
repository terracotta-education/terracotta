package edu.iu.terracotta.service.common.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.service.canvas.impl.CanvasOAuthServiceImpl;
import edu.iu.terracotta.service.common.LMSOAuthService;
import edu.iu.terracotta.service.common.LMSOAuthServiceManager;

@Service
public class LMSOAuthServiceManagerImpl implements LMSOAuthServiceManager {

    @Autowired
    CanvasOAuthServiceImpl canvasOAuthService;

    @Override
    public LMSOAuthService getLMSOAuthService(PlatformDeployment platformDeployment) {
        // TODO implement checking database configuration

        return canvasOAuthService;
    }

    @Override
    public LMSOAuthService getLMSOAuthService(long platformDeploymentId) {

        // TODO implement checking database configuration

        return canvasOAuthService;
    }

}
