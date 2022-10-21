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

import edu.iu.terracotta.config.ApplicationConfig;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.test.Conditions;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.LtiContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Set;

/**
 * Check if the database has initial data in it,
 * if it is empty on startup then we populate it with some initial data
 */
@Component
@Profile("test")
// only load this when running unit tests (not for for the application which has the '!testing' profile active)
public class DatabasePreloadTest {

    static final Logger log = LoggerFactory.getLogger(DatabasePreloadTest.class);

    @Autowired
    ApplicationConfig applicationConfig;

    @Autowired
    @SuppressWarnings({"SpringJavaAutowiredMembersInspection", "SpringJavaAutowiringInspection"})
    AllRepositories allRepositories;

    @Autowired
    @SuppressWarnings({"SpringJavaAutowiredMembersInspection", "SpringJavaAutowiringInspection"})
    LtiContextRepository ltiContextRepository;

    @Autowired
    PlatformDeploymentResourceServiceTest platformDeploymentResources;

    @Autowired
    LtiUserEntityResourceServiceTest ltiUserEntityResourceService;

    @Autowired
    ExperimentResourceServiceTest experimentResourceService;

    @Autowired
    ConditionResourceService conditionResourceService;

    @Autowired
    LtiContextEntityResourceServiceTest ltiContextEntityResourceService;

    @Autowired
    ExposureResourceServiceTest exposureResourceService;


    @PostConstruct
    public void initTest() throws IOException {

        if (allRepositories.platformDeploymentRepository.count() > 0) {
            // done, no preloading
            log.info("INIT - no preload");
        } else {
            buildDataFromFilesTest();
        }
    }

    public void buildDataFromFilesTest() throws IOException {
        Set<PlatformDeployment> deploymentPlatforms = platformDeploymentResources.getResources(PlatformDeployment.class);
        for (PlatformDeployment deploymentPlatform : deploymentPlatforms) {
            log.info("Storing (test): " + deploymentPlatform.getKeyId() + " : " + deploymentPlatform.getIss());
            allRepositories.platformDeploymentRepository.save(deploymentPlatform);
        }

        Set<LtiContextEntity> contextEntities = ltiContextEntityResourceService.getResources(LtiContextEntity.class);
        for(LtiContextEntity ltiContextEntity : contextEntities){
            log.info("Storing " + ltiContextEntity.getContextId() + " : " + ltiContextEntity.getTitle());
            ltiContextRepository.save(ltiContextEntity);
        }

        Set<LtiUserEntity> users = ltiUserEntityResourceService.getResources(LtiUserEntity.class);
        for (LtiUserEntity user : users) {
            allRepositories.ltiUserRepository.save(user);
        }

        Set<Experiment> experiments = experimentResourceService.getResources(Experiment.class);
        for(Experiment experiment : experiments){
            log.info("Storing: " + experiment.getExperimentId() + " : " + experiment.getTitle());
            allRepositories.experimentRepository.save(experiment);
        }
        allRepositories.experimentRepository.flush();

        Set<Conditions> conditions = conditionResourceService.getResources(Conditions.class);
        for(Conditions conditionList : conditions){
            for(Condition condition : conditionList.getConditionList()){
                log.info("Storing : " + condition.getName());
                allRepositories.conditionRepository.saveAndFlush(condition);
            }
        }

        Set<Exposure> exposures = exposureResourceService.getResources(Exposure.class);
        for(Exposure exposure : exposures){
            log.info("Storing : " + exposure.getTitle());
            allRepositories.exposureRepository.save(exposure);
        }
    }
}
