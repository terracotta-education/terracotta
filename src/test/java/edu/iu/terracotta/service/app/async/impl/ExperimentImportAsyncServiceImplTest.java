package edu.iu.terracotta.service.app.async.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.PlatformTransactionManager;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImportError;
import edu.iu.terracotta.dao.entity.integrations.IntegrationClient;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.model.distribute.export.AnswerMcExport;
import edu.iu.terracotta.dao.model.distribute.export.AssessmentExport;
import edu.iu.terracotta.dao.model.distribute.export.AssignmentExport;
import edu.iu.terracotta.dao.model.distribute.export.ConditionExport;
import edu.iu.terracotta.dao.model.distribute.export.ConsentDocumentExport;
import edu.iu.terracotta.dao.model.distribute.export.Export;
import edu.iu.terracotta.dao.model.distribute.export.ExperimentExport;
import edu.iu.terracotta.dao.model.distribute.export.ExposureExport;
import edu.iu.terracotta.dao.model.distribute.export.ExposureGroupConditionExport;
import edu.iu.terracotta.dao.model.distribute.export.GroupExport;
import edu.iu.terracotta.dao.model.distribute.export.IntegrationClientExport;
import edu.iu.terracotta.dao.model.distribute.export.IntegrationConfigurationExport;
import edu.iu.terracotta.dao.model.distribute.export.IntegrationExport;
import edu.iu.terracotta.dao.model.distribute.export.OutcomeExport;
import edu.iu.terracotta.dao.model.distribute.export.QuestionExport;
import edu.iu.terracotta.dao.model.distribute.export.TreatmentExport;
import edu.iu.terracotta.dao.model.enums.DistributionTypes;
import edu.iu.terracotta.dao.model.enums.ExposureTypes;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyNotificationService;
import tools.jackson.databind.json.JsonMapper;

class ExperimentImportAsyncServiceImplTest extends BaseTest {

    @Mock private ExperimentCopyNotificationService experimentCopyNotificationService;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks private ExperimentImportAsyncServiceImpl experimentImportAsyncServiceImpl;

    private Path importDirectory;
    private List<ExperimentImportError> errors;

    @BeforeEach
    void beforeEach() throws IOException {
        MockitoAnnotations.openMocks(this);
        setup();

        importDirectory = Files.createTempDirectory("experiment-import-test");
        when(fileStorageService.getExperimentImportFile(anyLong())).thenReturn(importDirectory.toFile());
        when(experimentImport.getOwner()).thenReturn(ltiUserEntity);
        when(experimentImport.getContext()).thenReturn(ltiContextEntity);

        // experimentImport is a mock; addErrorMessage() would otherwise be a no-op, so back it
        // with a real mutable list to faithfully reproduce the error-accumulation branching in process().
        errors = new ArrayList<>();
        when(experimentImport.getErrors()).thenReturn(errors);
        doAnswer(
            invocation -> {
                errors.add(mock(ExperimentImportError.class));
                return null;
            }
        ).when(experimentImport).addErrorMessage(anyString());

        when(experimentRepository.save(any(Experiment.class))).thenReturn(experiment);
        when(conditionRepository.save(any(Condition.class))).thenReturn(condition);
        when(exposureRepository.save(any(Exposure.class))).thenReturn(exposure);
        when(groupRepository.save(any(Group.class))).thenReturn(group);
        when(exposureGroupConditionRepository.save(any(ExposureGroupCondition.class))).thenReturn(exposureGroupCondition);
        when(consentDocumentRepository.save(any(ConsentDocument.class))).thenReturn(consentDocument);
        when(integrationClientRepository.save(any(IntegrationClient.class))).thenReturn(integrationClient);
    }

    private ExperimentExport experimentExport() {
        return ExperimentExport.builder()
            .id("100")
            .title("source experiment title")
            .description("description")
            .exposureType(ExposureTypes.BETWEEN)
            .participationType(ParticipationTypes.AUTO)
            .distributionType(DistributionTypes.EVEN)
            .build();
    }

