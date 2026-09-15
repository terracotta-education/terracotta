package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.entity.QuestionSubmissionComment;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.SubmissionComment;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.dao.repository.QuestionRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionCommentRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.dao.repository.SubmissionCommentRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.AssessmentService;

/**
 * Verifies (or disproves) an audit finding about {@code Assessment.questions}/
 * {@code Assessment.submissions} and {@code Submission.questionSubmissions}/
 * {@code Submission.submissionComments}: all four are declared {@code orphanRemoval = true} with
 * NO explicit {@code cascade} attribute (see {@code Assessment.java}/{@code Submission.java}).
 * The question is whether deleting an Assessment - via
 * {@code AssessmentRepository.deleteByAssessmentId}, the exact mechanism
 * {@code AssessmentServiceImpl.deleteById} uses - actually propagates that implicit
 * orphanRemoval-cascade not just one level down (Assessment -&gt; Question, Assessment -&gt;
 * Submission) but a SECOND level (Submission -&gt; QuestionSubmission, Submission -&gt;
 * SubmissionComment) and a THIRD level (QuestionSubmission -&gt; QuestionSubmissionComment,
 * the next association down per the actual entity graph - {@code QuestionSubmission.java}
 * declares {@code questionSubmissionComments} with the same orphanRemoval-only pattern).
 *
 * <p>{@code AssessmentRepository.deleteByAssessmentId} is a derived ({@code deleteBy...}, no
 * {@code @Query}) delete method, which Spring Data JPA implements by loading the matching
 * entities and calling {@code EntityManager.remove(...)} on each - not a bulk JPQL/SQL
 * {@code DELETE} - so JPA cascade/orphanRemoval rules are actually in play here (a bulk
 * {@code @Modifying @Query("delete ...")} would bypass them entirely). The repository method
 * itself is {@code @Transactional}, so the whole cascade happens within ONE transaction/session
 * regardless of whether {@code AssessmentServiceImpl.deleteById} (which has no
 * {@code @Transactional} of its own) is called directly or through a real request.</p>
 *
 * <p>Data is seeded in SEPARATE, already-committed transactions (no test-level
 * {@code @Transactional}) - the realistic "created in an earlier request" production shape -
 * before the delete runs in a fresh call of its own.</p>
 */
@SpringBootTest(
    classes = Terracotta.class,
    properties = {
        "aws.enabled=false",
        // isolated in-memory H2 instance, overriding any ambient/profile-based datasource
        "spring.datasource.url=jdbc:h2:mem:assessment-deletion-cascade-it;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@ActiveProfiles("test")
class AssessmentDeletionCascadeRealHibernateTest {

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
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private QuestionSubmissionRepository questionSubmissionRepository;
    @Autowired private QuestionSubmissionCommentRepository questionSubmissionCommentRepository;
    @Autowired private SubmissionCommentRepository submissionCommentRepository;
    @Autowired private AssessmentService assessmentService;

    @Test
    void deletingAssessmentCascadesThroughEveryDescendantLevel() throws Exception {
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

        // level 1: Question
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

        // level 1: Submission
        Submission submission = submissionRepository.saveAndFlush(Submission.builder().participant(participant).assessment(assessment).build());

        // level 2: QuestionSubmission (child of Submission) and SubmissionComment (child of Submission)
        QuestionSubmission questionSubmission = questionSubmissionRepository.saveAndFlush(
            QuestionSubmission.builder()
                .submission(submission)
                .question(question)
                .build()
        );

        SubmissionComment submissionComment = submissionCommentRepository.saveAndFlush(
            SubmissionComment.builder()
                .submission(submission)
                .creator("instructor-1")
                .comment("A comment on the submission")
                .build()
        );

        // level 3: QuestionSubmissionComment (child of QuestionSubmission)
        QuestionSubmissionComment questionSubmissionComment = questionSubmissionCommentRepository.saveAndFlush(
            QuestionSubmissionComment.builder()
                .questionSubmission(questionSubmission)
                .creator("instructor-1")
                .comment("A comment on the question submission")
                .build()
        );

        Long assessmentId = assessment.getAssessmentId();
        Long questionId = question.getQuestionId();
        Long submissionId = submission.getSubmissionId();
        Long questionSubmissionId = questionSubmission.getQuestionSubmissionId();
        Long submissionCommentId = submissionComment.getSubmissionCommentId();
        Long questionSubmissionCommentId = questionSubmissionComment.getQuestionSubmissionCommentId();

        assertNotNull(assessmentId);
        assertNotNull(questionId);
        assertNotNull(submissionId);
        assertNotNull(questionSubmissionId);
        assertNotNull(submissionCommentId);
        assertNotNull(questionSubmissionCommentId);

        // the real production entry point
        assertDoesNotThrow(
            () -> assessmentService.deleteById(assessmentId),
            "assessmentService.deleteById threw - see cause for whether this is an FK-constraint/nullability failure from an incomplete or misordered cascade"
        );

        // fresh reads (new, unrelated transactions/sessions) of actual DB state
        assertNull(assessmentRepository.findByAssessmentId(assessmentId), "Assessment itself should be gone");
        assertNull(questionRepository.findByQuestionId(questionId), "level 1: Question should have been cascade-deleted with the Assessment");
        assertNull(submissionRepository.findBySubmissionId(submissionId), "level 1: Submission should have been cascade-deleted with the Assessment");
        assertNull(
            questionSubmissionRepository.findByQuestionSubmissionId(questionSubmissionId),
            "level 2: QuestionSubmission should have been cascade-deleted with its Submission"
        );
        assertNull(
            submissionCommentRepository.findBySubmissionCommentId(submissionCommentId),
            "level 2: SubmissionComment should have been cascade-deleted with its Submission"
        );
        assertNull(
            questionSubmissionCommentRepository.findByQuestionSubmissionCommentId(questionSubmissionCommentId),
            "level 3: QuestionSubmissionComment should have been cascade-deleted with its QuestionSubmission"
        );
    }

}
