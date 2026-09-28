package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import edu.iu.Terracotta;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.ToolDeploymentRepository;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Two students opening a brand-new assignment at the same moment: each launch loads the
 * assignment (and its experiment) before either has marked it started. Each launch here gets its
 * own Hibernate session, bound to the thread the way open-in-view binds one per request, and the
 * two are interleaved: both load, then the first finishes, then the second carries on with the
 * copy it loaded before the first one's change.
 */
@SpringBootTest(
    classes = Terracotta.class,
    properties = {
        "aws.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:first-launch-started-race-it;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@ActiveProfiles("test")
class FirstLaunchStartedRaceRealHibernateTest {

    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private ToolDeploymentRepository toolDeploymentRepository;
    @Autowired private LtiContextRepository ltiContextRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ExposureRepository exposureRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PlatformTransactionManager transactionManager;

    private long experimentId;
    private long assignmentId;

    @BeforeEach
    void seed() {
        PlatformDeployment platformDeployment = platformDeploymentRepository.saveAndFlush(
            PlatformDeployment.builder()
                .iss("https://issuer.example.org")
                .clientId(UUID.randomUUID().toString())
                .oidcEndpoint("https://issuer.example.org/oidc")
                .enableAutomaticDeployments(false)
                .lmsConnector(LmsConnector.CANVAS)
                .build()
        );
        ToolDeployment toolDeployment = toolDeploymentRepository.saveAndFlush(
            ToolDeployment.builder().ltiDeploymentId(UUID.randomUUID().toString()).platformDeployment(platformDeployment).build()
        );
        LtiContextEntity ltiContextEntity = ltiContextRepository.saveAndFlush(
            LtiContextEntity.builder().contextKey(UUID.randomUUID().toString()).toolDeployment(toolDeployment).build()
        );
        Experiment experiment = experimentRepository.saveAndFlush(
            Experiment.builder().platformDeployment(platformDeployment).ltiContextEntity(ltiContextEntity).title("Experiment").build()
        );
        Exposure exposure = exposureRepository.saveAndFlush(Exposure.builder().experiment(experiment).title("Exposure 1").build());
        Assignment assignment = assignmentRepository.saveAndFlush(Assignment.builder().exposure(exposure).title("Assignment 1").build());

        experimentId = experiment.getExperimentId();
        assignmentId = assignment.getAssignmentId();
    }

    // what launching did before: load, set started, save - the second launch's save fails its
    // version check with the "Unexpected row count (expected row count 1 but was 0)" seen in the logs
    @Test
    void loadModifySaveFailsTheLaterOfTwoOverlappingFirstLaunches() {
        Launch first = new Launch();
        Launch second = new Launch();
        Assignment firstCopy = first.run(() -> assignmentRepository.findById(assignmentId).orElseThrow());
        Assignment secondCopy = second.run(() -> assignmentRepository.findById(assignmentId).orElseThrow());

        first.run(() -> {
            firstCopy.setStarted(Timestamp.valueOf(LocalDateTime.now()));
            return assignmentRepository.saveAndFlush(firstCopy);
        });

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> second.run(() -> {
            secondCopy.setStarted(Timestamp.valueOf(LocalDateTime.now()));
            return assignmentRepository.saveAndFlush(secondCopy);
        }));

        first.close();
        second.close();
    }

    @Test
    void markStartedLetsBothOverlappingFirstLaunchesSucceed() {
        Launch first = new Launch();
        Launch second = new Launch();
        Assignment firstCopy = first.run(() -> assignmentRepository.findById(assignmentId).orElseThrow());
        Assignment secondCopy = second.run(() -> assignmentRepository.findById(assignmentId).orElseThrow());
        int versionBefore = secondCopy.getVersion();
        assertNull(secondCopy.getStarted());

        Timestamp startedByFirst = Timestamp.valueOf(LocalDateTime.now().withNano(0));
        assertEquals(1, first.run(() -> {
            int updated = assignmentRepository.markStarted(assignmentId, startedByFirst);
            first.entityManager.refresh(firstCopy);
            return updated;
        }));

        // the second launch still holds its pre-start copy - it starts nothing, fails nothing,
        // and picks up the first launch's start time and version
        assertEquals(0, second.run(() -> {
            int updated = assignmentRepository.markStarted(assignmentId, Timestamp.valueOf(LocalDateTime.now()));
            second.entityManager.refresh(secondCopy);
            return updated;
        }));
        assertEquals(startedByFirst, secondCopy.getStarted());
        assertEquals(versionBefore + 1, secondCopy.getVersion());

        // and anything later in that same launch that saves the assignment doesn't conflict
        second.run(() -> {
            secondCopy.setTitle("Assignment 1 (renamed)");
            return assignmentRepository.saveAndFlush(secondCopy);
        });

        first.close();
        second.close();
    }

    @Test
    void markStartedLetsBothOverlappingFirstLaunchesStartTheExperiment() {
        Launch first = new Launch();
        Launch second = new Launch();
        Experiment firstCopy = first.run(() -> experimentRepository.findById(experimentId).orElseThrow());
        Experiment secondCopy = second.run(() -> experimentRepository.findById(experimentId).orElseThrow());

        assertEquals(1, first.run(() -> {
            int updated = experimentRepository.markStarted(experimentId, Timestamp.valueOf(LocalDateTime.now()));
            first.entityManager.refresh(firstCopy);
            return updated;
        }));
        assertEquals(0, second.run(() -> {
            int updated = experimentRepository.markStarted(experimentId, Timestamp.valueOf(LocalDateTime.now()));
            second.entityManager.refresh(secondCopy);
            return updated;
        }));

        assertNotNull(secondCopy.getStarted());
        second.run(() -> experimentRepository.saveAndFlush(secondCopy));

        first.close();
        second.close();
    }

    // one student's launch: its own session, kept open across its transactions the way
    // open-in-view keeps one open for a whole request, and bound to the thread only while it runs
    private class Launch {

        private final EntityManager entityManager = entityManagerFactory.createEntityManager();

        <T> T run(Supplier<T> work) {
            TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManager));

            try {
                return new TransactionTemplate(transactionManager).execute(status -> work.get());
            } finally {
                TransactionSynchronizationManager.unbindResource(entityManagerFactory);
            }
        }

        void close() {
            entityManager.close();
        }

    }

}