    private Export fullExport() {
        return Export.builder()
            .experiment(experimentExport())
            .conditions(List.of(ConditionExport.builder().id("300").name("condition").defaultCondition(true).distributionPct(50F).experimentId("100").build()))
            .exposures(List.of(ExposureExport.builder().id("400").title("exposure").experimentId("100").build()))
            .groups(List.of(GroupExport.builder().id("500").name("group").experimentId("100").build()))
            .exposureGroupConditions(List.of(ExposureGroupConditionExport.builder().id("600").conditionId("300").exposureId("400").groupId("500").build()))
            .assignments(List.of(AssignmentExport.builder().id("700").title("assignment").exposureId("400").numOfSubmissions(1).build()))
            .treatments(List.of(TreatmentExport.builder().id("800").assignmentId("700").conditionId("300").build()))
            .assessments(List.of(AssessmentExport.builder().id("900").title("assessment").treatmentId("800").build()))
            .questions(
                List.of(
                    QuestionExport.builder().id("1000").html("mc question").questionType(QuestionTypes.MC).assessmentId("900").randomizeAnswers(true).build(),
                    QuestionExport.builder().id("1001").html("essay question").questionType(QuestionTypes.ESSAY).assessmentId("900").build()
                )
            )
            .integrationClients(List.of(IntegrationClientExport.builder().id("1100").name("integration client").enabled(true).build()))
            .integrationConfigurations(List.of(IntegrationConfigurationExport.builder().id("1200").clientId("1100").launchUrl("http://launch.url").build()))
            .integrations(List.of(IntegrationExport.builder().id("1300").configurationId("1200").questionId("1000").build()))
            .answersMc(List.of(AnswerMcExport.builder().id("1400").answerOrder(1).correct(true).html("answer").questionId("1000").build()))
            .outcomes(List.of(OutcomeExport.builder().id("1500").title("outcome").maxPoints(10F).exposureId("400").build()))
            .build();
    }

    private void writeExportJson(Export export) throws IOException {
        JsonMapper.builder().build().writeValue(importDirectory.resolve(ExperimentImport.JSON_FILE_NAME).toFile(), export);
    }

    @Test
    void testProcessNoImportDirectory() {
        when(fileStorageService.getExperimentImportFile(anyLong())).thenReturn(null);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false);

        // prepare() records the specific error, then process() itself records a second, generic one, and
        // the failure is then saved (a third) since nothing else persists it
        verify(experimentImport, times(3)).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage("No import .zip file found.");
        verify(experimentImport).addErrorMessage("No import file found.");
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void testProcessNoJsonFile() {
        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false);

        verify(experimentImport, times(3)).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(String.format("No JSON file [%s] found in imported .zip file.", ExperimentImport.JSON_FILE_NAME));
        verify(experimentImport).addErrorMessage("No import file found.");
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void testProcessMalformedJsonFile() throws IOException {
        Files.writeString(importDirectory.resolve(ExperimentImport.JSON_FILE_NAME), "not valid json");

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false);

        verify(experimentImport, times(3)).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(String.format("Error reading JSON file: [%s]", ExperimentImport.JSON_FILE_NAME));
        verify(experimentImport).addErrorMessage("No import file found.");
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void testProcessExistingValidationErrors() throws IOException {
        writeExportJson(fullExport());
        errors.add(mock(ExperimentImportError.class));

        // validation errors already present when process() is invoked; it records the error and
        // returns immediately, never reaching the final rollback check that throws the exception -
        // the ERROR status is then saved on its own, since nothing in the import did
        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false);

