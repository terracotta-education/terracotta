package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.Terracotta;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.ToolDeploymentRepository;
import edu.iu.terracotta.connectors.generic.service.api.impl.ApiClientImpl;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.QuestionMc;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.ExposureRepository;
import edu.iu.terracotta.dao.repository.GroupRepository;
import edu.iu.terracotta.dao.repository.QuestionMcRepository;
import edu.iu.terracotta.dao.repository.QuestionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentTreatmentService;

/**
 * Reproduces (and, via {@link #duplicateTreatmentAgainstAlreadyCommittedDataDoesNotThrowNullIdentifierAssertionFailure()},
 * definitively finds the root cause of) the "null identifier (Assessment)" {@code
 * AssertionFailure} reported in production when duplicating an Assignment/Treatment, by
 * exercising the REAL {@link AssessmentServiceImpl}/{@link QuestionServiceImpl}/
 * {@link AssignmentTreatmentServiceImpl} beans and REAL Hibernate/H2 - no Mockito mocks for any
 * of the DB-relevant beans - so identifier-generation/flush/proxy timing is actually observed
 * rather than assumed.
 *
 * <p>This boots the full Spring context against an isolated in-memory H2 database with
 * {@code ddl-auto=create-drop} (schema generated from JPA annotations, matching the pattern in
 * {@code ParticipantRepositoryLmsSummaryQueryTest}), seeds the minimal valid entity graph the
 * duplication chain needs (PlatformDeployment -> ToolDeployment -> LtiContextEntity ->
 * Experiment -> Condition/Exposure -> Assignment -> Treatment -> Assessment -> Question/
 * QuestionMc), and exercises the real duplication chain at increasing scope: a single
 * Assessment/Question duplication, multiple in one transaction, the full {@code
 * duplicateTreatment} entry point, and finally (the key test) that same entry point against
 * data seeded in an ALREADY-COMMITTED, separate transaction - the realistic production shape,
 * and the only one of these that actually reproduces the reported crash. See that test's
 * Javadoc for the full root-cause writeup.</p>
 */
