package edu.iu.terracotta.database;

import edu.iu.terracotta.config.ApplicationConfig;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.test.Conditions;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.repository.ConditionRepository;
import edu.iu.terracotta.repository.ExperimentRepository;
import edu.iu.terracotta.repository.ExposureRepository;
import edu.iu.terracotta.repository.LtiContextRepository;
import edu.iu.terracotta.repository.LtiUserRepository;
import edu.iu.terracotta.repository.PlatformDeploymentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Set;

/**
 * Check if the database has initial data in it,
 * if it is empty on startup then we populate it with some initial data
 */
@Slf4j
@Component
@Profile("test")
@SuppressWarnings({"PMD.GuardLogStatement", "SpringJavaAutowiredMembersInspection", "SpringJavaAutowiringInspection"})
// only load this when running unit tests (not for for the application which has the '!testing' profile active)
public class DatabasePreloadTest {

    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ConditionRepository conditionRepository;
    @Autowired private ExposureRepository exposureRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired ApplicationConfig applicationConfig;
    @Autowired LtiContextRepository ltiContextRepository;
    @Autowired PlatformDeploymentResourceServiceTest platformDeploymentResources;
    @Autowired LtiUserEntityResourceServiceTest ltiUserEntityResourceService;
    @Autowired ExperimentResourceServiceTest experimentResourceService;
    @Autowired ConditionResourceService conditionResourceService;
    @Autowired LtiContextEntityResourceServiceTest ltiContextEntityResourceService;
    @Autowired ExposureResourceServiceTest exposureResourceService;

    @PostConstruct
    public void initTest() throws IOException {
        if (platformDeploymentRepository.count() > 0) {
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
            platformDeploymentRepository.save(deploymentPlatform);
        }

        Set<LtiContextEntity> contextEntities = ltiContextEntityResourceService.getResources(LtiContextEntity.class);

        for (LtiContextEntity ltiContextEntity : contextEntities) {
            log.info("Storing " + ltiContextEntity.getContextId() + " : " + ltiContextEntity.getTitle());
            ltiContextRepository.save(ltiContextEntity);
        }

        Set<LtiUserEntity> users = ltiUserEntityResourceService.getResources(LtiUserEntity.class);

        for (LtiUserEntity user : users) {
            ltiUserRepository.save(user);
        }

        Set<Experiment> experiments = experimentResourceService.getResources(Experiment.class);

        for (Experiment experiment : experiments) {
            log.info("Storing: " + experiment.getExperimentId() + " : " + experiment.getTitle());
            experimentRepository.save(experiment);
        }

        experimentRepository.flush();

        Set<Conditions> conditions = conditionResourceService.getResources(Conditions.class);

        for (Conditions conditionList : conditions) {
            for (Condition condition : conditionList.getConditions()) {
                log.info("Storing : " + condition.getName());
                conditionRepository.saveAndFlush(condition);
            }
        }

        Set<Exposure> exposures = exposureResourceService.getResources(Exposure.class);

        for (Exposure exposure : exposures) {
            log.info("Storing : " + exposure.getTitle());
            exposureRepository.save(exposure);
        }
    }

}