        verify(experimentImport, times(2)).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(org.mockito.ArgumentMatchers.startsWith("Validation errors:"));
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void testProcessSuccessNoAssignments() throws AssignmentNotCreatedException, TerracottaConnectorException {
        Export export = fullExport();
        export.setAssignments(Collections.emptyList());
        export.setTreatments(Collections.emptyList());
        export.setAssessments(Collections.emptyList());
        export.setQuestions(Collections.emptyList());
        export.setIntegrations(Collections.emptyList());
        export.setAnswersMc(Collections.emptyList());

        try {
            writeExportJson(export);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false);

        verify(experimentRepository).save(any(Experiment.class));
        verify(experimentImportRepository).save(experimentImport);
        verify(experimentImport).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.COMPLETE);
        verify(assignmentService, never()).createAssignmentInLms(any(), any(), anyLong(), anyString());
    }

    // an optimistic-locking failure on the final save must propagate rather than be retried
    // in place: process() is @Transactional, so once save() has thrown the transaction is
    // already rollback-only and any in-transaction re-fetch/re-save would be silently thrown
    // away at commit - surfacing it rolls the import back loudly instead. Preventing the
    // conflict in the first place is ExperimentImportServiceImpl.preprocess's job (it detaches
    // this entity from its request thread's session before handing it here).
    @Test
    void testProcessPropagatesOptimisticLockingFailureOnFinalSave() {
        Export export = fullExport();
        export.setAssignments(Collections.emptyList());
        export.setTreatments(Collections.emptyList());
        export.setAssessments(Collections.emptyList());
        export.setQuestions(Collections.emptyList());
        export.setIntegrations(Collections.emptyList());
        export.setAnswersMc(Collections.emptyList());

        try {
            writeExportJson(export);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        when(experimentImport.getId()).thenReturn(1L);
        when(experimentImportRepository.save(experimentImport))
            .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(ExperimentImport.class, 1L));

        assertThrows(
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false)
        );

        // no retry of the save inside the rolled-back transaction; the failure is recorded afterwards,
        // separately (that save fails here too, and is logged rather than masking the original error)
        verify(experimentImportRepository).findById(1L);
        verify(experimentImportRepository, times(2)).save(experimentImport);
    }

    @Test
    void testProcessSuccessFullExport() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        writeExportJson(fullExport());
        when(assignmentService.createAssignmentInLms(eq(ltiUserEntity), any(Assignment.class), anyLong(), anyString())).thenReturn(assignment);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false);

        verify(conditionRepository).save(any(Condition.class));
        verify(exposureRepository).save(any(Exposure.class));
        verify(groupRepository).save(any(Group.class));
        verify(exposureGroupConditionRepository).save(any(ExposureGroupCondition.class));
        verify(assignmentRepository).save(any(Assignment.class));
        verify(treatmentRepository, times(2)).save(any(edu.iu.terracotta.dao.entity.Treatment.class));
        verify(assessmentRepository).save(any(edu.iu.terracotta.dao.entity.Assessment.class));
        verify(questionRepository).save(any(edu.iu.terracotta.dao.entity.QuestionMc.class));
        // the MC question also matches the broader Question argument matcher, so this is 2 total invocations
        verify(questionRepository, times(2)).save(any(edu.iu.terracotta.dao.entity.Question.class));
        verify(integrationClientRepository).save(any(IntegrationClient.class));
        verify(integrationConfigurationRepository).save(any(edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration.class));
        verify(integrationRepository).save(any(edu.iu.terracotta.dao.entity.integrations.Integration.class));
        verify(answerMcRepository).save(any(edu.iu.terracotta.dao.entity.AnswerMc.class));
        verify(outcomeRepository).save(any(edu.iu.terracotta.dao.entity.Outcome.class));
        verify(assignmentService).createAssignmentInLms(eq(ltiUserEntity), any(Assignment.class), anyLong(), anyString());
        verify(fileStorageService, never()).sendConsentFileToLms(any(), any(), any());
        verify(experimentImport).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.COMPLETE);
        verify(experimentImportRepository).save(experimentImport);
    }

    // export id/FK fields are plain strings - fullExport() above uses old-style numeric strings
    // (as a pre-uuid export file would still contain), this proves a new-style export using real
    // uuid strings imports and links entities identically, since the cross-referencing is
    // format-agnostic (a fresh HashMap keyed by whatever string the file happens to use)
    @Test
    void testProcessSuccessFullExportWithUuidIds() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        String experimentId = UUID.randomUUID().toString();
        String conditionId = UUID.randomUUID().toString();
        String exposureId = UUID.randomUUID().toString();
        String groupId = UUID.randomUUID().toString();
        String exposureGroupConditionId = UUID.randomUUID().toString();
        String assignmentId = UUID.randomUUID().toString();
        String treatmentId = UUID.randomUUID().toString();
        String assessmentId = UUID.randomUUID().toString();
        String mcQuestionId = UUID.randomUUID().toString();
        String essayQuestionId = UUID.randomUUID().toString();
        String integrationClientId = UUID.randomUUID().toString();
        String integrationConfigurationId = UUID.randomUUID().toString();
        String integrationId = UUID.randomUUID().toString();
        String answerMcId = UUID.randomUUID().toString();
        String outcomeId = UUID.randomUUID().toString();

        Export export = Export.builder()
            .experiment(ExperimentExport.builder().id(experimentId).title("source experiment title").description("description").exposureType(ExposureTypes.BETWEEN).participationType(ParticipationTypes.AUTO).distributionType(DistributionTypes.EVEN).build())
            .conditions(List.of(ConditionExport.builder().id(conditionId).name("condition").defaultCondition(true).distributionPct(50F).experimentId(experimentId).build()))
            .exposures(List.of(ExposureExport.builder().id(exposureId).title("exposure").experimentId(experimentId).build()))
            .groups(List.of(GroupExport.builder().id(groupId).name("group").experimentId(experimentId).build()))
            .exposureGroupConditions(List.of(ExposureGroupConditionExport.builder().id(exposureGroupConditionId).conditionId(conditionId).exposureId(exposureId).groupId(groupId).build()))
            .assignments(List.of(AssignmentExport.builder().id(assignmentId).title("assignment").exposureId(exposureId).numOfSubmissions(1).build()))
            .treatments(List.of(TreatmentExport.builder().id(treatmentId).assignmentId(assignmentId).conditionId(conditionId).build()))
            .assessments(List.of(AssessmentExport.builder().id(assessmentId).title("assessment").treatmentId(treatmentId).build()))
            .questions(
                List.of(
                    QuestionExport.builder().id(mcQuestionId).html("mc question").questionType(QuestionTypes.MC).assessmentId(assessmentId).randomizeAnswers(true).build(),
                    QuestionExport.builder().id(essayQuestionId).html("essay question").questionType(QuestionTypes.ESSAY).assessmentId(assessmentId).build()
                )
            )
            .integrationClients(List.of(IntegrationClientExport.builder().id(integrationClientId).name("integration client").enabled(true).build()))
            .integrationConfigurations(List.of(IntegrationConfigurationExport.builder().id(integrationConfigurationId).clientId(integrationClientId).launchUrl("http://launch.url").build()))
            .integrations(List.of(IntegrationExport.builder().id(integrationId).configurationId(integrationConfigurationId).questionId(mcQuestionId).build()))
            .answersMc(List.of(AnswerMcExport.builder().id(answerMcId).answerOrder(1).correct(true).html("answer").questionId(mcQuestionId).build()))
            .outcomes(List.of(OutcomeExport.builder().id(outcomeId).title("outcome").maxPoints(10F).exposureId(exposureId).build()))
            .build();

        writeExportJson(export);
        when(assignmentService.createAssignmentInLms(eq(ltiUserEntity), any(Assignment.class), anyLong(), anyString())).thenReturn(assignment);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false);

        verify(conditionRepository).save(any(Condition.class));
        verify(exposureRepository).save(any(Exposure.class));
        verify(groupRepository).save(any(Group.class));
        verify(exposureGroupConditionRepository).save(any(ExposureGroupCondition.class));
        verify(assignmentRepository).save(any(Assignment.class));
        verify(treatmentRepository, times(2)).save(any(edu.iu.terracotta.dao.entity.Treatment.class));
        verify(assessmentRepository).save(any(edu.iu.terracotta.dao.entity.Assessment.class));
        verify(integrationRepository).save(any(edu.iu.terracotta.dao.entity.integrations.Integration.class));
        verify(answerMcRepository).save(any(edu.iu.terracotta.dao.entity.AnswerMc.class));
        verify(outcomeRepository).save(any(edu.iu.terracotta.dao.entity.Outcome.class));
        verify(experimentImport).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.COMPLETE);
        verify(experimentImport, never()).addErrorMessage(anyString());
    }

    // when a source assignment's old ID is in the repoint map, the already-existing (copied) LMS
    // assignment is re-pointed rather than a brand-new one created - see
    // ExperimentCopyCandidateServiceImpl for how that map gets built
    @Test
    void testProcessRepointsMappedAssignmentInsteadOfCreating() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        writeExportJson(fullExport());
        when(assignmentService.repointAssignmentInLms(eq(ltiUserEntity), any(Assignment.class), anyLong(), anyString(), eq(lmsAssignment))).thenReturn(assignment);

        // 700L is the old (source) assignment ID from fullExport()'s AssignmentExport
        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(700L, lmsAssignment), false, false);

        verify(assignmentService).repointAssignmentInLms(eq(ltiUserEntity), any(Assignment.class), anyLong(), anyString(), eq(lmsAssignment));
        verify(assignmentService, never()).createAssignmentInLms(any(), any(), anyLong(), anyString());
        verify(experimentImport).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.COMPLETE);
    }

    // a repointed assignment is the instructor's own pre-existing LMS content - on rollback it
    // must be restored to its original URL, never deleted, unlike a newly-created assignment
    @Test
    void testProcessRepointedAssignmentRestoredNotDeletedOnRollback() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException, AssignmentNotEditedException, ApiException {
        Export export = fullExport();
        // a second assignment (not in the repoint map) that will fail to create, forcing rollback
        export.setAssignments(
            List.of(
                AssignmentExport.builder().id("700").title("assignment").exposureId("400").numOfSubmissions(1).build(),
                AssignmentExport.builder().id("701").title("assignment 2").exposureId("400").numOfSubmissions(1).build()
            )
        );
        writeExportJson(export);

        when(lmsAssignment.getLmsExternalToolFields()).thenReturn(
            edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsExternalToolFields.builder().url("https://original.example.com/lti3?experiment=1&assignment=700").build()
        );
        when(assignmentService.repointAssignmentInLms(eq(ltiUserEntity), any(Assignment.class), anyLong(), anyString(), eq(lmsAssignment))).thenReturn(assignment);
        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString()))
            .thenThrow(new AssignmentNotCreatedException("boom"));

        assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(700L, lmsAssignment), false, false));

        verify(assignmentService, never()).deleteAssignmentInLms(any(), anyString(), any());
        verify(assignmentService).restoreRepointedAssignmentUrlInLms(eq(ltiUserEntity), eq(lmsAssignment), eq("https://original.example.com/lti3?experiment=1&assignment=700"), anyString());
    }

    // the import's own transaction rolls back on failure - the ERROR status must be saved
    // separately afterwards, or the import would look like it's still processing forever
    @Test
    void testProcessLmsFailureRollsBackThenRecordsErrorStatus() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        writeExportJson(fullExport());
        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString()))
            .thenThrow(new AssignmentNotCreatedException("boom"));

        assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false));

        verify(transactionManager).rollback(any());
        verify(experimentImport, atLeastOnce()).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.ERROR);
        verify(experimentImportRepository).findById(experimentImport.getId());
        verify(experimentImportRepository).save(experimentImport);
    }

    @Test
    void testProcessSuccessDoesNotRecordAnError() throws IOException {
        writeExportJson(fullExport());

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false);

        verify(transactionManager, never()).rollback(any());
        verify(experimentImportRepository, never()).findById(anyLong());
    }

    // a background course-copy recreation has nobody watching - its owner gets an email instead
    @Test
    void testProcessEmailsOwnerWhenLmsAssignmentFailsAndNotificationRequested() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        writeExportJson(fullExport());
        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString()))
            .thenThrow(new AssignmentNotCreatedException("boom"));

        assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), true, false));

        verify(experimentCopyNotificationService).notifyLmsFailure(ltiUserEntity);
    }

    @Test
    void testProcessDoesNotEmailOwnerWhenLmsAssignmentFailsWithoutNotificationRequested() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        writeExportJson(fullExport());
        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString()))
            .thenThrow(new AssignmentNotCreatedException("boom"));

        assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false));

        verify(experimentCopyNotificationService, never()).notifyLmsFailure(any());
    }

    @Test
    void testProcessDoesNotEmailOwnerWhenLmsAssignmentsSucceed() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        writeExportJson(fullExport());

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), true, false);

        verify(experimentCopyNotificationService, never()).notifyLmsFailure(any());
    }

    @Test
    void testProcessIntegrationClientReusesExistingEnabledClient() throws IOException {
        when(integrationClientRepository.findAll()).thenReturn(List.of(integrationClient));
        when(integrationClient.isEnabled()).thenReturn(true);
        when(integrationClient.getName()).thenReturn("integration client");

        Export export = fullExport();
        export.setAssignments(Collections.emptyList());
        export.setTreatments(Collections.emptyList());
        export.setAssessments(Collections.emptyList());
        export.setQuestions(Collections.emptyList());
        export.setIntegrations(Collections.emptyList());
        export.setAnswersMc(Collections.emptyList());
        writeExportJson(export);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false);

        verify(integrationClientRepository, never()).save(any(IntegrationClient.class));
    }

    @Test
    void testProcessConsentParticipationTypeSuccess() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        Export export = fullExport();
        export.getExperiment().setParticipationType(ParticipationTypes.CONSENT);
        export.setConsentDocument(ConsentDocumentExport.builder().id("200").title("consent title").html("<p>consent</p>").experimentId("100").build());
        writeExportJson(export);

        File consentDir = importDirectory.resolve("consent").toFile();
        consentDir.mkdirs();
        Files.writeString(consentDir.toPath().resolve(ExperimentImport.CONSENT_FILE_NAME), "pdf-bytes");

        when(fileStorageService.saveConsentFile(any(), anyString())).thenReturn(
            edu.iu.terracotta.dao.entity.FileSubmissionLocal.builder()
                .encryptionMethod("AES")
                .encryptionPhrase("phrase")
                .filePath("/tmp/consent.pdf")
                .build()
        );
        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString())).thenReturn(assignment);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false);

        verify(consentDocumentRepository).save(any(ConsentDocument.class));
        verify(fileStorageService).sendConsentFileToLms(any(ConsentDocument.class), any(Experiment.class), eq(ltiUserEntity));
        verify(experimentImport).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.COMPLETE);
    }

    @Test
    void testProcessConsentParticipationTypeMissingFile() throws IOException {
        Export export = fullExport();
        export.getExperiment().setParticipationType(ParticipationTypes.CONSENT);
        export.setConsentDocument(ConsentDocumentExport.builder().id("200").title("consent title").html("<p>consent</p>").experimentId("100").build());
        writeExportJson(export);

        // the missing consent file only aborts the consentDocument() step; process() continues
        // importing every other component and only rolls back once it reaches the final error check
        assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false));

        // once when the error happens, once more when it's saved after the rollback
        verify(experimentImport, times(2)).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(String.format("No consent PDF file [%s] found for experiment with consent participation type.", ExperimentImport.CONSENT_FILE_NAME));
        verify(consentDocumentRepository, never()).save(any(ConsentDocument.class));
        verify(conditionRepository).save(any(Condition.class));
    }

    @Test
    void testProcessConsentFileReadError() throws IOException {
        Export export = fullExport();
        export.getExperiment().setParticipationType(ParticipationTypes.CONSENT);
        export.setConsentDocument(ConsentDocumentExport.builder().id("200").title("consent title").html("<p>consent</p>").experimentId("100").build());
        writeExportJson(export);

        File consentDir = importDirectory.resolve("consent").toFile();
        consentDir.mkdirs();
        Files.writeString(consentDir.toPath().resolve(ExperimentImport.CONSENT_FILE_NAME), "pdf-bytes");

        when(fileStorageService.saveConsentFile(any(), anyString())).thenThrow(new RuntimeException("disk full"));

        assertThrows(RuntimeException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false));
    }

    @Test
    void testProcessExperimentTitleCollisionAppendsIndex() throws IOException {
        Export export = fullExport();
        export.setAssignments(Collections.emptyList());
        export.setTreatments(Collections.emptyList());
        export.setAssessments(Collections.emptyList());
        export.setQuestions(Collections.emptyList());
        export.setIntegrations(Collections.emptyList());
        export.setAnswersMc(Collections.emptyList());
        writeExportJson(export);

        String collidingTitle = String.format("%s %s", ExperimentImport.EXPERIMENT_TITLE_PREFIX, export.getExperiment().getTitle());
        when(experimentRepository.existsByTitle(collidingTitle)).thenReturn(true);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false);

        verify(experimentImport).setImportedTitle(String.format("%s %s (1)", ExperimentImport.EXPERIMENT_TITLE_PREFIX, export.getExperiment().getTitle()));
    }

    // a copied course's recreated experiment keeps its own title - the "(Imported)" label is only
    // for experiments an instructor imports themselves from an export file
    @Test
    void testProcessKeepSourceTitleUsesTheSourceTitleUnlabelled() throws IOException {
        Export export = fullExport();
        writeExportJson(export);
        when(experimentImport.getContext()).thenReturn(ltiContextEntity);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, true);

        verify(experimentImport).setImportedTitle(export.getExperiment().getTitle());
        // the source experiment in the original course always has this same title
        verify(experimentRepository, never()).existsByTitle(anyString());
    }

    @Test
    void testProcessKeepSourceTitleIsMadeUniqueWithinTheNewCourse() throws IOException {
        Export export = fullExport();
        writeExportJson(export);
        when(experimentImport.getContext()).thenReturn(ltiContextEntity);
        when(experimentRepository.existsByTitleAndLtiContextEntity_ContextId(export.getExperiment().getTitle(), 1L)).thenReturn(true);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, true);

        verify(experimentImport).setImportedTitle(String.format("%s (1)", export.getExperiment().getTitle()));
    }

    @Test
    void testProcessAssignmentCreationInLmsFailsDeletesCreatedAssignments() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException, AssignmentNotEditedException, ApiException {
        Export export = fullExport();
        export.setAssignments(
            List.of(
                AssignmentExport.builder().id("700").title("assignment one").exposureId("400").numOfSubmissions(1).build(),
                AssignmentExport.builder().id("701").title("assignment two").exposureId("400").numOfSubmissions(1).build()
            )
        );
        export.setTreatments(Collections.emptyList());
        export.setAssessments(Collections.emptyList());
        export.setQuestions(Collections.emptyList());
        export.setIntegrations(Collections.emptyList());
        export.setAnswersMc(Collections.emptyList());
        writeExportJson(export);

        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString()))
            .thenReturn(assignment)
            .thenThrow(new AssignmentNotCreatedException("failed to create assignment"));

        ExperimentImportException exception = assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false));

        assertEquals(String.format("Errors occurred processing experiment import with ID: [%s]. Rolling back transactions.", experimentImport.getId()), exception.getMessage());
        verify(experimentImport).addErrorMessage("Assignment creation in LMS failed");
        verify(assignmentService).deleteAssignmentInLms(eq(assignment), anyString(), eq(ltiUserEntity));
        // never saved as COMPLETE inside the rolled-back transaction - only as ERROR, afterwards
        verify(experimentImport, never()).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.COMPLETE);
        verify(experimentImportRepository).save(experimentImport);
    }

    @Test
    void testProcessAssignmentDeletionInLmsFailureIsSwallowed() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException, AssignmentNotEditedException, ApiException {
        Export export = fullExport();
        export.setAssignments(
            List.of(
                AssignmentExport.builder().id("700").title("assignment one").exposureId("400").numOfSubmissions(1).build(),
                AssignmentExport.builder().id("701").title("assignment two").exposureId("400").numOfSubmissions(1).build()
            )
        );
        export.setTreatments(Collections.emptyList());
        export.setAssessments(Collections.emptyList());
        export.setQuestions(Collections.emptyList());
        export.setIntegrations(Collections.emptyList());
        export.setAnswersMc(Collections.emptyList());
        writeExportJson(export);

        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString()))
            .thenReturn(assignment)
            .thenThrow(new TerracottaConnectorException("connector failed"));
        doThrow(new AssignmentNotEditedException("could not delete")).when(assignmentService).deleteAssignmentInLms(any(Assignment.class), anyString(), any());

        assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false));

        verify(assignmentService).deleteAssignmentInLms(eq(assignment), anyString(), eq(ltiUserEntity));
    }

    @Test
    void testProcessConsentAssignmentCreationInLmsFails() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        Export export = fullExport();
        export.getExperiment().setParticipationType(ParticipationTypes.CONSENT);
        export.setConsentDocument(ConsentDocumentExport.builder().id("200").title("consent title").html("<p>consent</p>").experimentId("100").build());
        writeExportJson(export);

        File consentDir = importDirectory.resolve("consent").toFile();
        consentDir.mkdirs();
        Files.writeString(consentDir.toPath().resolve(ExperimentImport.CONSENT_FILE_NAME), "pdf-bytes");

        when(fileStorageService.saveConsentFile(any(), anyString())).thenReturn(
            edu.iu.terracotta.dao.entity.FileSubmissionLocal.builder()
                .encryptionMethod("AES")
                .encryptionPhrase("phrase")
                .filePath("/tmp/consent.pdf")
                .build()
        );
        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString())).thenReturn(assignment);
        doThrow(new IOException("io error")).when(fileStorageService).sendConsentFileToLms(any(ConsentDocument.class), any(Experiment.class), any());

        ExperimentImportException exception = assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo, Map.of(), false, false));

        assertEquals(String.format("Errors occurred processing experiment import with ID: [%s]. Rolling back transactions.", experimentImport.getId()), exception.getMessage());
        verify(experimentImport).addErrorMessage("Consent assignment creation in LMS failed");
    }

}