@SpringBootTest(
    classes = Terracotta.class,
    properties = {
        "aws.enabled=false",
        // isolated in-memory H2 instance, overriding any ambient/profile-based datasource
        "spring.datasource.url=jdbc:h2:mem:assessment-duplication-it;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@ActiveProfiles("test")
class AssessmentDuplicationRealHibernateIT {

    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private ToolDeploymentRepository toolDeploymentRepository;
    @Autowired private LtiContextRepository ltiContextRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ConditionRepository conditionRepository;
    @Autowired private ExposureRepository exposureRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private QuestionMcRepository questionMcRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private ExposureGroupConditionRepository exposureGroupConditionRepository;

    @Autowired private AssessmentService assessmentService;
    @Autowired private AssignmentTreatmentService assignmentTreatmentService;

    // AssignmentTreatmentServiceImpl.duplicateTreatment calls apiClient.listAssignment(...) right
    // after saving the new Treatment - a real LMS network call that is out of scope for this
    // DB/Hibernate timing question, so that one call is stubbed out. A @MockitoSpyBean (not
    // @MockitoBean) on the concrete, real @Primary ApiClientImpl router bean is used - rather
    // than replacing the ApiClient interface bean outright - because other beans (e.g.
    // AdminServiceImpl) are wired to the CONCRETE ApiClientImpl type, which a plain interface
    // mock is not assignable to. Everything else in the call chain (repositories, the
    // Assessment/Question services, Hibernate, H2) is real.
    @MockitoSpyBean private ApiClientImpl apiClientImpl;

    /**
     * Builds: PlatformDeployment -> ToolDeployment -> LtiContextEntity -> Experiment ->
     * Condition + Exposure -> Assignment -> two Treatments (source + target) -> a source
     * Assessment (with one QuestionMc) under the source Treatment.
     *
     * <p>Then directly calls the real {@code AssessmentServiceImpl.duplicateAssessment(long,
     * Treatment, Assignment)} to duplicate the source Assessment onto the target Treatment,
     * which internally calls the real {@code QuestionServiceImpl.duplicateQuestionsForAssessment}
     * to duplicate the QuestionMc onto the newly-duplicated Assessment - the exact hand-off
     * chain the bug report is about.</p>
     */
    @Test
    @Transactional
    void duplicateAssessmentDoesNotThrowNullIdentifierAssertionFailure() throws Exception {
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

        Condition condition = conditionRepository.saveAndFlush(
            Condition.builder()
                .experiment(experiment)
                .name("Condition 1")
                .build()
        );

        Exposure exposure = exposureRepository.saveAndFlush(
            Exposure.builder()
                .experiment(experiment)
                .title("Exposure 1")
                .build()
        );

        Assignment assignment = assignmentRepository.saveAndFlush(
            Assignment.builder()
                .exposure(exposure)
                .title("Assignment 1")
                .build()
        );

        Treatment sourceTreatment = treatmentRepository.saveAndFlush(
            Treatment.builder()
                .condition(condition)
                .assignment(assignment)
                .build()
        );

        Treatment targetTreatment = treatmentRepository.saveAndFlush(
            Treatment.builder()
                .condition(condition)
                .assignment(assignment)
                .build()
        );

        Assessment sourceAssessment = assessmentRepository.saveAndFlush(
            Assessment.builder()
                .treatment(sourceTreatment)
                .title("Assessment 1")
                .build()
        );

        QuestionMc questionMc = new QuestionMc();
        questionMc.setAssessment(sourceAssessment);
        questionMc.setQuestionType(QuestionTypes.MC);
        questionMc.setRandomizeAnswers(false);
        questionMc = questionMcRepository.saveAndFlush(questionMc);

        assertNotNull(sourceAssessment.getAssessmentId());
        assertNotNull(questionMc.getQuestionId());

        // This is the exact call chain from the bug report:
        // AssessmentServiceImpl.duplicateAssessment -> assessmentRepository.saveAndFlush(from)
        // -> questionService.duplicateQuestionsForAssessment(oldAssessmentId, newAssessment)
        // -> questionRepository.saveAndFlush(originalQuestion), which references newAssessment
        // as its FK. If the reported AssertionFailure is real given this app's actual
        // Hibernate/Spring wiring, it throws here.
        Assessment newAssessment = assertDoesNotThrow(
            () -> assessmentService.duplicateAssessment(sourceAssessment.getAssessmentId(), targetTreatment, assignment),
            "duplicateAssessment threw - see cause for the real root cause"
        );

        assertNotNull(newAssessment.getAssessmentId());
        assertEquals(targetTreatment.getTreatmentId(), newAssessment.getTreatment().getTreatmentId());

        var duplicatedQuestions = questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(newAssessment.getAssessmentId());
        assertEquals(1, duplicatedQuestions.size());

        Question duplicatedQuestion = duplicatedQuestions.get(0);
        assertNotNull(duplicatedQuestion.getQuestionId());
        assertNotNull(duplicatedQuestion.getAssessment());
        assertEquals(newAssessment.getAssessmentId(), duplicatedQuestion.getAssessment().getAssessmentId());
    }

    /**
     * The real {@code AssignmentServiceImpl.duplicateAssignment} loops over EVERY Treatment on
     * the source Assignment (one per Condition) in a single transaction, calling
     * {@code AssignmentTreatmentServiceImpl.duplicateTreatment} - which calls
     * {@code AssessmentServiceImpl.duplicateAssessment} - for each one in turn, all sharing one
     * Hibernate Session/persistence context. This repeats the exact
     * saveAndFlush(Assessment)-then-saveAndFlush(Question) hand-off multiple times in the same
     * transaction rather than once in isolation, which is what the single-assessment test above
     * covers.
     *
     * <p>This test reproduces that multi-iteration shape directly at the Assessment/Question
     * layer (two independent source Treatment/Assessment/Question trees under two Conditions of
     * the same Experiment, two target Treatments under a second "new" Assignment) - bypassing
     * AssignmentTreatmentServiceImpl/AssignmentServiceImpl only because those layers additionally
     * call the real LMS ApiClient (a live network dependency out of scope for this DB/Hibernate
     * timing question) - to check whether having a SECOND Assessment+Question insert/flush cycle
     * pending in the same Session changes identifier-materialization behavior for Hibernate's
     * IDENTITY generator/JDBC batching in a way the single-iteration test can't reveal.</p>
     */
    @Test
    @Transactional
    void duplicateAssessmentAcrossMultipleConditionsInSameTransactionDoesNotThrow() throws Exception {
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

        Condition condition1 = conditionRepository.saveAndFlush(Condition.builder().experiment(experiment).name("Condition 1").build());
        Condition condition2 = conditionRepository.saveAndFlush(Condition.builder().experiment(experiment).name("Condition 2").build());

        Exposure exposure = exposureRepository.saveAndFlush(Exposure.builder().experiment(experiment).title("Exposure 1").build());

        Assignment sourceAssignment = assignmentRepository.saveAndFlush(Assignment.builder().exposure(exposure).title("Assignment 1").build());
        Assignment targetAssignment = assignmentRepository.saveAndFlush(Assignment.builder().exposure(exposure).title("Copy of Assignment 1").build());

        Treatment sourceTreatment1 = treatmentRepository.saveAndFlush(Treatment.builder().condition(condition1).assignment(sourceAssignment).build());
        Treatment sourceTreatment2 = treatmentRepository.saveAndFlush(Treatment.builder().condition(condition2).assignment(sourceAssignment).build());
        Treatment targetTreatment1 = treatmentRepository.saveAndFlush(Treatment.builder().condition(condition1).assignment(targetAssignment).build());
        Treatment targetTreatment2 = treatmentRepository.saveAndFlush(Treatment.builder().condition(condition2).assignment(targetAssignment).build());

        Assessment sourceAssessment1 = assessmentRepository.saveAndFlush(Assessment.builder().treatment(sourceTreatment1).title("Assessment 1").build());
        Assessment sourceAssessment2 = assessmentRepository.saveAndFlush(Assessment.builder().treatment(sourceTreatment2).title("Assessment 2").build());

        for (Assessment sourceAssessment : new Assessment[] {sourceAssessment1, sourceAssessment2}) {
            QuestionMc questionMc = new QuestionMc();
            questionMc.setAssessment(sourceAssessment);
            questionMc.setQuestionType(QuestionTypes.MC);
            questionMc.setRandomizeAnswers(false);
            questionMcRepository.saveAndFlush(questionMc);
        }

        // exact shape of AssignmentServiceImpl.duplicateAssignment's loop over
        // treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc,
        // condensed to the Assessment/Question layer it actually delegates to.
        Assessment newAssessment1 = assertDoesNotThrow(
            () -> assessmentService.duplicateAssessment(sourceAssessment1.getAssessmentId(), targetTreatment1, targetAssignment),
            "duplicateAssessment threw on the FIRST condition's assessment"
        );
        Assessment newAssessment2 = assertDoesNotThrow(
            () -> assessmentService.duplicateAssessment(sourceAssessment2.getAssessmentId(), targetTreatment2, targetAssignment),
            "duplicateAssessment threw on the SECOND condition's assessment, sharing a Session/transaction with the first duplication"
        );

        assertNotNull(newAssessment1.getAssessmentId());
        assertNotNull(newAssessment2.getAssessmentId());

        assertEquals(1, questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(newAssessment1.getAssessmentId()).size());
        assertEquals(1, questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(newAssessment2.getAssessmentId()).size());
    }

    /**
     * Exercises the REAL {@code AssignmentTreatmentServiceImpl.duplicateTreatment} entry point -
     * one layer up from the two tests above - which is where the bug was originally reported
     * ("This previously surfaced (and was 'fixed') for Treatment when duplicating an
     * Assessment"). Unlike {@code AssessmentServiceImpl.duplicateAssessment}, this method also
     * touches the OTHER, independently-owned FK between Treatment and Assessment:
     * {@code Treatment.assessment} (column {@code terr_treatment.assessment_assessment_id}) is
     * its own {@code @OneToOne @JoinColumn} - NOT {@code mappedBy} the {@code Assessment.treatment}
     * side (column {@code terr_assessment.treatment_treatment_id}) - so Treatment and Assessment
     * each independently own a real FK column pointing at the other, a circular pair of
     * physical foreign keys. duplicateTreatment resolves this by inserting the Treatment first
     * (with assessment left null), then the Assessment (referencing the now-known Treatment id),
     * then updating the Treatment's assessment FK - but that extra update, and the extra
     * saveAndFlush it runs through, is a real difference in Hibernate flush-queue shape versus
     * the two tests above and is worth exercising directly rather than assuming it is
     * equivalent.
     *
     * <p>{@code listAssignment} on the real {@link ApiClientImpl} bean is stubbed (see the
     * {@code apiClientImpl} field) purely to avoid a real LMS network call from
     * {@code setAssignmentDtoAttrs}; every DB/Hibernate-relevant bean and call is real.</p>
     */
    @Test
    @Transactional
    void duplicateTreatmentDoesNotThrowNullIdentifierAssertionFailure() throws Exception {
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
        Treatment sourceTreatment = treatmentRepository.saveAndFlush(Treatment.builder().condition(condition).assignment(assignment).build());
        Assessment sourceAssessment = assessmentRepository.saveAndFlush(Assessment.builder().treatment(sourceTreatment).title("Assessment 1").build());

        QuestionMc questionMc = new QuestionMc();
        questionMc.setAssessment(sourceAssessment);
        questionMc.setQuestionType(QuestionTypes.MC);
        questionMc.setRandomizeAnswers(false);
        questionMcRepository.saveAndFlush(questionMc);

        // link sourceTreatment -> sourceAssessment on the Treatment-owned FK too, so the source
        // graph looks like a real, fully-wired Treatment/Assessment pair
        sourceTreatment.setAssessment(sourceAssessment);
        treatmentRepository.saveAndFlush(sourceTreatment);

        // AssessmentServiceImpl.toDto (called at the end of duplicateTreatment) requires every
        // Condition/Exposure pair to resolve to a Group via ExposureGroupCondition, or it throws
        // "Error 124: Assessment [id] is without a group" - an unrelated business-rule check, not
        // part of the AssertionFailure this test targets, but required to seed a valid enough
        // graph to reach the end of duplicateTreatment.
        Group group = groupRepository.saveAndFlush(Group.builder().experiment(experiment).name("Group 1").build());
        exposureGroupConditionRepository.saveAndFlush(
            ExposureGroupCondition.builder()
                .condition(condition)
                .exposure(exposure)
                .group(group)
                .build()
        );

        LtiUserEntity instructor = ltiUserRepository.saveAndFlush(
            LtiUserEntity.builder()
                .userKey("instructor-1")
                .platformDeployment(platformDeployment)
                .build()
        );

        doReturn(Optional.empty()).when(apiClientImpl).listAssignment(any(), any(), any(Assignment.class));

        SecuredInfo securedInfo = SecuredInfo.builder()
            .platformDeploymentId(platformDeployment.getKeyId())
            .userId(instructor.getUserKey())
            .lmsCourseId("course-1")
            .build();

        // This is the real entry point one level above AssessmentServiceImpl.duplicateAssessment:
        // AssignmentTreatmentServiceImpl.duplicateTreatment -> treatmentRepository.saveAndFlush
        // -> assessmentService.duplicateAssessment -> questionService.duplicateQuestionsForAssessment
        // -> questionRepository.saveAndFlush, plus the extra Treatment.assessment FK link/update
        // at the end. If the reported AssertionFailure is real given this app's actual
        // Hibernate/Spring wiring, it throws here.
        assertDoesNotThrow(
            () -> assignmentTreatmentService.duplicateTreatment(sourceTreatment.getTreatmentId(), assignment, securedInfo),
            "duplicateTreatment threw - see cause for the real root cause"
        );
    }

    /**
     * THE key reproduction in this suite: this is the only test that seeds its data in
     * SEPARATE, already-committed transactions BEFORE calling the real duplication chain in a
     * fresh transaction of its own - exactly how this data actually comes to exist in
     * production (created in an earlier request, long committed, by the time someone clicks
     * "duplicate"). Tests 1-3 above seed and duplicate within one long-lived test-level
     * transaction, which is unrealistic and - as this test proves - hides the bug entirely.
     *
     * <p>Root cause found via this test (with debug instrumentation, since removed): {@code
     * AssessmentServiceImpl.getAssessment}/{@code AssignmentTreatmentServiceImpl}'s {@code
     * treatmentRepository.findByTreatmentId} query by the entity's {@code @Id} property. Against
     * a FRESH persistence context that has never touched that row before (i.e. real production
     * conditions), Spring Data JPA/Hibernate 7.4.5 resolves that as an uninitialized
     * reference/proxy rather than eagerly loading the full entity - confirmed by instrumenting
     * the code: the "Assessment" object flowing all the way through to the crash was a {@code
     * Assessment$HibernateProxy}, not a plain {@code Assessment}. {@code
     * AssessmentServiceImpl.duplicateAssessment} (and the analogous code in {@code
     * AssignmentTreatmentServiceImpl.duplicateTreatment}) then calls {@code
     * entityManager.detach(from)} and mutates fields directly on that reference - including
     * resetting its OWN identifier to null and re-persisting it as a new row. Mutating a
     * still-proxied reference this way corrupts the proxy's internal identity bookkeeping: it
     * keeps its ORIGINAL loaded identifier (or none) internally even after Hibernate assigns the
     * new IDENTITY-generated id, which is invisible as long as nothing needs that internal proxy
     * state - but the moment this same object is used as the FK on a dependent Question's insert
     * (in {@code QuestionServiceImpl.duplicateQuestionsForAssessment}), Hibernate's
     * transient-reference check falls back to that stale internal identifier and throws exactly
     * the reported {@code AssertionFailure: null identifier (Assessment)}. saveAndFlush (the
     * previously-applied fix) cannot help here - the entity's real, materialized id was never
     * the problem; the corrupted PROXY was.</p>
     *
     * <p>The fix: {@link org.hibernate.Hibernate#unproxy(Object)} the loaded entity BEFORE
     * detaching/mutating it, in both {@code AssessmentServiceImpl.duplicateAssessment} and
     * {@code AssignmentTreatmentServiceImpl.duplicateTreatment}, guaranteeing a real, plain
     * instance is what gets detached, mutated, and re-persisted - never a proxy.</p>
     *
     * <p>Separately (found and fixed along the way, but not itself the cause of the
     * AssertionFailure): {@code AssignmentTreatmentServiceImpl.duplicateTreatment} had no
     * {@code @Transactional} of its own, and one of its two real callers -
     * {@code TreatmentController.duplicateTreatment}, the "duplicate a single Treatment"
     * endpoint - does not wrap it in one either (unlike {@code AssignmentController
     * .duplicateAssignment}, which is {@code @Transactional}). That gap is a genuine atomicity
     * bug (a failure partway through would leave earlier writes committed) independent of the
     * proxy issue, now closed by adding {@code @Transactional} to both {@code duplicateTreatment}
     * overloads - safe/additive for the already-{@code @Transactional} caller (REQUIRED
     * propagation just joins the existing transaction there).</p>
     */
    @Test
    void duplicateTreatmentAgainstAlreadyCommittedDataDoesNotThrowNullIdentifierAssertionFailure() throws Exception {
        PlatformDeployment platformDeployment = platformDeploymentRepository.saveAndFlush(
            PlatformDeployment.builder()
                .iss("https://issuer2.example.org")
                .clientId("client-id-2")
                .oidcEndpoint("https://issuer2.example.org/oidc")
                .enableAutomaticDeployments(false)
                .lmsConnector(LmsConnector.CANVAS)
                .build()
        );

        ToolDeployment toolDeployment = toolDeploymentRepository.saveAndFlush(
            ToolDeployment.builder()
                .ltiDeploymentId("deployment-2")
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
        Treatment sourceTreatment = treatmentRepository.saveAndFlush(Treatment.builder().condition(condition).assignment(assignment).build());
        Assessment sourceAssessment = assessmentRepository.saveAndFlush(Assessment.builder().treatment(sourceTreatment).title("Assessment 1").build());

        QuestionMc questionMc = new QuestionMc();
        questionMc.setAssessment(sourceAssessment);
        questionMc.setQuestionType(QuestionTypes.MC);
        questionMc.setRandomizeAnswers(false);
        questionMcRepository.saveAndFlush(questionMc);

        sourceTreatment.setAssessment(sourceAssessment);
        treatmentRepository.saveAndFlush(sourceTreatment);

        Group group = groupRepository.saveAndFlush(Group.builder().experiment(experiment).name("Group 1").build());
        exposureGroupConditionRepository.saveAndFlush(
            ExposureGroupCondition.builder()
                .condition(condition)
                .exposure(exposure)
                .group(group)
                .build()
        );

        LtiUserEntity instructor = ltiUserRepository.saveAndFlush(
            LtiUserEntity.builder()
                .userKey("instructor-2")
                .platformDeployment(platformDeployment)
                .build()
        );

        doReturn(Optional.empty()).when(apiClientImpl).listAssignment(any(), any(), any(Assignment.class));

        SecuredInfo securedInfo = SecuredInfo.builder()
            .platformDeploymentId(platformDeployment.getKeyId())
            .userId(instructor.getUserKey())
            .lmsCourseId("course-1")
            .build();

        assertDoesNotThrow(
            () -> assignmentTreatmentService.duplicateTreatment(sourceTreatment.getTreatmentId(), assignment, securedInfo),
            "duplicateTreatment threw against already-committed data - see cause for the real root cause"
        );
    }

}
