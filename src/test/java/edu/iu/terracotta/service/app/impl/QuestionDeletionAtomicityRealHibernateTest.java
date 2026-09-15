package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import edu.iu.Terracotta;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiMembershipRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.ToolDeploymentRepository;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.dao.repository.QuestionRepository;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.QuestionService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

/**
 * Verifies (or disproves) an audit finding that {@link QuestionServiceImpl#deleteById(Long)} is
 * not atomic: it has no {@code @Transactional} anywhere in its call chain (confirmed by reading
 * both {@code QuestionServiceImpl.java} and {@code QuestionController.java}), and performs
 * multiple, separate Spring Data repository write calls (each of which is independently
 * {@code @Transactional} - see {@link org.springframework.data.jpa.repository.support.SimpleJpaRepository})
 * while deleting every {@link Submission} on the question's {@link Assessment}. If one of those
 * writes throws partway through, everything committed before the throw stays committed - a
 * partial-commit bug - while the {@link Question} row itself, deleted last, never gets removed.
 *
 * <p>Real production shape: {@code application.yml} sets {@code spring.jpa.open-in-view: true},
 * so in a real HTTP request through {@code QuestionController}, Spring's
 * {@code OpenEntityManagerInViewInterceptor} binds ONE Hibernate {@code EntityManager}/Session to
 * the request thread for its whole duration, while each individual repository call still opens
 * and commits its OWN transaction against that shared, long-lived session (lazy collections stay
 * loadable across calls, but writes are NOT unified into one atomic transaction). A plain
 * {@code @SpringBootTest} method that just calls the service directly does NOT go through that
 * web-request machinery, so - to reproduce the real shape rather than an artifact of a
 * non-representative test harness - this test manually binds an {@code EntityManager} to the
 * current thread exactly the way {@code OpenEntityManagerInViewInterceptor}/{@code Filter} do,
 * before invoking {@code questionService.deleteById(...)}, so Spring's {@code JpaTransactionManager}
 * finds and reuses it for each individual repository transaction instead of creating a fresh,
 * isolated one per call.</p>
 *
 * <p>Data is seeded in SEPARATE, already-committed transactions (no test-level
 * {@code @Transactional}) - the realistic "created in an earlier request" production shape - and
 * the operation under test runs in a fresh transaction/session of its own.</p>
 */
