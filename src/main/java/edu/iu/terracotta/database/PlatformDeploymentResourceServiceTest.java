package edu.iu.terracotta.database;

import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.service.common.ResourceService;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings({"PMD.UncommentedEmptyMethodBody"})
public class PlatformDeploymentResourceServiceTest implements ResourceService<PlatformDeployment> {

    private static final String PLATFORM_DEPLOYMENT_RESOURCE = "classpath:test_data/platform_deployment";

    @Override
    public String getDirectoryPath() {
        return PLATFORM_DEPLOYMENT_RESOURCE;
    }

    @Override
    public void setDefaults() {

    }

}
