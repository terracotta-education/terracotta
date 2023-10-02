/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.terracotta.database;

import edu.iu.terracotta.repository.LtiUserRepository;
import edu.iu.terracotta.repository.PlatformDeploymentRepository;
import edu.iu.terracotta.repository.ToolDeploymentRepository;
import jakarta.annotation.PostConstruct;
import edu.iu.terracotta.config.ApplicationConfig;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ToolDeployment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Check if the database has initial data in it,
 * if it is empty on startup then we populate it with some initial data
 */
@Component
@Profile("!test")
@SuppressWarnings({"PMD.GuardLogStatement"})
// only load this when running the application (not for unit tests which have the 'testing' profile active)
public class DatabasePreload {

    static final Logger log = LoggerFactory.getLogger(DatabasePreload.class);

    @Autowired
    ApplicationConfig applicationConfig;

    @Autowired
    @SuppressWarnings({"SpringJavaAutowiredMembersInspection", "SpringJavaAutowiringInspection"})
    LtiUserRepository ltiUserRepository;
    @Autowired
    @SuppressWarnings({"SpringJavaAutowiredMembersInspection", "SpringJavaAutowiringInspection"})
    PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired
    @SuppressWarnings({"SpringJavaAutowiredMembersInspection", "SpringJavaAutowiringInspection"})
    ToolDeploymentRepository toolDeploymentRepository;

    @Autowired
    PlatformDeploymentResourceService platformDeploymentResources;

    @Autowired
    LtiUserEntityResourceService ltiUserEntityResourceService;

    @PostConstruct
    public void init() throws IOException {

        if (platformDeploymentRepository.count() > 0) {
            // done, no preloading
            log.info("INIT - no preload");
        } else {
            buildDataFromFiles();
        }
    }

    public void buildDataFromFiles() throws IOException {
        Set<PlatformDeployment> deploymentPlatforms = platformDeploymentResources.getResources(PlatformDeployment.class);
        for (PlatformDeployment deploymentPlatform : deploymentPlatforms) {
            log.info("Storing: " + deploymentPlatform.getKeyId() + " : " + deploymentPlatform.getIss());
            PlatformDeployment savedDeploymentPlatform = platformDeploymentRepository.save(deploymentPlatform);
            for (ToolDeployment toolDeployment : deploymentPlatform.getToolDeployments()) {
                toolDeployment.setPlatformDeployment(savedDeploymentPlatform);
                toolDeploymentRepository.save(toolDeployment);
            }
        }

        Set<LtiUserEntity> users = ltiUserEntityResourceService.getResources(LtiUserEntity.class);
        for (LtiUserEntity user : users) {
            ltiUserRepository.save(user);
        }
    }
}