@SpringBootTest(
    classes = Terracotta.class,
    properties = {
        "aws.enabled=false",
        // isolated in-memory H2 instance, overriding any ambient/profile-based datasource
        "spring.datasource.url=jdbc:h2:mem:question-deletion-atomicity-it;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@ActiveProfiles("test")
class QuestionDeletionAtomicityRealHibernateTest {

    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private ToolDeploymentRepository toolDeploymentRepository;
    @Autowired private LtiContextRepository ltiContextRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private LtiMembershipRepository ltiMembershipRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ConditionRepository conditionRepository;
    @Autowired private ExposureRepository exposureRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PlatformTransactionManager transactionManager;
    @PersistenceContext private EntityManager entityManager;

    @Autowired private QuestionService questionService;

    // Real bean, spied - not replaced - so every non-stubbed call (findByAssessment_AssessmentId,
    // flush, etc.) still goes through the real SimpleJpaRepository/Hibernate/H2 machinery. Only
    // the SECOND invocation of delete(Submission) is intercepted, to simulate a failure partway
    // through QuestionServiceImpl.deleteById's submission-cleanup loop.
    @MockitoSpyBean private SubmissionRepository submissionRepository;

    @Test
    void partialFailureDuringSubmissionCleanupDoesNotLeaveAPartialCommit() throws Exception {
        PlatformDeployment platformDeployment = platformDeploymentRepository.saveAndFlush(
            PlatformDeployment.builder()
                .iss("https://issuer.example.org")
                .clientId("client-id")
                .oidcEndpoint("https://issuer.example.org/oidc")
                .enableAutomaticDeployments(false)
                .lmsConnector(LmsConnector.CANVAS)
                .build()
        );

        ToolDeployment toolDeployment = toolDeploymentRepository.saveAndFlush(
            ToolDeployment.builder()
                .ltiDeploymentId("deployment-1")
                .platformDeployment(platformDeployment)
                .build()
        );

        LtiContextEntity ltiContextEntity = ltiContextRepository.saveAndFlush(
            LtiContextEntity.builder()
                .contextKey(UUID.randomUUID().toString())
                .toolDeployment(toolDeployment)
                .build()
        );

        Experiment experiment = experimentRepository.saveAndFlush(
            Experiment.builder()
                .platformDeployment(platformDeployment)
                .ltiContextEntity(ltiContextEntity)
                .title("Experiment")
                .build()
        );

        Condition condition = conditionRepository.saveAndFlush(Condition.builder().experiment(experiment).name("Condition 1").build());
        Exposure exposure = exposureRepository.saveAndFlush(Exposure.builder().experiment(experiment).title("Exposure 1").build());
        Assignment assignment = assignmentRepository.saveAndFlush(Assignment.builder().exposure(exposure).title("Assignment 1").build());
        Treatment treatment = treatmentRepository.saveAndFlush(Treatment.builder().condition(condition).assignment(assignment).build());
        Assessment assessment = assessmentRepository.saveAndFlush(Assessment.builder().treatment(treatment).title("Assessment 1").build());

        Question question = questionRepository.saveAndFlush(
            Question.builder()
                .assessment(assessment)
                .questionType(QuestionTypes.ESSAY)
                .build()
        );

        LtiUserEntity ltiUserEntity = ltiUserRepository.saveAndFlush(
            LtiUserEntity.builder()
                .userKey("student-1")
                .platformDeployment(platformDeployment)
                .build()
        );

        LtiMembershipEntity ltiMembershipEntity = ltiMembershipRepository.saveAndFlush(
            LtiMembershipEntity.builder()
                .context(ltiContextEntity)
                .user(ltiUserEntity)
                .role(0)
                .build()
        );

        Participant participant = participantRepository.saveAndFlush(
            Participant.builder()
                .experiment(experiment)
                .ltiUserEntity(ltiUserEntity)
                .ltiMembershipEntity(ltiMembershipEntity)
                .build()
        );

        // two submissions on the assessment - no QuestionSubmissions on either, so the ONLY
        // repository writes deleteById performs are the two submissionRepository.delete(...)
        // calls, the flush, and the final questionRepository.deleteByQuestionId(...).
        Submission submission1 = submissionRepository.saveAndFlush(Submission.builder().participant(participant).assessment(assessment).build());
        Submission submission2 = submissionRepository.saveAndFlush(Submission.builder().participant(participant).assessment(assessment).build());

        Long questionId = question.getQuestionId();
        Long assessmentId = assessment.getAssessmentId();
        Long submission1Id = submission1.getSubmissionId();
        Long submission2Id = submission2.getSubmissionId();

        assertNotNull(questionId);
        assertNotNull(submission1Id);
        assertNotNull(submission2Id);

        // Mockito cannot callRealMethod() through a spy of a Spring Data repository interface
        // proxy (it is a synthetic JDK proxy with no reachable concrete method body, so
        // invocation.callRealMethod() fails with "Cannot call abstract real method on java
        // object!"). Instead, the "allowed" (first) call reproduces exactly what
        // SimpleJpaRepository.delete(T) itself does - entityManager.remove(entity) wrapped in its
        // own self-contained transaction - via a manual TransactionTemplate against the same
        // thread-bound EntityManager the OSIV emulation below sets up.
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        AtomicInteger deleteCalls = new AtomicInteger();
        doAnswer(
            invocation -> {
                Submission target = invocation.getArgument(0);

                if (deleteCalls.incrementAndGet() == 2) {
                    throw new IllegalStateException("Simulated failure deleting the second submission");
                }

                transactionTemplate.executeWithoutResult(status -> entityManager.remove(target));
                return null;
            }
        ).when(submissionRepository).delete(any(Submission.class));

        // Manually reproduce the OSIV shape (spring.jpa.open-in-view: true) that a real HTTP
        // request through QuestionController goes through: one EntityManager/Session bound to the
        // thread for the whole operation, so lazy collections (Submission.questionSubmissions)
        // stay loadable across the several independently-committing repository calls inside
        // deleteById - exactly what OpenEntityManagerInViewInterceptor/Filter do for a real
        // request, and exactly what a bare direct call (no bound EntityManager at all) would NOT
        // reproduce faithfully.
        EntityManager viewEntityManager = entityManagerFactory.createEntityManager();
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(viewEntityManager));

        IllegalStateException thrown;

        try {
            thrown = assertThrows(IllegalStateException.class, () -> questionService.deleteById(questionId));
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
            EntityManagerFactoryUtils.closeEntityManager(viewEntityManager);
        }

        assertEquals("Simulated failure deleting the second submission", thrown.getMessage());

        // Fresh reads (new, unrelated transactions/sessions) of actual DB state - this is the
        // real question: did the FIRST submission's delete stay committed (a partial commit)
        // despite the overall operation failing, and was the Question itself left undeleted?
        List<Submission> remainingSubmissions = submissionRepository.findByAssessment_AssessmentId(assessmentId);
        Question remainingQuestion = questionRepository.findByQuestionId(questionId);

        assertNotNull(remainingQuestion, "Question should not have been deleted since the overall operation threw partway through");
        assertEquals(
            2,
            remainingSubmissions.size(),
            "Expected BOTH submissions to still exist (i.e. the whole operation rolled back atomically) - "
                + "if this fails with fewer than 2, the first submission's delete was left partially committed "
                + "despite the overall deleteById call throwing, confirming the atomicity bug"
        );
    }

}
